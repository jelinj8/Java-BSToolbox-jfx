package cz.bliksoft.javautils.fx.controls.graph.interaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.xml.GraphSerializer;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.command.CompoundGraphCommand;
import cz.bliksoft.javautils.fx.controls.graph.command.CreateEdgeCommand;
import cz.bliksoft.javautils.fx.controls.graph.command.CreateNodeCommand;
import cz.bliksoft.javautils.fx.controls.graph.command.DeleteElementsCommand;
import cz.bliksoft.javautils.fx.controls.graph.command.IGraphCommand;
import cz.bliksoft.javautils.uuid.RandomUUIDCreator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class ClipboardHandler {

	private static final DataFormat GRAPH_FORMAT = new DataFormat("application/x-bsgraph-fragment");
	private static final double PASTE_OFFSET = 20;

	private final GraphCanvas canvas;
	private boolean lastWasCut;

	public ClipboardHandler(GraphCanvas canvas) {
		this.canvas = canvas;
		canvas.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
	}

	private void onKeyPressed(KeyEvent e) {
		if (!e.isControlDown())
			return;

		if (e.getCode() == KeyCode.C) {
			copy();
			e.consume();
		} else if (e.getCode() == KeyCode.X) {
			cut();
			e.consume();
		} else if (e.getCode() == KeyCode.V) {
			paste();
			e.consume();
		}
	}

	public void copy() {
		String xml = serializeSelection();
		if (xml == null)
			return;
		lastWasCut = false;
		setClipboard(xml);
	}

	public void cut() {
		String xml = serializeSelection();
		if (xml == null)
			return;
		lastWasCut = true;
		setClipboard(xml);

		DeleteElementsCommand deleteCmd = new DeleteElementsCommand(canvas.getGraph(),
				canvas.getSelectionModel().getSelection());
		canvas.getCommandHistory().execute(deleteCmd);
		canvas.getSelectionModel().clear();
		canvas.refreshGraph();
	}

	public void paste() {
		String xml = getClipboard();
		if (xml == null)
			return;

		try {
			Graph fragment = GraphSerializer.unmarshal(xml);
			pasteFragment(fragment);
		} catch (Exception ignore) {
		}
	}

	private void pasteFragment(Graph fragment) {
		Group target = findPasteTarget();
		if (target == null)
			return;

		Map<UUID, UUID> idMapping = new HashMap<>();

		List<IGraphCommand> commands = new ArrayList<>();
		List<UUID> newElementIds = new ArrayList<>();

		for (Node node : fragment.getNodes()) {
			UUID newId = RandomUUIDCreator.getRandomUuid();
			idMapping.put(node.getId(), newId);
			node.setId(newId);
			node.setX(node.getX() + PASTE_OFFSET);
			node.setY(node.getY() + PASTE_OFFSET);

			for (JoinPoint jp : node.getJoinPoints()) {
				UUID newJpId = RandomUUIDCreator.getRandomUuid();
				idMapping.put(jp.getId(), newJpId);
				jp.setId(newJpId);
			}

			commands.add(new CreateNodeCommand(target, node));
			newElementIds.add(newId);
		}

		for (Edge edge : fragment.getEdges()) {
			UUID newEdgeId = RandomUUIDCreator.getRandomUuid();
			idMapping.put(edge.getId(), newEdgeId);
			edge.setId(newEdgeId);
			UUID newSource = idMapping.get(edge.getSourceJoinPointId());
			UUID newTarget = idMapping.get(edge.getTargetJoinPointId());
			if (newSource != null)
				edge.setSourceJoinPointId(newSource);
			if (newTarget != null)
				edge.setTargetJoinPointId(newTarget);

			if (newSource != null && newTarget != null)
				commands.add(new CreateEdgeCommand(target, edge));
		}

		for (Group group : fragment.getGroups()) {
			UUID newGroupId = RandomUUIDCreator.getRandomUuid();
			idMapping.put(group.getId(), newGroupId);
			group.setId(newGroupId);
			group.setX(group.getX() + PASTE_OFFSET);
			group.setY(group.getY() + PASTE_OFFSET);

			for (JoinPoint jp : group.getExposedJoinPoints()) {
				UUID newJpId = RandomUUIDCreator.getRandomUuid();
				idMapping.put(jp.getId(), newJpId);
				jp.setId(newJpId);
			}

			commands.add(new IGraphCommand() {
				@Override
				public void execute() {
					target.getGroups().add(group);
				}

				@Override
				public void undo() {
					target.getGroups().remove(group);
				}

				@Override
				public void redo() {
					execute();
				}

				@Override
				public String getDescription() {
					return "Paste group";
				}
			});
			newElementIds.add(newGroupId);
		}

		if (commands.isEmpty())
			return;

		CompoundGraphCommand compound = new CompoundGraphCommand("Paste", commands);
		canvas.getCommandHistory().execute(compound);
		canvas.refreshGraph();

		canvas.getSelectionModel().selectAll(new java.util.LinkedHashSet<>(newElementIds));
		canvas.updateSelectionVisuals();
	}

	private String serializeSelection() {
		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		if (selected.isEmpty() || canvas.getGraph() == null)
			return null;

		Graph fragment = new Graph("clipboard");

		Set<UUID> copiedNodeIds = new java.util.HashSet<>();
		Set<UUID> allJpIds = new java.util.HashSet<>();

		for (Node node : canvas.getGraph().getNodes()) {
			if (selected.contains(node.getId())) {
				fragment.getNodes().add(node);
				copiedNodeIds.add(node.getId());
				node.getJoinPoints().forEach(jp -> allJpIds.add(jp.getId()));
			}
		}

		for (Group group : canvas.getGraph().getGroups()) {
			Set<UUID> groupNodeIds = new java.util.HashSet<>();
			group.getNodes().forEach(n -> groupNodeIds.add(n.getId()));
			if (selected.contains(group.getId()) || copiedNodeIds.containsAll(groupNodeIds)) {
				fragment.getGroups().add(group);
				for (JoinPoint jp : group.getExposedJoinPoints())
					allJpIds.add(jp.getId());
			}
		}

		for (Edge edge : canvas.getGraph().getEdges()) {
			if (selected.contains(edge.getId()))
				fragment.getEdges().add(edge);
			else if (allJpIds.contains(edge.getSourceJoinPointId()) && allJpIds.contains(edge.getTargetJoinPointId()))
				fragment.getEdges().add(edge);
		}

		try {
			return GraphSerializer.marshal(fragment);
		} catch (Exception e) {
			return null;
		}
	}

	private Group findPasteTarget() {
		Group root = canvas.getGraph();
		if (root == null)
			return null;
		var sel = canvas.getSelectionModel().getSelection();
		if (sel.size() == 1) {
			Group selected = root.findGroup(sel.iterator().next());
			if (selected != null && !selected.isCollapsed())
				return selected;
		}
		return root;
	}

	private void setClipboard(String xml) {
		ClipboardContent content = new ClipboardContent();
		content.put(GRAPH_FORMAT, xml);
		content.putString(xml);
		Clipboard.getSystemClipboard().setContent(content);
	}

	private String getClipboard() {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		Object data = clipboard.getContent(GRAPH_FORMAT);
		if (data instanceof String s)
			return s;
		String text = clipboard.getString();
		if (text != null && text.startsWith("<?xml") && text.contains("<graph"))
			return text;
		return null;
	}
}
