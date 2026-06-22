package cz.bliksoft.javautils.fx.controls.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.model.Point2D;
import cz.bliksoft.dataflow.types.EdgeType;
import cz.bliksoft.dataflow.types.EdgeTypeRegistry;
import cz.bliksoft.dataflow.types.NodeType;
import cz.bliksoft.dataflow.types.NodeTypeRegistry;
import cz.bliksoft.javautils.fx.controls.graph.command.GraphCommandHistory;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupInteractionHandler;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupRenderer;
import cz.bliksoft.javautils.fx.controls.graph.interaction.ClipboardHandler;
import cz.bliksoft.javautils.fx.controls.graph.interaction.ConnectionHandler;
import cz.bliksoft.javautils.fx.controls.graph.interaction.GraphContextMenus;
import cz.bliksoft.javautils.fx.controls.graph.interaction.GraphSelectionModel;
import cz.bliksoft.javautils.fx.controls.graph.interaction.NodeInteractionHandler;
import cz.bliksoft.javautils.fx.controls.graph.render.EdgeRendererRegistry;
import cz.bliksoft.javautils.fx.controls.graph.render.IEdgeRenderer;
import cz.bliksoft.javautils.fx.controls.graph.render.INodeRenderer;
import cz.bliksoft.javautils.fx.controls.graph.render.JoinPointRenderer;
import cz.bliksoft.javautils.fx.controls.graph.render.NodeRendererRegistry;
import cz.bliksoft.javautils.fx.controls.graph.render.RenderContext;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class GraphCanvas extends Region {

	private static final double MIN_ZOOM = 0.1;
	private static final double MAX_ZOOM = 4.0;
	private static final double ZOOM_STEP = 1.1;
	private static final double SCROLL_SPEED = 20.0;

	private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);
	private final DoubleProperty scrollX = new SimpleDoubleProperty(0);
	private final DoubleProperty scrollY = new SimpleDoubleProperty(0);
	private final ObjectProperty<GridStyle> gridStyle = new SimpleObjectProperty<>(GridStyle.DOT);
	private final DoubleProperty gridSpacing = new SimpleDoubleProperty(20);
	private final BooleanProperty snapToGrid = new SimpleBooleanProperty(false);

	private final java.util.List<Runnable> postRefreshCallbacks = new java.util.ArrayList<>();

	private final Canvas backgroundCanvas;
	private final Pane edgePane;
	private final Pane groupPane;
	private final Pane nodePane;
	private final Pane contentPane;
	private final Map<UUID, Region> nodeVisuals = new HashMap<>();
	private final Map<UUID, javafx.scene.Group> edgeVisuals = new HashMap<>();
	private final GraphSelectionModel selectionModel = new GraphSelectionModel();
	private final GraphCommandHistory commandHistory = new GraphCommandHistory();
	private final NodeInteractionHandler interactionHandler;
	private final ConnectionHandler connectionHandler;
	private final ClipboardHandler clipboardHandler;
	private final GraphContextMenus contextMenus;
	private final GroupInteractionHandler groupHandler;

	private Group graph;
	private Group rootGraph;

	private double panStartX, panStartY;
	private double panStartScrollX, panStartScrollY;
	private boolean panning;

	private javafx.scene.shape.Rectangle rubberBand;
	private double rubberStartX, rubberStartY;

	public GraphCanvas() {
		getStyleClass().add("graph-canvas");
		getStylesheets().add(getClass().getResource("/css/graph-canvas.css").toExternalForm());

		backgroundCanvas = new Canvas();
		backgroundCanvas.getStyleClass().add("graph-background");
		backgroundCanvas.setManaged(false);

		edgePane = new Pane();
		edgePane.setPickOnBounds(false);

		groupPane = new Pane();
		groupPane.setPickOnBounds(false);

		nodePane = new Pane();
		nodePane.setPickOnBounds(false);

		contentPane = new Pane(groupPane, edgePane, nodePane);
		contentPane.getStyleClass().add("graph-content");
		contentPane.setManaged(false);
		contentPane.setPickOnBounds(false);

		setClip(new javafx.scene.shape.Rectangle());
		layoutBoundsProperty().addListener((obs, o, n) -> {
			javafx.scene.shape.Rectangle clip = (javafx.scene.shape.Rectangle) getClip();
			clip.setWidth(n.getWidth());
			clip.setHeight(n.getHeight());
		});

		getChildren().addAll(backgroundCanvas, contentPane);

		bindContentTransform();
		setupScrollHandlers();
		setupPanHandlers();
		setupSelectionHandlers();
		setupRedrawTriggers();

		interactionHandler = new NodeInteractionHandler(this, commandHistory);
		connectionHandler = new ConnectionHandler(this);
		clipboardHandler = new ClipboardHandler(this);
		contextMenus = new GraphContextMenus(this);
		groupHandler = new GroupInteractionHandler(this);
	}

	private void bindContentTransform() {
		contentPane.scaleXProperty().bind(zoom);
		contentPane.scaleYProperty().bind(zoom);
		contentPane.translateXProperty().bind(scrollX);
		contentPane.translateYProperty().bind(scrollY);
	}

	private void setupScrollHandlers() {
		addEventFilter(ScrollEvent.SCROLL, e -> {
			if (e.isControlDown()) {
				handleZoom(e);
			} else {
				handleScroll(e);
			}
			e.consume();
		});
	}

	private void handleZoom(ScrollEvent e) {
		double oldZoom = zoom.get();
		double factor = e.getDeltaY() > 0 ? ZOOM_STEP : 1.0 / ZOOM_STEP;
		double newZoom = clampZoom(oldZoom * factor);

		if (newZoom == oldZoom)
			return;

		// zoom centered on cursor position
		double mouseX = e.getX();
		double mouseY = e.getY();

		double contentX = (mouseX - scrollX.get()) / oldZoom;
		double contentY = (mouseY - scrollY.get()) / oldZoom;

		zoom.set(newZoom);
		scrollX.set(mouseX - contentX * newZoom);
		scrollY.set(mouseY - contentY * newZoom);
	}

	private void handleScroll(ScrollEvent e) {
		if (e.isShiftDown()) {
			scrollX.set(scrollX.get() + e.getDeltaY() * SCROLL_SPEED / zoom.get());
		} else {
			scrollY.set(scrollY.get() + e.getDeltaY() * SCROLL_SPEED / zoom.get());
		}
	}

	private void setupPanHandlers() {
		addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			if (e.getButton() == MouseButton.MIDDLE) {
				panning = true;
				panStartX = e.getScreenX();
				panStartY = e.getScreenY();
				panStartScrollX = scrollX.get();
				panStartScrollY = scrollY.get();
				e.consume();
			}
		});

		addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			if (panning) {
				double dx = e.getScreenX() - panStartX;
				double dy = e.getScreenY() - panStartY;
				scrollX.set(panStartScrollX + dx);
				scrollY.set(panStartScrollY + dy);
				e.consume();
			}
		});

		addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (e.getButton() == MouseButton.MIDDLE && panning) {
				panning = false;
				e.consume();
			}
		});
	}

	private void setupSelectionHandlers() {
		addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
			requestFocus();

			if (e.getButton() != MouseButton.PRIMARY || panning)
				return;

			UUID clickedId = findElementAt(e);
			if (clickedId != null) {
				if (e.isControlDown()) {
					selectionModel.toggle(clickedId);
				} else if (!selectionModel.isSelected(clickedId)) {
					selectionModel.select(clickedId);
				}
				e.consume();
			} else {
				if (!e.isControlDown())
					selectionModel.clear();
				startRubberBand(e);
			}
			updateSelectionVisuals();
		});

		addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
			if (rubberBand != null)
				updateRubberBand(e);
		});

		addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
			if (rubberBand != null) {
				finishRubberBand(e);
				updateSelectionVisuals();
			}
		});
	}

	private UUID findElementAt(MouseEvent e) {
		javafx.scene.Node target = e.getPickResult().getIntersectedNode();
		while (target != null && target != contentPane) {
			Object nodeId = target.getProperties().get("nodeId");
			if (nodeId instanceof UUID)
				return (UUID) nodeId;
			Object edgeId = target.getProperties().get("edgeId");
			if (edgeId instanceof UUID)
				return (UUID) edgeId;
			Object groupId = target.getProperties().get("groupId");
			if (groupId instanceof UUID)
				return (UUID) groupId;
			target = target.getParent();
		}
		return null;
	}

	private void startRubberBand(MouseEvent e) {
		javafx.geometry.Point2D local = contentPane.screenToLocal(e.getScreenX(), e.getScreenY());
		if (local == null)
			return;
		rubberStartX = local.getX();
		rubberStartY = local.getY();
		rubberBand = new javafx.scene.shape.Rectangle(rubberStartX, rubberStartY, 0, 0);
		rubberBand.getStyleClass().add("selection-rect");
		rubberBand.setMouseTransparent(true);
		contentPane.getChildren().add(rubberBand);
	}

	private void updateRubberBand(MouseEvent e) {
		javafx.geometry.Point2D local = contentPane.screenToLocal(e.getScreenX(), e.getScreenY());
		if (local == null)
			return;
		double x = Math.min(rubberStartX, local.getX());
		double y = Math.min(rubberStartY, local.getY());
		double w = Math.abs(local.getX() - rubberStartX);
		double h = Math.abs(local.getY() - rubberStartY);
		rubberBand.setX(x);
		rubberBand.setY(y);
		rubberBand.setWidth(w);
		rubberBand.setHeight(h);
	}

	private void finishRubberBand(MouseEvent e) {
		javafx.geometry.Bounds rubberBounds = rubberBand.getBoundsInParent();
		contentPane.getChildren().remove(rubberBand);

		if (rubberBounds.getWidth() > 3 || rubberBounds.getHeight() > 3) {
			Group scope = findInnermostGroupContaining(graph, rubberBounds);

			Set<UUID> scopeChildIds = new java.util.HashSet<>();
			if (scope != null) {
				for (Node n : scope.getNodes())
					scopeChildIds.add(n.getId());
				for (Group g : scope.getGroups())
					scopeChildIds.add(g.getId());
			}

			Set<UUID> selected = new java.util.LinkedHashSet<>();
			for (var entry : nodeVisuals.entrySet()) {
				if (!scopeChildIds.contains(entry.getKey()))
					continue;
				javafx.geometry.Bounds nodeBounds = entry.getValue().getBoundsInParent();
				if (rubberBounds.intersects(nodeBounds))
					selected.add(entry.getKey());
			}
			if (e.isControlDown()) {
				selected.forEach(selectionModel::addToSelection);
			} else {
				selectionModel.selectAll(selected);
			}
		}

		rubberBand = null;
	}

	private Group findInnermostGroupContaining(Group parent, javafx.geometry.Bounds bounds) {
		if (parent == null)
			return graph;
		for (Group child : parent.getGroups()) {
			if (child.isCollapsed())
				continue;
			if (bounds.getMinX() >= child.getX() && bounds.getMinY() >= child.getY()
					&& bounds.getMaxX() <= child.getX() + child.getWidth()
					&& bounds.getMaxY() <= child.getY() + child.getHeight()) {
				return findInnermostGroupContaining(child, bounds);
			}
		}
		return parent;
	}

	private static final double RESIZE_HANDLE_SIZE = 6;

	public void updateSelectionVisuals() {
		Set<UUID> ownerGroupIds = new java.util.HashSet<>();

		for (var entry : nodeVisuals.entrySet()) {
			Region visual = entry.getValue();
			boolean selected = selectionModel.isSelected(entry.getKey());
			if (selected) {
				if (!visual.getStyleClass().contains("graph-node-selected"))
					visual.getStyleClass().add("graph-node-selected");
				addResizeHandle(visual, entry.getKey());

				if (graph != null) {
					cz.bliksoft.dataflow.model.Group owner = cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder
							.findGroupContaining(graph, entry.getKey());
					if (owner != null)
						ownerGroupIds.add(owner.getId());
				}
			} else {
				visual.getStyleClass().remove("graph-node-selected");
				removeResizeHandle(visual);
			}
		}
		for (var entry : edgeVisuals.entrySet()) {
			javafx.scene.Group visual = entry.getValue();
			if (selectionModel.isSelected(entry.getKey())) {
				if (!visual.getStyleClass().contains("graph-edge-selected"))
					visual.getStyleClass().add("graph-edge-selected");
			} else {
				visual.getStyleClass().remove("graph-edge-selected");
			}
		}

		for (var entry : nodeVisuals.entrySet()) {
			Region visual = entry.getValue();
			if (ownerGroupIds.contains(entry.getKey())) {
				if (!visual.getStyleClass().contains("graph-group-owner-highlight"))
					visual.getStyleClass().add("graph-group-owner-highlight");
			} else {
				visual.getStyleClass().remove("graph-group-owner-highlight");
			}
		}
	}

	private void addResizeHandle(Region visual, UUID elementId) {
		if (!(visual instanceof Pane pane))
			return;
		for (javafx.scene.Node child : pane.getChildren()) {
			if (child.getProperties().containsKey("resizeNodeId") || child.getProperties().containsKey("resizeGroupId"))
				return;
		}
		double w = pane.getPrefWidth();
		double h = pane.getPrefHeight();
		javafx.scene.shape.Rectangle handle = new javafx.scene.shape.Rectangle(w - RESIZE_HANDLE_SIZE,
				h - RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
		handle.setFill(javafx.scene.paint.Color.web("#3399ff"));
		handle.setOpacity(0.6);
		handle.setCursor(javafx.scene.Cursor.SE_RESIZE);

		boolean isGroup = visual.getStyleClass().contains("graph-group-expanded")
				|| visual.getStyleClass().contains("graph-group-collapsed");
		handle.getProperties().put(isGroup ? "resizeGroupId" : "resizeNodeId", elementId);
		pane.getChildren().add(handle);
	}

	private void removeResizeHandle(Region visual) {
		if (!(visual instanceof Pane pane))
			return;
		pane.getChildren().removeIf(child -> child.getProperties().containsKey("resizeNodeId")
				|| child.getProperties().containsKey("resizeGroupId"));
	}

	private void setupRedrawTriggers() {
		widthProperty().addListener((obs, o, n) -> redrawGrid());
		heightProperty().addListener((obs, o, n) -> redrawGrid());
		zoom.addListener((obs, o, n) -> redrawGrid());
		scrollX.addListener((obs, o, n) -> redrawGrid());
		scrollY.addListener((obs, o, n) -> redrawGrid());
		gridStyle.addListener((obs, o, n) -> redrawGrid());
		gridSpacing.addListener((obs, o, n) -> redrawGrid());
	}

	@Override
	protected void layoutChildren() {
		double w = getWidth();
		double h = getHeight();
		backgroundCanvas.setWidth(w);
		backgroundCanvas.setHeight(h);
		backgroundCanvas.relocate(0, 0);
		contentPane.relocate(0, 0);
		redrawGrid();
	}

	private void redrawGrid() {
		double w = backgroundCanvas.getWidth();
		double h = backgroundCanvas.getHeight();
		if (w <= 0 || h <= 0)
			return;

		GraphicsContext gc = backgroundCanvas.getGraphicsContext2D();
		gc.clearRect(0, 0, w, h);

		GridStyle style = gridStyle.get();
		if (style == GridStyle.NONE)
			return;

		double spacing = gridSpacing.get() * zoom.get();
		if (spacing < 4)
			return;

		double offsetX = scrollX.get() % spacing;
		double offsetY = scrollY.get() % spacing;

		gc.setFill(Color.rgb(180, 180, 180));
		gc.setStroke(Color.rgb(210, 210, 210));
		gc.setLineWidth(0.5);

		if (style == GridStyle.DOT) {
			double dotSize = Math.max(1.0, zoom.get());
			for (double x = offsetX; x < w; x += spacing) {
				for (double y = offsetY; y < h; y += spacing) {
					gc.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
				}
			}
		} else if (style == GridStyle.LINE) {
			for (double x = offsetX; x < w; x += spacing) {
				gc.strokeLine(x, 0, x, h);
			}
			for (double y = offsetY; y < h; y += spacing) {
				gc.strokeLine(0, y, w, y);
			}
		}
	}

	// --- public API ---

	public void setGraph(Group graph) {
		this.graph = graph;
		selectionModel.clear();
		renderGraph();
	}

	public Group getGraph() {
		return graph;
	}

	public void setRootGraph(Group rootGraph) {
		this.rootGraph = rootGraph;
	}

	public Group getRootGraph() {
		return rootGraph != null ? rootGraph : graph;
	}

	public GraphSelectionModel getSelectionModel() {
		return selectionModel;
	}

	private void renderGraph() {
		edgePane.getChildren().clear();
		nodePane.getChildren().clear();
		groupPane.getChildren().clear();
		nodeVisuals.clear();
		edgeVisuals.clear();

		if (graph == null)
			return;

		renderGroupContents(graph);
		renderRootExposedJoinPoints();
	}

	private void renderRootExposedJoinPoints() {
		if (graph == null || graph.getExposedJoinPoints().isEmpty())
			return;

		double gw = graph.getWidth();
		double gh = graph.getHeight();
		if (gw <= 0 || gh <= 0) {
			double[] bounds = computeContentBounds();
			if (bounds != null) {
				gw = bounds[2] - bounds[0] + 100;
				gh = bounds[3] - bounds[1] + 100;
			} else {
				gw = 800;
				gh = 600;
			}
		}

		for (JoinPoint jp : graph.getExposedJoinPoints()) {
			double[] rel = JoinPointRenderer.computePosition(jp.getPosition(), jp.getCustomX(), jp.getCustomY(), gw,
					gh);
			double px = graph.getX() + rel[0];
			double py = graph.getY() + rel[1];

			javafx.scene.shape.Rectangle portShape = new javafx.scene.shape.Rectangle(20, 14);
			portShape.setFill(javafx.scene.paint.Color.web("#e8e8f0"));
			portShape.setStroke(javafx.scene.paint.Color.web("#666"));
			portShape.setStrokeWidth(1);
			portShape.setArcWidth(4);
			portShape.setArcHeight(4);

			javafx.scene.control.Label portLabel = new javafx.scene.control.Label(jp.getName());
			portLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #444;");
			portLabel.setMouseTransparent(true);
			portLabel.setLayoutX(22);
			portLabel.setLayoutY(-2);

			Pane portNode = new Pane(portShape, portLabel);
			portNode.setManaged(false);
			portNode.setLayoutX(px - 10);
			portNode.setLayoutY(py - 7);
			portNode.setPickOnBounds(false);
			portNode.getProperties().put("exposedPortNode", true);

			JoinPointRenderer.renderJoinPoints(portNode, java.util.List.of(jp), 20, 14);

			nodePane.getChildren().add(portNode);
		}
	}

	private double[] computeContentBounds() {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (Node n : graph.getAllNodesRecursive()) {
			minX = Math.min(minX, n.getX());
			minY = Math.min(minY, n.getY());
			maxX = Math.max(maxX, n.getX() + n.getWidth());
			maxY = Math.max(maxY, n.getY() + n.getHeight());
		}
		for (Group g : graph.getAllGroupsRecursive()) {
			minX = Math.min(minX, g.getX());
			minY = Math.min(minY, g.getY());
			maxX = Math.max(maxX, g.getX() + g.getWidth());
			maxY = Math.max(maxY, g.getY() + g.getHeight());
		}
		if (minX >= Double.MAX_VALUE)
			return null;
		return new double[] { minX, minY, maxX, maxY };
	}

	private void renderGroupContents(Group group) {
		NodeRendererRegistry rendererRegistry = NodeRendererRegistry.getInstance();
		RenderContext ctx = new RenderContext(zoom.get(), false);

		java.util.List<Node> sortedNodes = new java.util.ArrayList<>(group.getNodes());
		sortedNodes.sort(java.util.Comparator.comparingInt(Node::getzOrder));

		for (Node node : sortedNodes) {
			NodeType type = NodeTypeRegistry.getInstance().get(node.getTypeId());
			INodeRenderer renderer = rendererRegistry.get(node.getTypeId());
			if (renderer == null || type == null)
				continue;

			Region visual = renderer.createNodeVisual(node, type, ctx);
			visual.setLayoutX(0);
			visual.setLayoutY(0);
			visual.getProperties().put("nodeId", node.getId());

			Pane wrapper = new Pane(visual);
			wrapper.setManaged(false);
			wrapper.setLayoutX(node.getX());
			wrapper.setLayoutY(node.getY());
			wrapper.setPrefSize(node.getWidth(), node.getHeight());
			wrapper.getProperties().put("nodeId", node.getId());

			JoinPointRenderer.renderJoinPoints(wrapper, node.getJoinPoints(), node.getWidth(), node.getHeight(),
					type.isShowJoinPointLabels());

			nodeVisuals.put(node.getId(), wrapper);
			nodePane.getChildren().add(wrapper);
		}

		renderGroupEdges(group);

		java.util.List<cz.bliksoft.dataflow.model.Group> sorted = new java.util.ArrayList<>(group.getGroups());
		sorted.sort((a, b) -> Double.compare(b.getWidth() * b.getHeight(), a.getWidth() * a.getHeight()));

		for (cz.bliksoft.dataflow.model.Group child : sorted) {
			Region groupVisual;
			if (child.isCollapsed()) {
				groupVisual = GroupRenderer.renderCollapsed(child);
				nodePane.getChildren().add(groupVisual);
			} else {
				groupVisual = GroupRenderer.renderExpanded(child, group);
				groupPane.getChildren().add(groupVisual);

				if (!child.getExposedJoinPoints().isEmpty()) {
					Pane jpOverlay = new Pane();
					jpOverlay.setManaged(false);
					jpOverlay.setLayoutX(child.getX());
					jpOverlay.setLayoutY(child.getY());
					jpOverlay.setPrefSize(child.getWidth(), child.getHeight());
					jpOverlay.setPickOnBounds(false);
					jpOverlay.getProperties().put("nodeId", child.getId());
					double w = child.getWidth(), h = child.getHeight();
					JoinPointRenderer.renderJoinPoints(jpOverlay, child.getExposedJoinPoints(), w, h);
					nodePane.getChildren().add(jpOverlay);
				}

				renderGroupContents(child);
			}
			nodeVisuals.put(child.getId(), groupVisual);
		}
	}

	private void renderGroupEdges(Group group) {
		EdgeRendererRegistry rendererRegistry = EdgeRendererRegistry.getInstance();
		RenderContext ctx = new RenderContext(zoom.get(), false);

		for (Edge edge : group.getEdges()) {
			EdgeType type = EdgeTypeRegistry.getInstance().get(edge.getTypeId());
			IEdgeRenderer renderer = rendererRegistry.get(edge.getTypeId());
			if (renderer == null)
				continue;

			Point2D sourcePos = resolveJoinPointPosition(edge.getSourceJoinPointId());
			Point2D targetPos = resolveJoinPointPosition(edge.getTargetJoinPointId());
			if (sourcePos == null || targetPos == null)
				continue;

			javafx.scene.Group edgeVisual = renderer.createEdgeVisual(edge, type, sourcePos, targetPos,
					edge.getWaypoints().isEmpty() ? null : edge.getWaypoints(), ctx);

			addConditionalMarker(edgeVisual, edge, sourcePos, targetPos);

			edgeVisuals.put(edge.getId(), edgeVisual);
			edgePane.getChildren().add(edgeVisual);
		}
	}

	private void addConditionalMarker(javafx.scene.Group edgeVisual, Edge edge, Point2D sourcePos, Point2D targetPos) {
		java.util.Map<String, Object> props = edge.getProperties();
		if (props == null || !props.containsKey("condition"))
			return;

		double midX = (sourcePos.getX() + targetPos.getX()) / 2;
		double midY = (sourcePos.getY() + targetPos.getY()) / 2;
		double size = 6;

		javafx.scene.shape.Polygon diamond = new javafx.scene.shape.Polygon(midX, midY - size, midX + size, midY, midX,
				midY + size, midX - size, midY);
		diamond.setFill(javafx.scene.paint.Color.web("#2196F3"));
		diamond.setStroke(javafx.scene.paint.Color.WHITE);
		diamond.setStrokeWidth(1);
		diamond.setMouseTransparent(true);
		diamond.getStyleClass().add("graph-edge-condition-marker");
		edgeVisual.getChildren().add(diamond);
	}

	private Point2D resolveJoinPointPosition(UUID joinPointId) {
		if (graph == null)
			return null;
		return resolveJoinPointInGroup(graph, joinPointId);
	}

	private Point2D resolveJoinPointInGroup(Group group, UUID joinPointId) {
		for (Node node : group.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints()) {
				if (jp.getId().equals(joinPointId)) {
					double[] rel = JoinPointRenderer.computePosition(jp.getPosition(), jp.getCustomX(), jp.getCustomY(),
							node.getWidth(), node.getHeight());
					return new Point2D(node.getX() + rel[0], node.getY() + rel[1]);
				}
			}
		}
		for (JoinPoint jp : group.getExposedJoinPoints()) {
			if (jp.getId().equals(joinPointId)) {
				double w = Math.max(group.getWidth(), 80);
				double h = Math.max(group.getHeight(), 50);
				double[] rel = JoinPointRenderer.computePosition(jp.getPosition(), jp.getCustomX(), jp.getCustomY(), w,
						h);
				return new Point2D(group.getX() + rel[0], group.getY() + rel[1]);
			}
		}
		for (cz.bliksoft.dataflow.model.Group child : group.getGroups()) {
			Point2D found = resolveJoinPointInGroup(child, joinPointId);
			if (found != null)
				return found;
		}
		return null;
	}

	public Region getNodeVisual(UUID nodeId) {
		return nodeVisuals.get(nodeId);
	}

	public javafx.scene.Group getEdgeVisual(UUID edgeId) {
		return edgeVisuals.get(edgeId);
	}

	public void addPostRefreshCallback(Runnable callback) {
		postRefreshCallbacks.add(callback);
	}

	private void firePostRefresh() {
		for (Runnable r : postRefreshCallbacks)
			r.run();
	}

	public void refreshGraph() {
		Set<UUID> selected = new java.util.LinkedHashSet<>(selectionModel.getSelection());
		renderGraph();
		selectionModel.selectAll(selected);
		updateSelectionVisuals();
		firePostRefresh();
	}

	public void refreshNodeVisual(UUID nodeId) {
		if (graph == null)
			return;

		Node node = graph.getNodes().stream().filter(n -> n.getId().equals(nodeId)).findFirst().orElse(null);
		if (node == null)
			return;

		Region oldVisual = nodeVisuals.get(nodeId);
		if (oldVisual == null)
			return;

		NodeType type = NodeTypeRegistry.getInstance().get(node.getTypeId());
		INodeRenderer renderer = NodeRendererRegistry.getInstance().get(node.getTypeId());
		if (renderer == null || type == null)
			return;

		int index = nodePane.getChildren().indexOf(oldVisual);
		nodePane.getChildren().remove(oldVisual);

		Region visual = renderer.createNodeVisual(node, type, new RenderContext(zoom.get(), false));
		visual.setLayoutX(0);
		visual.setLayoutY(0);
		visual.getProperties().put("nodeId", node.getId());

		Pane wrapper = new Pane(visual);
		wrapper.setManaged(false);
		wrapper.setLayoutX(node.getX());
		wrapper.setLayoutY(node.getY());
		wrapper.setPrefSize(node.getWidth(), node.getHeight());
		wrapper.getProperties().put("nodeId", node.getId());

		JoinPointRenderer.renderJoinPoints(wrapper, node.getJoinPoints(), node.getWidth(), node.getHeight(),
				type.isShowJoinPointLabels());

		nodeVisuals.put(nodeId, wrapper);
		if (index >= 0 && index < nodePane.getChildren().size())
			nodePane.getChildren().add(index, wrapper);
		else
			nodePane.getChildren().add(wrapper);

		if (selectionModel.isSelected(nodeId)) {
			if (!wrapper.getStyleClass().contains("graph-node-selected"))
				wrapper.getStyleClass().add("graph-node-selected");
		}

		edgePane.getChildren().clear();
		edgeVisuals.clear();
		groupPane.getChildren().clear();
		renderGroupContentsEdgesAndGroups(graph);
	}

	private void renderGroupContentsEdgesAndGroups(Group g) {
		renderGroupEdges(g);
		for (cz.bliksoft.dataflow.model.Group child : g.getGroups()) {
			if (!child.isCollapsed())
				renderGroupContentsEdgesAndGroups(child);
		}
	}

	public void refreshEdges() {
		edgePane.getChildren().clear();
		edgeVisuals.clear();
		groupPane.getChildren().clear();
		renderGroupContentsEdgesAndGroups(graph);
	}

	public Pane getContentPane() {
		return contentPane;
	}

	public GraphCommandHistory getCommandHistory() {
		return commandHistory;
	}

	public GroupInteractionHandler getGroupHandler() {
		return groupHandler;
	}

	public void zoomToFit() {
		if (graph == null || graph.getNodes().isEmpty()) {
			zoom.set(1.0);
			scrollX.set(0);
			scrollY.set(0);
			return;
		}

		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
		for (Node n : graph.getNodes()) {
			minX = Math.min(minX, n.getX());
			minY = Math.min(minY, n.getY());
			maxX = Math.max(maxX, n.getX() + n.getWidth());
			maxY = Math.max(maxY, n.getY() + n.getHeight());
		}

		double contentW = maxX - minX;
		double contentH = maxY - minY;
		if (contentW <= 0 || contentH <= 0)
			return;

		double padding = 40;
		double viewW = getWidth() - padding * 2;
		double viewH = getHeight() - padding * 2;
		if (viewW <= 0 || viewH <= 0)
			return;

		double fitZoom = clampZoom(Math.min(viewW / contentW, viewH / contentH));
		zoom.set(fitZoom);
		scrollX.set(padding - minX * fitZoom + (viewW - contentW * fitZoom) / 2);
		scrollY.set(padding - minY * fitZoom + (viewH - contentH * fitZoom) / 2);
	}

	public void resetZoom() {
		zoom.set(1.0);
	}

	public void zoomIn() {
		double w = getWidth() / 2;
		double h = getHeight() / 2;
		double oldZoom = zoom.get();
		double newZoom = clampZoom(oldZoom * ZOOM_STEP);
		if (newZoom == oldZoom)
			return;

		double cx = (w - scrollX.get()) / oldZoom;
		double cy = (h - scrollY.get()) / oldZoom;
		zoom.set(newZoom);
		scrollX.set(w - cx * newZoom);
		scrollY.set(h - cy * newZoom);
	}

	public void zoomOut() {
		double w = getWidth() / 2;
		double h = getHeight() / 2;
		double oldZoom = zoom.get();
		double newZoom = clampZoom(oldZoom / ZOOM_STEP);
		if (newZoom == oldZoom)
			return;

		double cx = (w - scrollX.get()) / oldZoom;
		double cy = (h - scrollY.get()) / oldZoom;
		zoom.set(newZoom);
		scrollX.set(w - cx * newZoom);
		scrollY.set(h - cy * newZoom);
	}

	private static double clampZoom(double v) {
		return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, v));
	}

	// --- properties ---

	public DoubleProperty zoomProperty() {
		return zoom;
	}

	public double getZoom() {
		return zoom.get();
	}

	public void setZoom(double value) {
		zoom.set(clampZoom(value));
	}

	public DoubleProperty scrollXProperty() {
		return scrollX;
	}

	public double getScrollX() {
		return scrollX.get();
	}

	public void setScrollX(double value) {
		scrollX.set(value);
	}

	public DoubleProperty scrollYProperty() {
		return scrollY;
	}

	public double getScrollY() {
		return scrollY.get();
	}

	public void setScrollY(double value) {
		scrollY.set(value);
	}

	public ObjectProperty<GridStyle> gridStyleProperty() {
		return gridStyle;
	}

	public GridStyle getGridStyle() {
		return gridStyle.get();
	}

	public void setGridStyle(GridStyle style) {
		gridStyle.set(style);
	}

	public DoubleProperty gridSpacingProperty() {
		return gridSpacing;
	}

	public double getGridSpacing() {
		return gridSpacing.get();
	}

	public void setGridSpacing(double spacing) {
		gridSpacing.set(spacing);
	}

	public BooleanProperty snapToGridProperty() {
		return snapToGrid;
	}

	public boolean isSnapToGrid() {
		return snapToGrid.get();
	}

	public void setSnapToGrid(boolean snap) {
		snapToGrid.set(snap);
	}

	public double snapX(double x) {
		if (!snapToGrid.get())
			return x;
		double spacing = gridSpacing.get();
		return Math.round(x / spacing) * spacing;
	}

	public double snapY(double y) {
		if (!snapToGrid.get())
			return y;
		double spacing = gridSpacing.get();
		return Math.round(y / spacing) * spacing;
	}
}
