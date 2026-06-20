package cz.bliksoft.javautils.fx.controls.graph.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.command.CreateEdgeCommand;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder;
import cz.bliksoft.javautils.fx.controls.graph.render.JoinPointRenderer;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class ConnectionHandler {

	private final GraphCanvas canvas;

	private UUID sourceJoinPointId;
	private UUID sourceNodeId;
	private Line previewLine;
	private final List<javafx.scene.Node> highlightedTargets = new ArrayList<>();

	private UUID repositioningJpId;
	private cz.bliksoft.dataflow.model.Group repositioningGroup;

	public ConnectionHandler(GraphCanvas canvas) {
		this.canvas = canvas;
		setupHandlers();
	}

	private UUID repositioningOwnerNodeId;

	private void setupHandlers() {
		canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
		canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
		canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
		canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
	}

	private void onMouseMoved(MouseEvent e) {
		UUID jpId = findJoinPointAt(e);
		if (jpId != null && e.isShiftDown()) {
			canvas.setCursor(javafx.scene.Cursor.MOVE);
		} else if (jpId != null) {
			canvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
		} else {
			canvas.setCursor(javafx.scene.Cursor.DEFAULT);
		}
	}

	private void handleShiftDoubleClick(UUID jpId) {
		cz.bliksoft.dataflow.model.Group group = findGroupOwningExposedJp(jpId);
		if (group != null) {
			JoinPoint jp = findJoinPoint(jpId);
			if (jp != null)
				GroupBuilder.positionSingleExposedJoinPoint(canvas.getGraph(), group, jp);
			canvas.refreshGraph();
			return;
		}

		JoinPoint jp = findJoinPoint(jpId);
		if (jp != null) {
			Node ownerNode = findNodeOwningJp(jpId);
			if (ownerNode != null) {
				resetJoinPointToDefault(jp, ownerNode);
				canvas.refreshGraph();
			}
		}
	}

	private void onMousePressed(MouseEvent e) {
		if (e.getButton() != MouseButton.PRIMARY)
			return;

		UUID jpId = findJoinPointAt(e);
		if (jpId == null)
			return;

		if (e.isShiftDown()) {
			if (e.getClickCount() == 2) {
				handleShiftDoubleClick(jpId);
				e.consume();
				return;
			}
			repositioningJpId = jpId;
			repositioningGroup = findGroupOwningExposedJp(jpId);
			if (repositioningGroup == null) {
				Node owner = findNodeOwningJp(jpId);
				if (owner != null)
					repositioningOwnerNodeId = owner.getId();
			}
			e.consume();
			return;
		}

		sourceJoinPointId = jpId;
		sourceNodeId = findOwnerNode(jpId);

		javafx.geometry.Point2D pos = getJoinPointScreenPos(jpId);
		if (pos == null)
			return;

		javafx.geometry.Point2D local = canvas.getContentPane().screenToLocal(e.getScreenX(), e.getScreenY());
		if (local == null)
			return;

		javafx.geometry.Point2D sourceLocal = canvas.getContentPane().screenToLocal(pos.getX(), pos.getY());
		if (sourceLocal == null)
			return;

		previewLine = new Line(sourceLocal.getX(), sourceLocal.getY(), local.getX(), local.getY());
		previewLine.setStroke(Color.web("#3399ff"));
		previewLine.setStrokeWidth(2);
		previewLine.getStrokeDashArray().addAll(6.0, 4.0);
		previewLine.setMouseTransparent(true);
		canvas.getContentPane().getChildren().add(previewLine);

		highlightCompatibleTargets();
		e.consume();
	}

	private void onMouseDragged(MouseEvent e) {
		if (repositioningJpId != null) {
			handleRepositionDrag(e);
			e.consume();
			return;
		}

		if (previewLine == null)
			return;

		javafx.geometry.Point2D local = canvas.getContentPane().screenToLocal(e.getScreenX(), e.getScreenY());
		if (local != null) {
			previewLine.setEndX(local.getX());
			previewLine.setEndY(local.getY());
		}
		e.consume();
	}

	private void onMouseReleased(MouseEvent e) {
		if (repositioningJpId != null) {
			repositioningJpId = null;
			repositioningGroup = null;
			repositioningOwnerNodeId = null;
			canvas.refreshGraph();
			e.consume();
			return;
		}

		if (previewLine == null)
			return;

		boolean created = false;
		UUID targetJpId = findJoinPointAt(e);

		if (targetJpId != null && !targetJpId.equals(sourceJoinPointId)) {
			created = tryCreateConnection(targetJpId);
		} else if (targetJpId == null) {
			UUID groupId = findGroupHeaderAt(e);
			if (groupId != null)
				created = tryCreateExposedJoinPoint(groupId);
		}

		cleanup();

		if (created)
			canvas.refreshGraph();

		e.consume();
	}

	private boolean tryCreateConnection(UUID targetJpId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return false;

		JoinPoint sourceJp = findJoinPoint(sourceJoinPointId);
		JoinPoint targetJp = findJoinPoint(targetJpId);
		if (sourceJp == null || targetJp == null)
			return false;

		if (!sourceJp.getDirection().canConnectTo(targetJp.getDirection()))
			return false;

		UUID sourceOwnerId = findOwnerNode(sourceJoinPointId);
		UUID targetOwnerId = findOwnerNode(targetJpId);

		if (sourceOwnerId != null && targetOwnerId != null) {
			boolean sourceIsNode = !isGroupId(sourceOwnerId);
			boolean targetIsNode = !isGroupId(targetOwnerId);

			if (sourceIsNode && targetIsNode) {
				int bordersCrossed = GroupBuilder.countBordersCrossed(graph, sourceOwnerId, targetOwnerId);
				if (bordersCrossed > 1)
					return false;

				if (bordersCrossed == 1) {
					cz.bliksoft.dataflow.model.Group sourceGroup = GroupBuilder.findGroupContaining(graph,
							sourceOwnerId);
					cz.bliksoft.dataflow.model.Group targetGroup = GroupBuilder.findGroupContaining(graph,
							targetOwnerId);
					return createCrossBorderLink(graph, sourceGroup, targetGroup, targetJpId);
				}
			}

			if (sourceIsNode && !targetIsNode) {
				cz.bliksoft.dataflow.model.Group sourceGroup = GroupBuilder.findGroupContaining(graph, sourceOwnerId);
				if (sourceGroup != null && !sourceGroup.getId().equals(targetOwnerId)
						&& !isTransitivelyInside(graph, targetOwnerId, false, sourceGroup)) {
					return createCrossBorderLink(graph, sourceGroup, null, targetJpId);
				}
			}

			if (!sourceIsNode && targetIsNode) {
				cz.bliksoft.dataflow.model.Group targetGroup = GroupBuilder.findGroupContaining(graph, targetOwnerId);
				if (targetGroup != null && !targetGroup.getId().equals(sourceOwnerId)
						&& !isTransitivelyInside(graph, sourceOwnerId, false, targetGroup)) {
					return createCrossBorderLink(graph, null, targetGroup, targetJpId);
				}
			}

			if (!sourceIsNode) {
				cz.bliksoft.dataflow.model.Group parentOfSource = findParentGroup(graph, sourceOwnerId);
				if (parentOfSource != null) {
					boolean targetInParent = isTransitivelyInside(graph, targetOwnerId, targetIsNode, parentOfSource);
					if (!targetInParent) {
						return createCrossBorderLink(graph, parentOfSource, null, targetJpId);
					}
				}
			}

			if (!targetIsNode) {
				cz.bliksoft.dataflow.model.Group parentOfTarget = findParentGroup(graph, targetOwnerId);
				if (parentOfTarget != null) {
					boolean sourceInParent = isTransitivelyInside(graph, sourceOwnerId, sourceIsNode, parentOfTarget);
					if (!sourceInParent) {
						return createCrossBorderLink(graph, null, parentOfTarget, targetJpId);
					}
				}
			}
		}

		if (canConnect(sourceJoinPointId, targetJpId)) {
			Edge edge = createOrientedEdge(sourceJoinPointId, targetJpId);
			CreateEdgeCommand cmd = new CreateEdgeCommand(canvas.getGraph(), edge);
			canvas.getCommandHistory().execute(cmd);
			return true;
		}

		return false;
	}

	private boolean createCrossBorderLink(Graph graph, cz.bliksoft.dataflow.model.Group sourceGroup,
			cz.bliksoft.dataflow.model.Group targetGroup, UUID targetJpId) {
		java.util.List<Edge> edgesBefore = new java.util.ArrayList<>(graph.getEdges());
		java.util.List<JoinPoint> exposedBefore = sourceGroup != null
				? new java.util.ArrayList<>(sourceGroup.getExposedJoinPoints())
				: (targetGroup != null ? new java.util.ArrayList<>(targetGroup.getExposedJoinPoints()) : null);

		if (sourceGroup != null) {
			JoinPoint exposed = GroupBuilder.getOrCreateExposedJoinPoint(graph, sourceGroup, sourceJoinPointId);
			if (exposed == null)
				return false;
			graph.getEdges().add(createOrientedEdge(exposed.getId(), targetJpId));
		} else if (targetGroup != null) {
			JoinPoint exposed = GroupBuilder.getOrCreateExposedJoinPoint(graph, targetGroup, targetJpId);
			if (exposed == null)
				return false;
			graph.getEdges().add(createOrientedEdge(sourceJoinPointId, exposed.getId()));
		}

		java.util.List<Edge> newEdges = new java.util.ArrayList<>(graph.getEdges());
		newEdges.removeAll(edgesBefore);

		cz.bliksoft.dataflow.model.Group affectedGroup = sourceGroup != null ? sourceGroup : targetGroup;
		java.util.List<JoinPoint> newExposed = new java.util.ArrayList<>(affectedGroup.getExposedJoinPoints());
		if (exposedBefore != null)
			newExposed.removeAll(exposedBefore);

		canvas.getCommandHistory().execute(new cz.bliksoft.javautils.fx.controls.graph.command.IGraphCommand() {
			private final java.util.List<Edge> addedEdges = new java.util.ArrayList<>(newEdges);
			private final java.util.List<JoinPoint> addedExposed = new java.util.ArrayList<>(newExposed);
			private final cz.bliksoft.dataflow.model.Group group = affectedGroup;

			@Override
			public void execute() {
			}

			@Override
			public void undo() {
				for (Edge e : addedEdges)
					graph.getEdges().remove(e);
				for (JoinPoint jp : addedExposed) {
					group.getExposedJoinPoints().remove(jp);
					group.getJoinPointMappings().removeIf(m -> m.getExposedId().equals(jp.getId()));
					group.getMemberEdgeIds().removeIf(id -> {
						for (Edge e : addedEdges)
							if (e.getId().equals(id))
								return true;
						return false;
					});
				}
			}

			@Override
			public void redo() {
				for (JoinPoint jp : addedExposed) {
					if (!group.getExposedJoinPoints().contains(jp)) {
						group.getExposedJoinPoints().add(jp);
						GroupBuilder.positionSingleExposedJoinPoint(graph, group, jp);
					}
				}
				for (Edge e : addedEdges) {
					if (!graph.getEdges().contains(e))
						graph.getEdges().add(e);
				}
			}

			@Override
			public String getDescription() {
				return "Create cross-border connection";
			}
		});

		return true;
	}

	private boolean tryCreateExposedJoinPoint(UUID groupId) {
		Graph graph = canvas.getGraph();
		if (graph == null || sourceJoinPointId == null)
			return false;

		cz.bliksoft.dataflow.model.Group group = GroupBuilder.findGroupById(graph, groupId);
		if (group == null)
			return false;

		cz.bliksoft.dataflow.model.Group sourceOwnerGroup = findGroupOwningExposedJp(sourceJoinPointId);
		if (sourceOwnerGroup != null && sourceOwnerGroup.getId().equals(groupId)) {
			if (!GroupBuilder.hasConnections(graph, sourceJoinPointId)) {
				JoinPoint removedJp = findJoinPoint(sourceJoinPointId);
				UUID removedJpId = sourceJoinPointId;
				cz.bliksoft.dataflow.model.JoinPointMapping removedMapping = null;
				for (var m : group.getJoinPointMappings()) {
					if (m.getExposedId().equals(removedJpId)) {
						removedMapping = m;
						break;
					}
				}
				java.util.List<Edge> removedEdges = new java.util.ArrayList<>();
				for (Edge e : graph.getEdges()) {
					if (e.getSourceJoinPointId().equals(removedJpId) || e.getTargetJoinPointId().equals(removedJpId))
						removedEdges.add(e);
				}
				GroupBuilder.removeExposedJoinPoint(graph, sourceOwnerGroup, removedJpId);
				final cz.bliksoft.dataflow.model.JoinPointMapping fMapping = removedMapping;
				canvas.getCommandHistory().execute(new cz.bliksoft.javautils.fx.controls.graph.command.IGraphCommand() {
					@Override
					public void execute() {
					}

					@Override
					public void undo() {
						if (removedJp != null)
							group.getExposedJoinPoints().add(removedJp);
						if (fMapping != null)
							group.getJoinPointMappings().add(fMapping);
						graph.getEdges().addAll(removedEdges);
					}

					@Override
					public void redo() {
						GroupBuilder.removeExposedJoinPoint(graph, group, removedJpId);
					}

					@Override
					public String getDescription() {
						return "Remove exposed joint";
					}
				});
				return true;
			}
			return false;
		}

		UUID sourceOwnerId = findOwnerNode(sourceJoinPointId);
		if (sourceOwnerId == null || !group.getMemberNodeIds().contains(sourceOwnerId))
			return false;

		java.util.List<JoinPoint> exposedBefore = new java.util.ArrayList<>(group.getExposedJoinPoints());
		java.util.List<Edge> edgesBefore = new java.util.ArrayList<>(graph.getEdges());

		GroupBuilder.getOrCreateExposedJoinPoint(graph, group, sourceJoinPointId);

		java.util.List<JoinPoint> addedExposed = new java.util.ArrayList<>(group.getExposedJoinPoints());
		addedExposed.removeAll(exposedBefore);
		java.util.List<Edge> addedEdges = new java.util.ArrayList<>(graph.getEdges());
		addedEdges.removeAll(edgesBefore);

		canvas.getCommandHistory().execute(new cz.bliksoft.javautils.fx.controls.graph.command.IGraphCommand() {
			@Override
			public void execute() {
			}

			@Override
			public void undo() {
				for (Edge e : addedEdges)
					graph.getEdges().remove(e);
				for (JoinPoint jp : addedExposed) {
					group.getExposedJoinPoints().remove(jp);
					group.getJoinPointMappings().removeIf(m -> m.getExposedId().equals(jp.getId()));
				}
			}

			@Override
			public void redo() {
				for (JoinPoint jp : addedExposed) {
					if (!group.getExposedJoinPoints().contains(jp)) {
						group.getExposedJoinPoints().add(jp);
						GroupBuilder.positionSingleExposedJoinPoint(graph, group, jp);
					}
				}
				for (Edge e : addedEdges) {
					if (!graph.getEdges().contains(e))
						graph.getEdges().add(e);
				}
			}

			@Override
			public String getDescription() {
				return "Expose joint on group";
			}
		});
		return true;
	}

	private UUID findGroupHeaderAt(MouseEvent e) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != canvas) {
			Object groupId = target.getProperties().get("groupId");
			if (groupId instanceof UUID)
				return (UUID) groupId;
			target = target.getParent();
		}
		return null;
	}

	private cz.bliksoft.dataflow.model.Group findParentGroup(Graph graph, UUID groupId) {
		for (cz.bliksoft.dataflow.model.Group g : graph.getGroups()) {
			if (g.getMemberGroupIds().contains(groupId))
				return g;
		}
		return null;
	}

	private boolean isTransitivelyInside(Graph graph, UUID elementId, boolean isNode,
			cz.bliksoft.dataflow.model.Group container) {
		if (isNode) {
			if (container.getMemberNodeIds().contains(elementId))
				return true;
			for (UUID childGroupId : container.getMemberGroupIds()) {
				cz.bliksoft.dataflow.model.Group child = GroupBuilder.findGroupById(graph, childGroupId);
				if (child != null && isTransitivelyInside(graph, elementId, true, child))
					return true;
			}
			return false;
		} else {
			return container.getId().equals(elementId) || container.getMemberGroupIds().contains(elementId);
		}
	}

	private boolean isGroupId(UUID id) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return false;
		return graph.getGroups().stream().anyMatch(g -> g.getId().equals(id));
	}

	private Edge createOrientedEdge(UUID jpIdA, UUID jpIdB) {
		JoinPoint jpA = findJoinPoint(jpIdA);
		JoinPoint jpB = findJoinPoint(jpIdB);
		if (jpA == null || jpB == null)
			return new Edge("default", jpIdA, jpIdB);

		cz.bliksoft.dataflow.model.Direction dirA = jpA.getDirection();
		cz.bliksoft.dataflow.model.Direction dirB = jpB.getDirection();

		boolean aBidir = dirA == cz.bliksoft.dataflow.model.Direction.BIDIR;
		boolean bBidir = dirB == cz.bliksoft.dataflow.model.Direction.BIDIR;

		if (aBidir && bBidir) {
			Edge edge = new Edge("default", jpIdA, jpIdB);
			edge.setDirectionality(cz.bliksoft.dataflow.model.Directionality.BIDIRECTIONAL);
			return edge;
		}

		boolean aIsOut = dirA == cz.bliksoft.dataflow.model.Direction.OUT
				|| dirA == cz.bliksoft.dataflow.model.Direction.OUT_IN;
		boolean bIsOut = dirB == cz.bliksoft.dataflow.model.Direction.OUT
				|| dirB == cz.bliksoft.dataflow.model.Direction.OUT_IN;

		if (aIsOut && !bIsOut)
			return new Edge("default", jpIdA, jpIdB);
		if (bIsOut && !aIsOut)
			return new Edge("default", jpIdB, jpIdA);

		if (aBidir)
			return bIsOut ? new Edge("default", jpIdB, jpIdA) : new Edge("default", jpIdA, jpIdB);
		if (bBidir)
			return aIsOut ? new Edge("default", jpIdA, jpIdB) : new Edge("default", jpIdB, jpIdA);

		return new Edge("default", jpIdA, jpIdB);
	}

	private void cleanup() {
		if (previewLine != null) {
			canvas.getContentPane().getChildren().remove(previewLine);
			previewLine = null;
		}
		for (javafx.scene.Node c : highlightedTargets)
			c.setEffect(null);
		highlightedTargets.clear();
		sourceJoinPointId = null;
		sourceNodeId = null;
	}

	private void highlightCompatibleTargets() {
		Graph graph = canvas.getGraph();
		if (graph == null || sourceJoinPointId == null)
			return;

		JoinPoint sourceJp = findJoinPoint(sourceJoinPointId);
		if (sourceJp == null)
			return;

		for (Node node : graph.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints()) {
				if (jp.getId().equals(sourceJoinPointId))
					continue;
				if (canConnect(sourceJp, jp, node.getId())) {
					javafx.scene.Node indicator = findJoinPointIndicator(jp.getId());
					if (indicator != null) {
						indicator.setEffect(new javafx.scene.effect.DropShadow(8, Color.web("#3399ff")));
						highlightedTargets.add(indicator);
					}
				}
			}
		}
	}

	boolean canConnect(UUID sourceJpId, UUID targetJpId) {
		JoinPoint source = findJoinPoint(sourceJpId);
		JoinPoint target = findJoinPoint(targetJpId);
		if (source == null || target == null)
			return false;

		UUID targetNodeId = findOwnerNode(targetJpId);
		return canConnect(source, target, targetNodeId);
	}

	private boolean canConnect(JoinPoint source, JoinPoint target, UUID targetNodeId) {
		if (!source.getDirection().canConnectTo(target.getDirection()))
			return false;

		if (sourceNodeId != null && sourceNodeId.equals(targetNodeId))
			return false;

		if (hasDuplicateEdge(source.getId(), target.getId()))
			return false;

		if (target.getMaxConnections() > 0) {
			int existing = countConnections(target.getId());
			if (existing >= target.getMaxConnections())
				return false;
		}
		if (source.getMaxConnections() > 0) {
			int existing = countConnections(source.getId());
			if (existing >= source.getMaxConnections())
				return false;
		}

		return true;
	}

	private int countConnections(UUID joinPointId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return 0;
		int count = 0;
		for (Edge edge : graph.getEdges()) {
			if (edge.getSourceJoinPointId().equals(joinPointId) || edge.getTargetJoinPointId().equals(joinPointId))
				count++;
		}
		return count;
	}

	private boolean hasDuplicateEdge(UUID jpIdA, UUID jpIdB) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return false;
		for (Edge edge : graph.getEdges()) {
			if ((edge.getSourceJoinPointId().equals(jpIdA) && edge.getTargetJoinPointId().equals(jpIdB))
					|| (edge.getSourceJoinPointId().equals(jpIdB) && edge.getTargetJoinPointId().equals(jpIdA)))
				return true;
		}
		return false;
	}

	private JoinPoint findJoinPoint(UUID jpId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return null;
		for (Node node : graph.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints()) {
				if (jp.getId().equals(jpId))
					return jp;
			}
		}
		for (cz.bliksoft.dataflow.model.Group group : graph.getGroups()) {
			for (JoinPoint jp : group.getExposedJoinPoints()) {
				if (jp.getId().equals(jpId))
					return jp;
			}
		}
		return null;
	}

	private UUID findOwnerNode(UUID jpId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return null;
		for (Node node : graph.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints())
				if (jp.getId().equals(jpId))
					return node.getId();
		}
		for (cz.bliksoft.dataflow.model.Group group : graph.getGroups()) {
			for (JoinPoint jp : group.getExposedJoinPoints())
				if (jp.getId().equals(jpId))
					return group.getId();
		}
		return null;
	}

	private void handleRepositionDrag(MouseEvent e) {
		if (repositioningJpId == null)
			return;

		javafx.geometry.Point2D local = canvas.getContentPane().screenToLocal(e.getScreenX(), e.getScreenY());
		if (local == null)
			return;

		JoinPoint jp = findJoinPoint(repositioningJpId);
		if (jp == null)
			return;

		if (repositioningGroup != null) {
			repositionOnGroupBorder(jp, local.getX(), local.getY());
		} else if (repositioningOwnerNodeId != null) {
			repositionOnNode(jp, local.getX(), local.getY());
		}

		canvas.refreshGraph();
	}

	private void repositionOnGroupBorder(JoinPoint jp, double px, double py) {
		double gx = repositioningGroup.getX(), gy = repositioningGroup.getY();
		double gw = repositioningGroup.getWidth(), gh = repositioningGroup.getHeight();

		double dTop = Math.abs(py - gy);
		double dBottom = Math.abs(py - (gy + gh));
		double dLeft = Math.abs(px - gx);
		double dRight = Math.abs(px - (gx + gw));
		double min = Math.min(Math.min(dTop, dBottom), Math.min(dLeft, dRight));

		jp.setPosition(cz.bliksoft.dataflow.model.JoinPointPosition.CUSTOM);
		if (min == dLeft) {
			jp.setCustomX(0);
			jp.setCustomY(clamp((py - gy) / gh, 0.05, 0.95));
		} else if (min == dRight) {
			jp.setCustomX(1);
			jp.setCustomY(clamp((py - gy) / gh, 0.05, 0.95));
		} else if (min == dTop) {
			jp.setCustomX(clamp((px - gx) / gw, 0.05, 0.95));
			jp.setCustomY(0);
		} else {
			jp.setCustomX(clamp((px - gx) / gw, 0.05, 0.95));
			jp.setCustomY(1);
		}
	}

	private void repositionOnNode(JoinPoint jp, double px, double py) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return;
		for (Node node : graph.getNodes()) {
			if (node.getId().equals(repositioningOwnerNodeId)) {
				double nx = node.getX(), ny = node.getY();
				double nw = node.getWidth(), nh = node.getHeight();

				double dTop = Math.abs(py - ny);
				double dBottom = Math.abs(py - (ny + nh));
				double dLeft = Math.abs(px - nx);
				double dRight = Math.abs(px - (nx + nw));
				double min = Math.min(Math.min(dTop, dBottom), Math.min(dLeft, dRight));

				jp.setPosition(cz.bliksoft.dataflow.model.JoinPointPosition.CUSTOM);
				if (min == dLeft) {
					jp.setCustomX(0);
					jp.setCustomY(clamp((py - ny) / nh, 0.05, 0.95));
				} else if (min == dRight) {
					jp.setCustomX(1);
					jp.setCustomY(clamp((py - ny) / nh, 0.05, 0.95));
				} else if (min == dTop) {
					jp.setCustomX(clamp((px - nx) / nw, 0.05, 0.95));
					jp.setCustomY(0);
				} else {
					jp.setCustomX(clamp((px - nx) / nw, 0.05, 0.95));
					jp.setCustomY(1);
				}
				return;
			}
		}
	}

	private Node findNodeOwningJp(UUID jpId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return null;
		for (Node node : graph.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints())
				if (jp.getId().equals(jpId))
					return node;
		}
		return null;
	}

	private void resetJoinPointToDefault(JoinPoint jp, Node ownerNode) {
		cz.bliksoft.dataflow.types.NodeType type = cz.bliksoft.dataflow.types.NodeTypeRegistry.getInstance()
				.get(ownerNode.getTypeId());
		if (type == null)
			return;
		for (var def : type.getDefaultJoinPoints()) {
			if (def.getName().equals(jp.getName())) {
				jp.setPosition(def.getPosition());
				jp.setCustomX(0);
				jp.setCustomY(0);
				return;
			}
		}
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	private cz.bliksoft.dataflow.model.Group findGroupOwningExposedJp(UUID jpId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return null;
		for (cz.bliksoft.dataflow.model.Group group : graph.getGroups()) {
			for (JoinPoint jp : group.getExposedJoinPoints()) {
				if (jp.getId().equals(jpId))
					return group;
			}
		}
		return null;
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

	private javafx.scene.Node findJoinPointIndicator(UUID jpId) {
		return findIndicatorIn(canvas.getContentPane(), jpId);
	}

	private javafx.scene.Node findIndicatorIn(javafx.scene.Parent parent, UUID jpId) {
		for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
			if (jpId.equals(child.getProperties().get("joinPointId")))
				return child;
			if (child instanceof javafx.scene.Parent p) {
				javafx.scene.Node found = findIndicatorIn(p, jpId);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	private javafx.geometry.Point2D getJoinPointScreenPos(UUID jpId) {
		Graph graph = canvas.getGraph();
		if (graph == null)
			return null;
		for (Node node : graph.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints()) {
				if (jp.getId().equals(jpId)) {
					double[] rel = JoinPointRenderer.computePosition(jp.getPosition(), jp.getCustomX(), jp.getCustomY(),
							node.getWidth(), node.getHeight());
					javafx.scene.layout.Region visual = canvas.getNodeVisual(node.getId());
					if (visual == null)
						return null;
					return visual.localToScreen(rel[0], rel[1]);
				}
			}
		}
		for (cz.bliksoft.dataflow.model.Group group : graph.getGroups()) {
			for (JoinPoint jp : group.getExposedJoinPoints()) {
				if (jp.getId().equals(jpId)) {
					double w = Math.max(group.getWidth(), 80);
					double h = Math.max(group.getHeight(), 50);
					double[] rel = JoinPointRenderer.computePosition(jp.getPosition(), jp.getCustomX(), jp.getCustomY(),
							w, h);
					javafx.scene.Node indicator = findJoinPointIndicator(jpId);
					if (indicator != null)
						return indicator.localToScreen(0, 0);
					return null;
				}
			}
		}
		return null;
	}
}
