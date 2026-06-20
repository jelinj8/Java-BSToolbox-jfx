package cz.bliksoft.javautils.fx.controls.graph.group;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.command.GroupCommand;
import cz.bliksoft.javautils.fx.controls.graph.command.UngroupCommand;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class GroupInteractionHandler {

	private final GraphCanvas canvas;
	private GroupEditingPane editingPane;

	public GroupInteractionHandler(GraphCanvas canvas) {
		this.canvas = canvas;
		canvas.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
		canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onDoubleClick);
	}

	public void setEditingPane(GroupEditingPane editingPane) {
		this.editingPane = editingPane;
	}

	private void onKeyPressed(KeyEvent e) {
		if (e.isControlDown() && e.getCode() == KeyCode.G) {
			if (e.isShiftDown())
				ungroupSelected();
			else
				groupSelected();
			e.consume();
		}
	}

	private void onDoubleClick(MouseEvent e) {
		if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2)
			return;

		UUID clickedGroupId = findGroupAt(e);
		if (clickedGroupId != null && editingPane != null) {
			editingPane.enterGroup(clickedGroupId);
			e.consume();
			return;
		}

		UUID clickedCollapsedGroupId = findCollapsedGroupAt(e);
		if (clickedCollapsedGroupId != null) {
			toggleCollapse(clickedCollapsedGroupId);
			e.consume();
		}
	}

	public void groupSelected() {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return;

		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		Set<UUID> selectedNodes = selected.stream()
				.filter(id -> graph.getNodes().stream().anyMatch(n -> n.getId().equals(id)))
				.collect(Collectors.toSet());

		if (selectedNodes.size() < 2)
			return;

		if (!validateGroupingAllowed(graph, selectedNodes))
			return;

		GroupBuilder.GroupResult result = GroupBuilder.createFromSelection(graph, selectedNodes, "Group");
		GroupCommand cmd = new GroupCommand(graph, result.getGroup(), result.getBridgeEdges(), result.getRelinks());
		canvas.getCommandHistory().execute(cmd);
		canvas.refreshGraph();
	}

	private boolean validateGroupingAllowed(Graph graph, Set<UUID> selectedNodes) {
		Group commonGroup = null;
		boolean hasGrouped = false;
		boolean hasUngrouped = false;

		for (UUID nodeId : selectedNodes) {
			Group containing = GroupBuilder.findGroupContaining(graph, nodeId);
			if (containing == null) {
				hasUngrouped = true;
			} else {
				hasGrouped = true;
				if (commonGroup == null)
					commonGroup = containing;
				else if (!commonGroup.getId().equals(containing.getId()))
					return false;
			}
		}

		if (hasGrouped && hasUngrouped)
			return false;

		if (commonGroup != null && !commonGroup.getMemberNodeIds().containsAll(selectedNodes))
			return false;

		return true;
	}

	public void ungroupSelected() {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return;

		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		for (UUID id : selected) {
			Group group = GroupBuilder.findGroupById(graph, id);
			if (group != null) {
				UngroupCommand cmd = new UngroupCommand(graph, group);
				canvas.getCommandHistory().execute(cmd);
				canvas.getSelectionModel().clear();
				canvas.refreshGraph();
				return;
			}
		}
	}

	public void toggleCollapse(UUID groupId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return;

		Group group = GroupBuilder.findGroupById(graph, groupId);
		if (group == null)
			return;

		group.setCollapsed(!group.isCollapsed());
		canvas.refreshGraph();
	}

	private UUID findGroupAt(MouseEvent e) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != canvas) {
			Object groupId = target.getProperties().get("groupId");
			if (groupId instanceof UUID uid) {
				if (target.getStyleClass().contains("graph-group-expanded"))
					return uid;
			}
			target = target.getParent();
		}
		return null;
	}

	private UUID findCollapsedGroupAt(MouseEvent e) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != canvas) {
			if (target.getStyleClass().contains("graph-group-collapsed")) {
				Object groupId = target.getProperties().get("groupId");
				if (groupId instanceof UUID uid)
					return uid;
			}
			target = target.getParent();
		}
		return null;
	}
}
