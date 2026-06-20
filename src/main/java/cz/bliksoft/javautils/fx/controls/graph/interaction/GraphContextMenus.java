package cz.bliksoft.javautils.fx.controls.graph.interaction;

import java.util.Set;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Direction;
import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.JoinPointPosition;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.command.DeleteElementsCommand;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class GraphContextMenus {

	private final GraphCanvas canvas;
	private ContextMenu activeMenu;

	public GraphContextMenus(GraphCanvas canvas) {
		this.canvas = canvas;
		setupHandler();
	}

	private void setupHandler() {
		canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
			if (activeMenu != null) {
				activeMenu.hide();
				activeMenu = null;
			}
		});

		canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
			if (e.getButton() != MouseButton.SECONDARY)
				return;

			UUID jpId = findJoinPointAt(e);
			if (jpId != null) {
				showJoinPointMenu(e, jpId);
				e.consume();
				return;
			}

			UUID elementId = findElementAt(e);
			if (elementId != null) {
				if (isGroupId(elementId))
					showGroupMenu(e, elementId);
				else if (isNodeId(elementId))
					showNodeMenu(e, elementId);
				else
					showEdgeMenu(e, elementId);
			} else {
				showCanvasMenu(e);
			}
			e.consume();
		});
	}

	private void showCanvasMenu(MouseEvent e) {
		ContextMenu menu = new ContextMenu();

		MenuItem selectAll = new MenuItem("Select All");
		selectAll.setOnAction(a -> {
			Set<UUID> all = new java.util.LinkedHashSet<>();
			if (canvas.getGraph() != null) {
				canvas.getGraph().getNodes().forEach(n -> all.add(n.getId()));
				canvas.getGraph().getEdges().forEach(ed -> all.add(ed.getId()));
			}
			canvas.getSelectionModel().selectAll(all);
			canvas.updateSelectionVisuals();
		});

		MenuItem zoomFit = new MenuItem("Zoom to Fit");
		zoomFit.setOnAction(a -> canvas.zoomToFit());

		MenuItem zoom100 = new MenuItem("Zoom 100%");
		zoom100.setOnAction(a -> canvas.resetZoom());

		menu.getItems().addAll(selectAll, new SeparatorMenuItem(), zoomFit, zoom100);
		show(menu, e);
	}

	private void showNodeMenu(MouseEvent e, UUID nodeId) {
		if (!canvas.getSelectionModel().isSelected(nodeId)) {
			canvas.getSelectionModel().select(nodeId);
			canvas.updateSelectionVisuals();
		}

		ContextMenu menu = new ContextMenu();

		MenuItem delete = new MenuItem("Delete");
		delete.setOnAction(a -> deleteSelected());

		MenuItem toFront = new MenuItem("Bring to Front");
		toFront.setOnAction(a -> changeZOrder(nodeId, 1));

		MenuItem toBack = new MenuItem("Send to Back");
		toBack.setOnAction(a -> changeZOrder(nodeId, -1));

		menu.getItems().addAll(delete, new SeparatorMenuItem(), toFront, toBack);
		show(menu, e);
	}

	private void showEdgeMenu(MouseEvent e, UUID edgeId) {
		if (!canvas.getSelectionModel().isSelected(edgeId)) {
			canvas.getSelectionModel().select(edgeId);
			canvas.updateSelectionVisuals();
		}

		ContextMenu menu = new ContextMenu();

		MenuItem delete = new MenuItem("Delete");
		delete.setOnAction(a -> deleteSelected());

		MenuItem reverse = new MenuItem("Reverse Direction");
		reverse.setOnAction(a -> reverseEdge(edgeId));

		menu.getItems().addAll(delete, new SeparatorMenuItem(), reverse);
		show(menu, e);
	}

	private void deleteSelected() {
		Set<UUID> selected = canvas.getSelectionModel().getSelection();
		if (selected.isEmpty())
			return;
		DeleteElementsCommand cmd = new DeleteElementsCommand(canvas.getGraph(), selected);
		canvas.getCommandHistory().execute(cmd);
		canvas.getSelectionModel().clear();
		canvas.refreshGraph();
	}

	private void reverseEdge(UUID edgeId) {
		if (canvas.getGraph() == null)
			return;
		for (Edge edge : canvas.getGraph().getEdges()) {
			if (edge.getId().equals(edgeId)) {
				UUID oldSource = edge.getSourceJoinPointId();
				edge.setSourceJoinPointId(edge.getTargetJoinPointId());
				edge.setTargetJoinPointId(oldSource);
				canvas.refreshGraph();
				return;
			}
		}
	}

	private void changeZOrder(UUID nodeId, int delta) {
		if (canvas.getGraph() == null)
			return;
		for (Node node : canvas.getGraph().getNodes()) {
			if (node.getId().equals(nodeId)) {
				node.setzOrder(node.getzOrder() + delta);
				canvas.refreshGraph();
				return;
			}
		}
	}

	private UUID findElementAt(MouseEvent e) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != canvas) {
			Object nodeId = target.getProperties().get("nodeId");
			if (nodeId instanceof UUID)
				return (UUID) nodeId;
			Object edgeId = target.getProperties().get("edgeId");
			if (edgeId instanceof UUID)
				return (UUID) edgeId;
			target = target.getParent();
		}
		return null;
	}

	private void showGroupMenu(MouseEvent e, UUID groupId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return;
		Group group = GroupBuilder.findGroupById(graph, groupId);
		if (group == null)
			return;

		canvas.getSelectionModel().select(groupId);
		canvas.updateSelectionVisuals();

		ContextMenu menu = new ContextMenu();

		MenuItem ungroup = new MenuItem("Ungroup");
		ungroup.setOnAction(a -> canvas.getGroupHandler().ungroupSelected());

		MenuItem collapse = new MenuItem(group.isCollapsed() ? "Expand" : "Collapse");
		collapse.setOnAction(a -> canvas.getGroupHandler().toggleCollapse(groupId));

		menu.getItems().addAll(ungroup, collapse);
		show(menu, e);
	}

	private void showJoinPointMenu(MouseEvent e, UUID jpId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return;

		ContextMenu menu = new ContextMenu();

		Group ownerGroup = findGroupOwningJoinPoint(graph, jpId);
		Node ownerNode = findNodeOwningJoinPoint(graph, jpId);

		if (ownerNode != null) {
			Group containingGroup = GroupBuilder.findGroupContaining(graph, ownerNode.getId());
			if (containingGroup != null) {
				boolean alreadyExposed = containingGroup.getJoinPointMappings().stream()
						.anyMatch(m -> m.getInternalId().equals(jpId));
				if (!alreadyExposed) {
					MenuItem expose = new MenuItem("Expose on Group");
					expose.setOnAction(a -> {
						JoinPoint jp = findJoinPointById(graph, jpId);
						if (jp == null)
							return;
						JoinPoint exposed = new JoinPoint(jp.getName(), jp.getPosition(),
								jp.getDirection().toBorderDirection(), -1);
						GroupBuilder.exposeJoinPoint(graph, containingGroup, jpId, exposed);
						canvas.refreshGraph();
					});
					menu.getItems().add(expose);
				}
			}
		}

		if (ownerGroup != null) {
			MenuItem unexpose = new MenuItem("Unexpose");
			unexpose.setOnAction(a -> {
				GroupBuilder.unexposeJoinPoint(graph, ownerGroup, jpId);
				canvas.refreshGraph();
			});
			menu.getItems().add(unexpose);
		}

		if (menu.getItems().isEmpty())
			return;

		show(menu, e);
	}

	private boolean isNodeId(UUID id) {
		if (canvas.getGraph() == null)
			return false;
		return canvas.getGraph().getNodes().stream().anyMatch(n -> n.getId().equals(id));
	}

	private boolean isGroupId(UUID id) {
		if (canvas.getGraph() == null)
			return false;
		return canvas.getGraph().getGroups().stream().anyMatch(g -> g.getId().equals(id));
	}

	private UUID findJoinPointAt(MouseEvent e) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != canvas) {
			Object jpId = target.getProperties().get("joinPointId");
			if (jpId instanceof UUID)
				return (UUID) jpId;
			target = target.getParent();
		}
		return null;
	}

	private Group findGroupOwningJoinPoint(Graph graph, UUID jpId) {
		for (Group group : graph.getGroups()) {
			if (group.getExposedJoinPoints().stream().anyMatch(jp -> jp.getId().equals(jpId)))
				return group;
		}
		return null;
	}

	private Node findNodeOwningJoinPoint(Graph graph, UUID jpId) {
		for (Node node : graph.getNodes()) {
			if (node.getJoinPoints().stream().anyMatch(jp -> jp.getId().equals(jpId)))
				return node;
		}
		return null;
	}

	private JoinPoint findJoinPointById(Graph graph, UUID jpId) {
		return GroupBuilder.findJoinPoint(graph, jpId);
	}

	private void show(ContextMenu menu, MouseEvent e) {
		activeMenu = menu;
		menu.show(canvas, e.getScreenX(), e.getScreenY());
	}
}
