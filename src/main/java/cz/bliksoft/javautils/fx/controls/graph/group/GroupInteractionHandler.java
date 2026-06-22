package cz.bliksoft.javautils.fx.controls.graph.group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.Node;
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
		Group root = canvas.getGraph();
		if (root == null)
			return;

		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		if (selected.isEmpty())
			return;

		Group commonParent = findCommonParent(root, selected);
		if (commonParent == null)
			return;

		Set<UUID> selectedIds = new java.util.LinkedHashSet<>();
		for (UUID id : selected) {
			if (commonParent.getNodes().stream().anyMatch(n -> n.getId().equals(id)))
				selectedIds.add(id);
			else if (commonParent.getGroups().stream().anyMatch(g -> g.getId().equals(id)))
				selectedIds.add(id);
		}

		if (selectedIds.isEmpty())
			return;

		repairMisplacedEdges(commonParent);

		GroupBuilder.GroupResult result = GroupBuilder.createFromSelection(commonParent, selectedIds, "Group");
		GroupCommand cmd = new GroupCommand(commonParent, result.getGroup(), result.getBridgeEdges(),
				result.getRelinks());
		canvas.getCommandHistory().execute(cmd);
		canvas.refreshGraph();
	}

	private Group findCommonParent(Group root, Set<UUID> ids) {
		Group common = null;
		for (UUID id : ids) {
			Group parent = root.findParentOf(id);
			if (parent == null)
				return null;
			if (common == null)
				common = parent;
			else if (!common.getId().equals(parent.getId()))
				return null;
		}
		return common;
	}

	public void ungroupSelected() {
		Group root = canvas.getGraph();
		if (root == null)
			return;

		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		for (UUID id : selected) {
			Group group = root.findGroup(id);
			if (group == null)
				continue;
			Group actualParent = root.findParentOf(id);
			if (actualParent == null)
				continue;
			UngroupCommand cmd = new UngroupCommand(actualParent, group);
			canvas.getCommandHistory().execute(cmd);
			canvas.getSelectionModel().clear();
			canvas.refreshGraph();
			return;
		}
	}

	public void enterGroup(java.util.UUID groupId) {
		if (editingPane != null)
			editingPane.enterGroup(groupId);
	}

	public void toggleCollapse(UUID groupId) {
		Group graph = canvas.getGraph();
		if (graph == null)
			return;

		Group group = GroupBuilder.findGroupById(graph, groupId);
		if (group == null)
			return;

		group.setCollapsed(!group.isCollapsed());
		canvas.refreshGraph();
	}

	private void repairMisplacedEdges(Group targetGroup) {
		Group rootGraph = canvas.getRootGraph();
		if (rootGraph == null || rootGraph.getId().equals(targetGroup.getId()))
			return;

		Set<UUID> targetJpIds = new HashSet<>();
		for (Node node : targetGroup.getNodes())
			for (JoinPoint jp : node.getJoinPoints())
				targetJpIds.add(jp.getId());
		for (Group child : targetGroup.getGroups())
			for (JoinPoint jp : child.getExposedJoinPoints())
				targetJpIds.add(jp.getId());

		if (targetJpIds.isEmpty())
			return;

		collectMisplacedEdgesFrom(rootGraph, targetGroup, targetJpIds);
	}

	private void collectMisplacedEdgesFrom(Group searchGroup, Group targetGroup, Set<UUID> targetJpIds) {
		if (searchGroup.getId().equals(targetGroup.getId()))
			return;

		List<Edge> toMove = new ArrayList<>();
		for (Edge edge : searchGroup.getEdges()) {
			if (targetJpIds.contains(edge.getSourceJoinPointId()) && targetJpIds.contains(edge.getTargetJoinPointId()))
				toMove.add(edge);
		}
		for (Edge edge : toMove) {
			searchGroup.getEdges().remove(edge);
			targetGroup.getEdges().add(edge);
		}

		for (Group child : searchGroup.getGroups())
			collectMisplacedEdgesFrom(child, targetGroup, targetJpIds);
	}

	private UUID findGroupAt(MouseEvent e) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != canvas) {
			Object headerId = target.getProperties().get("groupHeader");
			if (headerId instanceof UUID uid)
				return uid;
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
