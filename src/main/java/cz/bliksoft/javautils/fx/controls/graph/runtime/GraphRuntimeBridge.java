package cz.bliksoft.javautils.fx.controls.graph.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cz.bliksoft.dataflow.engine.GraphExecutor;
import cz.bliksoft.dataflow.engine.GraphExecutorListener;
import cz.bliksoft.dataflow.engine.GraphInstance;
import cz.bliksoft.dataflow.engine.Message;
import cz.bliksoft.dataflow.engine.NodeState;
import cz.bliksoft.dataflow.engine.SteppingController;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class GraphRuntimeBridge {

	private final GraphCanvas canvas;
	private final GraphExecutor executor;
	private final NodeStateOverlay stateOverlay;
	private final BreakpointManager breakpointManager;

	private final BooleanProperty paused = new SimpleBooleanProperty(false);
	private final BooleanProperty running = new SimpleBooleanProperty(false);

	private GraphInstance lastInstance;

	private enum EdgeState {
		ACTIVE, TRAVERSED, SKIPPED
	}

	private final Map<UUID, EdgeState> edgeStates = new HashMap<>();

	public GraphRuntimeBridge(GraphCanvas canvas, GraphExecutor executor) {
		this.canvas = canvas;
		this.executor = executor;
		this.stateOverlay = new NodeStateOverlay(canvas);
		this.breakpointManager = new BreakpointManager();

		canvas.addPostRefreshCallback(() -> {
			stateOverlay.reapplyAll();
			reapplyEdgeStates();
			updateBreakpointVisuals();
		});

		executor.addListener(new GraphExecutorListener() {
			@Override
			public void onExecutionStarted(GraphInstance instance) {
				lastInstance = instance;
			}

			@Override
			public void onNodeStarted(UUID nodeId) {
				stateOverlay.updateNodeState(nodeId, NodeState.RUNNING);
				checkPaused();
				notifyInspector();
			}

			@Override
			public void onNodeCompleted(UUID nodeId) {
				stateOverlay.updateNodeState(nodeId, NodeState.COMPLETED);
				notifyInspector();
			}

			@Override
			public void onNodeFailed(UUID nodeId, Throwable error) {
				stateOverlay.updateNodeState(nodeId, NodeState.FAILED);
				notifyInspector();
			}

			@Override
			public void onNodeSkipped(UUID nodeId) {
				stateOverlay.updateNodeState(nodeId, NodeState.SKIPPED);
			}

			@Override
			public void onEdgeTraversed(UUID edgeId, Message message) {
				highlightEdge(edgeId, true);
			}

			@Override
			public void onEdgeSkipped(UUID edgeId) {
				highlightEdge(edgeId, false);
			}

			@Override
			public void onExecutionCompleted(GraphInstance instance) {
				Platform.runLater(() -> {
					running.set(false);
					paused.set(false);
					notifyInspector();
				});
			}
		});
	}

	public void execute(Message initialMessage) {
		startExecution(initialMessage, SteppingController.Mode.RUN);
	}

	public void executeStepByStep(Message initialMessage) {
		startExecution(initialMessage, SteppingController.Mode.STEP);
	}

	private void startExecution(Message initialMessage, SteppingController.Mode mode) {
		Group graph = canvas.getGraph();
		if (graph == null)
			return;

		stateOverlay.clear();
		clearEdgeRuntimeStyles();
		updateBreakpointVisuals();
		SteppingController sc = executor.getSteppingController();
		sc.reset();
		sc.setMode(mode);
		sc.setBreakpoints(breakpointManager.getBreakpoints());
		running.set(true);
		paused.set(false);

		Thread executionThread = new Thread(() -> {
			executor.execute(graph, initialMessage);
		}, "GraphExecutor");
		executionThread.setDaemon(true);
		executionThread.start();
	}

	public void stepOver() {
		var sel = canvas.getSelectionModel().getSelection();
		SteppingController sc = executor.getSteppingController();
		if (sel.size() == 1 && sc.getPausedNodes().contains(sel.iterator().next())) {
			sc.stepOver(sel.iterator().next());
		} else {
			sc.stepAll();
		}
	}

	public void stepAll() {
		executor.getSteppingController().stepAll();
	}

	public void resume() {
		executor.getSteppingController().resume();
		Platform.runLater(() -> paused.set(false));
	}

	public void resumeToBreakpoints() {
		executor.getSteppingController().setBreakpoints(breakpointManager.getBreakpoints());
		executor.getSteppingController().resumeToBreakpoints();
		Platform.runLater(() -> paused.set(false));
	}

	public void pause() {
		executor.getSteppingController().setMode(SteppingController.Mode.STEP);
	}

	public void stop() {
		executor.getSteppingController().stop();
		Platform.runLater(() -> {
			running.set(false);
			paused.set(false);
			stateOverlay.clear();
			clearEdgeRuntimeStyles();
		});
	}

	public BooleanProperty runningProperty() {
		return running;
	}

	public boolean isRunning() {
		return running.get();
	}

	public BooleanProperty pausedProperty() {
		return paused;
	}

	public boolean isPaused() {
		return paused.get();
	}

	public GraphInstance getLastInstance() {
		return lastInstance;
	}

	public void toggleBreakpoint(UUID nodeId) {
		breakpointManager.toggleBreakpoint(nodeId);
		updateBreakpointVisual(nodeId);
	}

	public void updateBreakpointVisuals() {
		if (canvas.getGraph() == null)
			return;
		for (var node : canvas.getGraph().getNodes())
			updateBreakpointVisual(node.getId());
	}

	private void updateBreakpointVisual(UUID nodeId) {
		javafx.scene.layout.Region visual = canvas.getNodeVisual(nodeId);
		if (visual == null)
			return;
		if (breakpointManager.hasBreakpoint(nodeId)) {
			if (!visual.getStyleClass().contains("graph-node-breakpoint"))
				visual.getStyleClass().add("graph-node-breakpoint");
			addBreakpointDot(visual, nodeId);
		} else {
			visual.getStyleClass().remove("graph-node-breakpoint");
			removeBreakpointDot(visual);
		}
	}

	private void addBreakpointDot(javafx.scene.layout.Region visual, UUID nodeId) {
		if (!(visual instanceof javafx.scene.layout.Pane pane))
			return;
		for (javafx.scene.Node child : pane.getChildren()) {
			if (child.getProperties().containsKey("breakpointDot"))
				return;
		}
		javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
		dot.setFill(javafx.scene.paint.Color.web("#F44336"));
		dot.setStroke(javafx.scene.paint.Color.WHITE);
		dot.setStrokeWidth(1);
		dot.setLayoutX(5);
		dot.setLayoutY(5);
		dot.setMouseTransparent(true);
		dot.getProperties().put("breakpointDot", true);
		pane.getChildren().add(dot);
	}

	private void removeBreakpointDot(javafx.scene.layout.Region visual) {
		if (!(visual instanceof javafx.scene.layout.Pane pane))
			return;
		pane.getChildren().removeIf(child -> child.getProperties().containsKey("breakpointDot"));
	}

	public NodeStateOverlay getStateOverlay() {
		return stateOverlay;
	}

	public BreakpointManager getBreakpointManager() {
		return breakpointManager;
	}

	public GraphExecutor getExecutor() {
		return executor;
	}

	private void checkPaused() {
		SteppingController sc = executor.getSteppingController();
		javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
		delay.setOnFinished(e -> {
			paused.set(sc.isPaused());
			if (sc.isPaused()) {
				var sel = canvas.getSelectionModel().getSelection();
				boolean selectionIsPaused = sel.size() == 1 && sc.getPausedNodes().contains(sel.iterator().next());
				if (!selectionIsPaused) {
					UUID first = sc.getFirstPausedNode();
					if (first != null) {
						canvas.getSelectionModel().select(first);
						canvas.updateSelectionVisuals();
					}
				}
				if (onPauseInspector != null)
					onPauseInspector.run();
			}
		});
		Platform.runLater(delay::play);
	}

	private Runnable onPauseInspector;

	public void setOnPauseInspector(Runnable inspector) {
		this.onPauseInspector = inspector;
	}

	private void notifyInspector() {
		if (onPauseInspector != null)
			Platform.runLater(onPauseInspector);
	}

	private void clearEdgeRuntimeStyles() {
		edgeStates.clear();
		if (canvas.getGraph() == null)
			return;
		for (var edge : canvas.getGraph().getAllEdgesRecursive()) {
			javafx.scene.Group edgeVisual = canvas.getEdgeVisual(edge.getId());
			if (edgeVisual != null) {
				edgeVisual.setEffect(null);
				edgeVisual.setOpacity(1.0);
			}
		}
	}

	private void highlightEdge(UUID edgeId, boolean traversed) {
		Platform.runLater(() -> {
			if (!traversed) {
				edgeStates.put(edgeId, EdgeState.SKIPPED);
				applyEdgeEffect(edgeId, EdgeState.SKIPPED);
				return;
			}

			edgeStates.put(edgeId, EdgeState.ACTIVE);
			applyEdgeEffect(edgeId, EdgeState.ACTIVE);

			javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(
					javafx.util.Duration.millis(400));
			pt.setOnFinished(e -> {
				edgeStates.put(edgeId, EdgeState.TRAVERSED);
				applyEdgeEffect(edgeId, EdgeState.TRAVERSED);
			});
			pt.play();
		});
	}

	private void applyEdgeEffect(UUID edgeId, EdgeState state) {
		javafx.scene.Group edgeVisual = canvas.getEdgeVisual(edgeId);
		if (edgeVisual == null)
			return;
		switch (state) {
		case ACTIVE -> {
			edgeVisual.setEffect(new javafx.scene.effect.DropShadow(10, javafx.scene.paint.Color.web("#FFC107")));
			edgeVisual.setOpacity(1.0);
		}
		case TRAVERSED -> {
			edgeVisual.setEffect(new javafx.scene.effect.DropShadow(6, javafx.scene.paint.Color.web("#4CAF50")));
			edgeVisual.setOpacity(1.0);
		}
		case SKIPPED -> {
			edgeVisual.setEffect(null);
			edgeVisual.setOpacity(0.25);
		}
		}
	}

	private void reapplyEdgeStates() {
		for (var entry : edgeStates.entrySet())
			applyEdgeEffect(entry.getKey(), entry.getValue());
	}
}
