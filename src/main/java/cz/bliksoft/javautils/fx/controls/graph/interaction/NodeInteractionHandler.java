package cz.bliksoft.javautils.fx.controls.graph.interaction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.command.DeleteElementsCommand;
import cz.bliksoft.javautils.fx.controls.graph.command.GraphCommandHistory;
import cz.bliksoft.javautils.fx.controls.graph.command.MoveNodesCommand;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class NodeInteractionHandler {

	private static final double ALIGNMENT_THRESHOLD = 5.0;
	private static final double NUDGE_AMOUNT = 1.0;

	private final GraphCanvas canvas;
	private final GraphCommandHistory commandHistory;

	private boolean dragging;
	private double dragStartScreenX, dragStartScreenY;
	private Map<UUID, double[]> dragStartPositions;

	private boolean resizing;
	private UUID resizingGroupId;
	private UUID resizingNodeId;
	private double resizeStartX, resizeStartY, resizeStartW, resizeStartH;

	private Line alignGuideH;
	private Line alignGuideV;

	public NodeInteractionHandler(GraphCanvas canvas, GraphCommandHistory commandHistory) {
		this.canvas = canvas;
		this.commandHistory = commandHistory;
		setupHandlers();
	}

	private void setupHandlers() {
		canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
		canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
		canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
		canvas.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
		canvas.setFocusTraversable(true);
	}

	private void onMousePressed(MouseEvent e) {
		if (e.getButton() != MouseButton.PRIMARY)
			return;

		UUID resizeGroupId = findResizeHandle(e, "resizeGroupId");
		UUID resizeNodeIdFound = findResizeHandle(e, "resizeNodeId");
		if (resizeGroupId != null || resizeNodeIdFound != null) {
			if (e.getClickCount() == 2) {
				resetSize(resizeGroupId, resizeNodeIdFound);
				e.consume();
				return;
			}
			resizing = true;
			resizingGroupId = resizeGroupId;
			resizingNodeId = resizeNodeIdFound;
			dragStartScreenX = e.getScreenX();
			dragStartScreenY = e.getScreenY();
			if (resizeGroupId != null) {
				Group group = GroupBuilder.findGroupById(canvas.getGraph(), resizeGroupId);
				if (group != null) {
					resizeStartW = group.getWidth();
					resizeStartH = group.getHeight();
				}
			} else if (resizeNodeIdFound != null) {
				for (Node node : canvas.getGraph().getNodes()) {
					if (node.getId().equals(resizeNodeIdFound)) {
						resizeStartX = node.getX();
						resizeStartY = node.getY();
						resizeStartW = node.getWidth();
						resizeStartH = node.getHeight();
						break;
					}
				}
			}
			e.consume();
			return;
		}

		UUID clickedNodeId = findNodeAt(e);
		if (clickedNodeId == null)
			return;

		if (e.isControlDown())
			return;

		GraphSelectionModel sel = canvas.getSelectionModel();
		if (!sel.isSelected(clickedNodeId))
			sel.select(clickedNodeId);

		canvas.updateSelectionVisuals();

		dragging = true;
		dragStartScreenX = e.getScreenX();
		dragStartScreenY = e.getScreenY();
		dragStartPositions = capturePositionsWithGroups(sel.getSelection());
		e.consume();
	}

	private void onMouseDragged(MouseEvent e) {
		if (resizing) {
			handleResizeDrag(e);
			e.consume();
			return;
		}
		if (!dragging)
			return;

		double dx = (e.getScreenX() - dragStartScreenX) / canvas.getZoom();
		double dy = (e.getScreenY() - dragStartScreenY) / canvas.getZoom();

		double[] constrained = constrainDelta(dx, dy);
		dx = constrained[0];
		dy = constrained[1];

		for (var entry : dragStartPositions.entrySet()) {
			UUID nodeId = entry.getKey();
			double[] startPos = entry.getValue();
			double newX = startPos[0] + dx;
			double newY = startPos[1] + dy;

			if (canvas.isSnapToGrid()) {
				newX = canvas.snapX(newX);
				newY = canvas.snapY(newY);
			}

			Region visual = canvas.getNodeVisual(nodeId);
			if (visual != null) {
				visual.setLayoutX(newX);
				visual.setLayoutY(newY);
			}
		}

		updateAlignmentGuides();
		e.consume();
	}

	private void onMouseReleased(MouseEvent e) {
		if (resizing) {
			if (resizingNodeId != null) {
				for (Node node : canvas.getGraph().getNodes()) {
					if (node.getId().equals(resizingNodeId)) {
						cz.bliksoft.javautils.fx.controls.graph.command.ResizeNodeCommand cmd = new cz.bliksoft.javautils.fx.controls.graph.command.ResizeNodeCommand(
								canvas.getGraph(), resizingNodeId, resizeStartX, resizeStartY, resizeStartW,
								resizeStartH, node.getX(), node.getY(), node.getWidth(), node.getHeight());
						commandHistory.execute(cmd);
						break;
					}
				}
			}
			resizing = false;
			resizingGroupId = null;
			resizingNodeId = null;
			GroupBuilder.expandAllAncestors(canvas.getGraph());
			canvas.refreshGraph();
			e.consume();
			return;
		}
		if (!dragging)
			return;

		dragging = false;
		removeAlignmentGuides();

		Map<UUID, double[]> newPositions = new LinkedHashMap<>();
		for (UUID id : dragStartPositions.keySet()) {
			Region visual = canvas.getNodeVisual(id);
			if (visual != null)
				newPositions.put(id, new double[] { visual.getLayoutX(), visual.getLayoutY() });
		}

		boolean moved = false;
		for (var entry : newPositions.entrySet()) {
			double[] oldPos = dragStartPositions.get(entry.getKey());
			double[] newPos = entry.getValue();
			if (oldPos[0] != newPos[0] || oldPos[1] != newPos[1]) {
				moved = true;
				break;
			}
		}

		if (moved) {
			MoveNodesCommand cmd = new MoveNodesCommand(canvas.getGraph(), dragStartPositions, newPositions);
			commandHistory.execute(cmd);
			canvas.refreshGraph();
		}

		dragStartPositions = null;
		e.consume();
	}

	private void onKeyPressed(KeyEvent e) {
		if (e.isControlDown() && e.getCode() == KeyCode.Z) {
			if (commandHistory.canUndo()) {
				commandHistory.undo();
				canvas.refreshGraph();
			}
			e.consume();
		} else if (e.isControlDown() && e.getCode() == KeyCode.Y) {
			if (commandHistory.canRedo()) {
				commandHistory.redo();
				canvas.refreshGraph();
			}
			e.consume();
		} else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
			deleteSelected();
			e.consume();
		} else if (e.isControlDown() && e.getCode() == KeyCode.A) {
			selectAll();
			e.consume();
		} else if (isArrowKey(e.getCode())) {
			nudge(e.getCode());
			e.consume();
		}
	}

	private void deleteSelected() {
		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		if (selected.isEmpty())
			return;

		DeleteElementsCommand cmd = new DeleteElementsCommand(canvas.getGraph(), selected);
		commandHistory.execute(cmd);
		canvas.getSelectionModel().clear();
		canvas.refreshGraph();
	}

	private void selectAll() {
		java.util.Set<UUID> all = new java.util.LinkedHashSet<>();
		if (canvas.getGraph() != null) {
			canvas.getGraph().getNodes().forEach(n -> all.add(n.getId()));
			canvas.getGraph().getEdges().forEach(e -> all.add(e.getId()));
		}
		canvas.getSelectionModel().selectAll(all);
		canvas.updateSelectionVisuals();
	}

	private void nudge(KeyCode code) {
		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		if (selected.isEmpty())
			return;

		double amount = canvas.isSnapToGrid() ? canvas.getGridSpacing() : NUDGE_AMOUNT;
		double dx = 0, dy = 0;
		switch (code) {
		case UP -> dy = -amount;
		case DOWN -> dy = amount;
		case LEFT -> dx = -amount;
		case RIGHT -> dx = amount;
		default -> {
			return;
		}
		}

		Map<UUID, double[]> oldPositions = captureModelPositions(selected);
		Map<UUID, double[]> newPositions = new LinkedHashMap<>();
		for (var entry : oldPositions.entrySet()) {
			newPositions.put(entry.getKey(), new double[] { entry.getValue()[0] + dx, entry.getValue()[1] + dy });
		}

		MoveNodesCommand cmd = new MoveNodesCommand(canvas.getGraph(), oldPositions, newPositions);
		commandHistory.execute(cmd);
		canvas.refreshGraph();
	}

	private boolean isArrowKey(KeyCode code) {
		return code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.LEFT || code == KeyCode.RIGHT;
	}

	private Map<UUID, double[]> capturePositionsWithGroups(Set<UUID> selectedIds) {
		Map<UUID, double[]> positions = new LinkedHashMap<>();
		Set<UUID> expanded = expandGroupMembers(selectedIds);
		for (UUID id : expanded) {
			Region visual = canvas.getNodeVisual(id);
			if (visual != null)
				positions.put(id, new double[] { visual.getLayoutX(), visual.getLayoutY() });
		}
		return positions;
	}

	private Map<UUID, double[]> captureModelPositions(Set<UUID> selectedIds) {
		Map<UUID, double[]> positions = new LinkedHashMap<>();
		Graph graph = canvas.getGraph();
		if (graph == null)
			return positions;
		Set<UUID> expanded = expandGroupMembers(selectedIds);
		for (Node node : graph.getNodes()) {
			if (expanded.contains(node.getId()))
				positions.put(node.getId(), new double[] { node.getX(), node.getY() });
		}
		for (Group group : graph.getGroups()) {
			if (expanded.contains(group.getId()))
				positions.put(group.getId(), new double[] { group.getX(), group.getY() });
		}
		return positions;
	}

	private Set<UUID> expandGroupMembers(Set<UUID> selectedIds) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return selectedIds;
		Set<UUID> expanded = new java.util.LinkedHashSet<>(selectedIds);
		for (UUID id : selectedIds) {
			Group group = GroupBuilder.findGroupById(graph, id);
			if (group != null)
				expandGroupRecursive(graph, group, expanded);
		}
		return expanded;
	}

	private void expandGroupRecursive(Graph graph, Group group, Set<UUID> result) {
		result.addAll(group.getMemberNodeIds());
		for (UUID childGroupId : group.getMemberGroupIds()) {
			result.add(childGroupId);
			Group child = GroupBuilder.findGroupById(graph, childGroupId);
			if (child != null)
				expandGroupRecursive(graph, child, result);
		}
	}

	private boolean dragIncludesGroup() {
		Graph graph = canvas.getGraph();
		if (graph == null || dragStartPositions == null)
			return false;
		for (UUID id : dragStartPositions.keySet()) {
			if (GroupBuilder.findGroupById(graph, id) != null)
				return true;
		}
		return false;
	}

	private UUID findNodeAt(MouseEvent e) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != canvas) {
			Object nodeId = target.getProperties().get("nodeId");
			if (nodeId instanceof UUID)
				return (UUID) nodeId;
			target = target.getParent();
		}
		return null;
	}

	private void updateAlignmentGuides() {
		removeAlignmentGuides();

		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		if (selected.isEmpty() || canvas.getGraph() == null)
			return;

		Region firstSelected = canvas.getNodeVisual(selected.iterator().next());
		if (firstSelected == null)
			return;
		double sx = firstSelected.getLayoutX();
		double sy = firstSelected.getLayoutY();
		double sw = firstSelected.getPrefWidth();
		double sh = firstSelected.getPrefHeight();
		double scx = sx + sw / 2;
		double scy = sy + sh / 2;

		for (Node node : canvas.getGraph().getNodes()) {
			if (selected.contains(node.getId()))
				continue;
			double nx = node.getX(), ny = node.getY();
			double nw = node.getWidth(), nh = node.getHeight();
			double ncx = nx + nw / 2, ncy = ny + nh / 2;

			if (alignGuideV == null) {
				if (Math.abs(scx - ncx) < ALIGNMENT_THRESHOLD)
					alignGuideV = createGuide(ncx, Math.min(sy, ny) - 20, ncx, Math.max(sy + sh, ny + nh) + 20);
				else if (Math.abs(sx - nx) < ALIGNMENT_THRESHOLD)
					alignGuideV = createGuide(nx, Math.min(sy, ny) - 20, nx, Math.max(sy + sh, ny + nh) + 20);
				else if (Math.abs(sx + sw - nx - nw) < ALIGNMENT_THRESHOLD)
					alignGuideV = createGuide(nx + nw, Math.min(sy, ny) - 20, nx + nw, Math.max(sy + sh, ny + nh) + 20);
			}

			if (alignGuideH == null) {
				if (Math.abs(scy - ncy) < ALIGNMENT_THRESHOLD)
					alignGuideH = createGuide(Math.min(sx, nx) - 20, ncy, Math.max(sx + sw, nx + nw) + 20, ncy);
				else if (Math.abs(sy - ny) < ALIGNMENT_THRESHOLD)
					alignGuideH = createGuide(Math.min(sx, nx) - 20, ny, Math.max(sx + sw, nx + nw) + 20, ny);
				else if (Math.abs(sy + sh - ny - nh) < ALIGNMENT_THRESHOLD)
					alignGuideH = createGuide(Math.min(sx, nx) - 20, ny + nh, Math.max(sx + sw, nx + nw) + 20, ny + nh);
			}

			if (alignGuideH != null && alignGuideV != null)
				break;
		}

		if (alignGuideV != null)
			canvas.getContentPane().getChildren().add(alignGuideV);
		if (alignGuideH != null)
			canvas.getContentPane().getChildren().add(alignGuideH);
	}

	private void removeAlignmentGuides() {
		if (alignGuideV != null) {
			canvas.getContentPane().getChildren().remove(alignGuideV);
			alignGuideV = null;
		}
		if (alignGuideH != null) {
			canvas.getContentPane().getChildren().remove(alignGuideH);
			alignGuideH = null;
		}
	}

	private Line createGuide(double x1, double y1, double x2, double y2) {
		Line line = new Line(x1, y1, x2, y2);
		line.setStroke(Color.web("#FF6B6B"));
		line.setStrokeWidth(0.5);
		line.getStrokeDashArray().addAll(4.0, 4.0);
		line.setMouseTransparent(true);
		return line;
	}

	private void resetSize(UUID groupId, UUID nodeId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return;

		if (groupId != null) {
			Group group = GroupBuilder.findGroupById(graph, groupId);
			if (group != null) {
				double[] minBounds = GroupBuilder.computeMinBounds(graph, group);
				group.setX(minBounds[0]);
				group.setY(minBounds[1]);
				group.setWidth(minBounds[2]);
				group.setHeight(minBounds[3]);
				canvas.refreshGraph();
			}
		} else if (nodeId != null) {
			for (Node node : graph.getNodes()) {
				if (node.getId().equals(nodeId)) {
					cz.bliksoft.dataflow.types.NodeType type = cz.bliksoft.dataflow.types.NodeTypeRegistry.getInstance()
							.get(node.getTypeId());
					if (type != null) {
						cz.bliksoft.javautils.fx.controls.graph.command.ResizeNodeCommand cmd = new cz.bliksoft.javautils.fx.controls.graph.command.ResizeNodeCommand(
								graph, nodeId, node.getX(), node.getY(), node.getWidth(), node.getHeight(), node.getX(),
								node.getY(), type.getDefaultWidth(), type.getDefaultHeight());
						commandHistory.execute(cmd);
					}
					canvas.refreshGraph();
					return;
				}
			}
		}
	}

	private double[] constrainDelta(double dx, double dy) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return new double[] { dx, dy };

		double padding = 10;

		for (UUID id : dragStartPositions.keySet()) {
			double[] startPos = dragStartPositions.get(id);
			double elemW = 0, elemH = 0;
			Group container = null;

			Group movedGroup = GroupBuilder.findGroupById(graph, id);
			if (movedGroup != null) {
				elemW = movedGroup.getWidth();
				elemH = movedGroup.getHeight();
				container = findParentGroupOf(graph, movedGroup.getId());
			} else {
				for (Node node : graph.getNodes()) {
					if (node.getId().equals(id)) {
						elemW = node.getWidth();
						elemH = node.getHeight();
						container = GroupBuilder.findGroupContaining(graph, id);
						break;
					}
				}
			}

			if (container == null)
				continue;
			if (dragStartPositions.containsKey(container.getId()))
				continue;

			double minX = container.getX() + padding;
			double minY = container.getY() + padding;
			double maxX = container.getX() + container.getWidth() - elemW - padding;
			double maxY = container.getY() + container.getHeight() - elemH - padding;

			double newX = startPos[0] + dx;
			double newY = startPos[1] + dy;

			if (newX < minX)
				dx = minX - startPos[0];
			if (newY < minY)
				dy = minY - startPos[1];
			if (newX > maxX)
				dx = maxX - startPos[0];
			if (newY > maxY)
				dy = maxY - startPos[1];
		}

		return new double[] { dx, dy };
	}

	private Group findParentGroupOf(Graph graph, UUID groupId) {
		for (Group g : graph.getGroups()) {
			if (g.getMemberGroupIds().contains(groupId))
				return g;
		}
		return null;
	}

	private static final double MIN_NODE_SIZE = 20;

	private void handleResizeDrag(MouseEvent e) {
		double dx = (e.getScreenX() - dragStartScreenX) / canvas.getZoom();
		double dy = (e.getScreenY() - dragStartScreenY) / canvas.getZoom();
		Graph graph = canvas.getGraph();
		if (graph == null)
			return;

		if (resizingGroupId != null) {
			Group group = GroupBuilder.findGroupById(graph, resizingGroupId);
			if (group == null)
				return;
			double[] minBounds = GroupBuilder.computeMinBounds(graph, group);
			group.setWidth(Math.max(minBounds[2], resizeStartW + dx));
			group.setHeight(Math.max(minBounds[3], resizeStartH + dy));
			GroupBuilder.expandAllAncestors(graph);
			canvas.refreshGraph();
		} else if (resizingNodeId != null) {
			for (Node node : graph.getNodes()) {
				if (node.getId().equals(resizingNodeId)) {
					node.setWidth(Math.max(MIN_NODE_SIZE, resizeStartW + dx));
					node.setHeight(Math.max(MIN_NODE_SIZE, resizeStartH + dy));
					GroupBuilder.expandAllAncestors(graph);
					canvas.refreshNodeVisual(resizingNodeId);
					return;
				}
			}
		}
	}

	private UUID findResizeHandle(MouseEvent e, String propertyKey) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != canvas) {
			Object resizeId = target.getProperties().get(propertyKey);
			if (resizeId instanceof UUID)
				return (UUID) resizeId;
			target = target.getParent();
		}
		return null;
	}

	public GraphCommandHistory getCommandHistory() {
		return commandHistory;
	}
}
