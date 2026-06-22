package cz.bliksoft.javautils.fx.controls.graph.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cz.bliksoft.dataflow.engine.NodeState;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class NodeStateOverlay {

	private static final double BADGE_RADIUS = 6;

	private static final Map<NodeState, Color> STATE_COLORS = Map.of(NodeState.PENDING, Color.web("#BDBDBD"),
			NodeState.RUNNING, Color.web("#FFC107"), NodeState.COMPLETED, Color.web("#4CAF50"), NodeState.FAILED,
			Color.web("#F44336"), NodeState.SKIPPED, Color.web("#9E9E9E"));

	private final GraphCanvas canvas;
	private final Map<UUID, NodeState> states = new HashMap<>();

	public NodeStateOverlay(GraphCanvas canvas) {
		this.canvas = canvas;
	}

	public void updateNodeState(UUID nodeId, NodeState state) {
		Platform.runLater(() -> {
			Region visual = canvas.getNodeVisual(nodeId);
			if (visual == null || !(visual instanceof javafx.scene.layout.Pane pane))
				return;

			Circle existing = findBadgeIn(pane);
			if (existing != null) {
				existing.setFill(STATE_COLORS.getOrDefault(state, Color.GRAY));
			} else {
				Circle badge = new Circle(BADGE_RADIUS);
				badge.setStroke(Color.WHITE);
				badge.setStrokeWidth(1.5);
				badge.setMouseTransparent(true);
				badge.getStyleClass().add("graph-runtime-badge");
				badge.setFill(STATE_COLORS.getOrDefault(state, Color.GRAY));
				badge.setLayoutX(pane.getPrefWidth() - BADGE_RADIUS);
				badge.setLayoutY(BADGE_RADIUS);
				badge.getProperties().put("runtimeBadge", true);
				pane.getChildren().add(badge);
			}

			states.put(nodeId, state);

			if (state == NodeState.RUNNING) {
				visual.setStyle("-fx-effect: dropshadow(gaussian, #FFC107, 10, 0.3, 0, 0);");
			} else if (state == NodeState.COMPLETED) {
				visual.setStyle("-fx-effect: dropshadow(gaussian, #4CAF50, 6, 0.2, 0, 0);");
			} else if (state == NodeState.FAILED) {
				visual.setStyle("-fx-effect: dropshadow(gaussian, #F44336, 10, 0.3, 0, 0);");
			} else {
				visual.setStyle("");
			}
		});
	}

	public void reapplyAll() {
		Platform.runLater(() -> {
			for (var entry : states.entrySet())
				updateNodeState(entry.getKey(), entry.getValue());
		});
	}

	private Circle findBadgeIn(javafx.scene.layout.Pane pane) {
		for (javafx.scene.Node child : pane.getChildren()) {
			if (child instanceof Circle c && c.getProperties().containsKey("runtimeBadge"))
				return c;
		}
		return null;
	}

	public void clear() {
		Platform.runLater(() -> {
			states.clear();

			if (canvas.getGraph() != null) {
				for (var node : canvas.getGraph().getAllNodesRecursive()) {
					Region visual = canvas.getNodeVisual(node.getId());
					if (visual instanceof javafx.scene.layout.Pane pane) {
						Circle badge = findBadgeIn(pane);
						if (badge != null)
							pane.getChildren().remove(badge);
					}
					if (visual != null)
						visual.setStyle("");
				}
			}
		});
	}
}
