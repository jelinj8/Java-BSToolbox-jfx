package cz.bliksoft.javautils.fx.controls.graph.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import cz.bliksoft.dataflow.engine.GraphInstance;
import cz.bliksoft.dataflow.engine.Message;
import cz.bliksoft.dataflow.engine.NodeState;
import cz.bliksoft.dataflow.engine.ProcessingContext;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ContextInspectorPanel extends VBox {

	private final Label titleLabel = new Label("Context Inspector");
	private final TextArea content = new TextArea();

	public ContextInspectorPanel() {
		getStyleClass().add("graph-context-inspector");
		setSpacing(4);
		setPadding(new Insets(8));

		titleLabel.setStyle("-fx-font-weight: bold;");
		content.setEditable(false);
		content.setWrapText(true);
		VBox.setVgrow(content, Priority.ALWAYS);

		getChildren().addAll(titleLabel, content);
		showEmpty();
	}

	public void showEmpty() {
		titleLabel.setText("Context Inspector");
		content.setText("Select a node during execution to inspect its context.");
	}

	public void showFlowContext(GraphInstance instance) {
		if (instance == null) {
			showEmpty();
			return;
		}

		StringBuilder sb = new StringBuilder();

		sb.append("--- Flow ---\n");
		sb.append("Name: ").append(instance.getFlowName()).append("\n");
		sb.append("Run ID: ").append(instance.getRunId()).append("\n");
		sb.append("Started: ").append(instance.getStartedAt()).append("\n");
		Duration elapsed = Duration.between(instance.getStartedAt(), Instant.now());
		sb.append("Elapsed: ").append(formatDuration(elapsed)).append("\n\n");

		sb.append("--- Process Status ---\n");
		Map<UUID, NodeState> states = instance.getNodeStates();
		int pending = 0, running = 0, completed = 0, failed = 0, skipped = 0;
		for (NodeState s : states.values()) {
			switch (s) {
			case PENDING -> pending++;
			case RUNNING -> running++;
			case COMPLETED -> completed++;
			case FAILED -> failed++;
			case SKIPPED -> skipped++;
			}
		}
		sb.append("  Total: ").append(states.size()).append("\n");
		if (completed > 0)
			sb.append("  Completed: ").append(completed).append("\n");
		if (running > 0)
			sb.append("  Running: ").append(running).append("\n");
		if (pending > 0)
			sb.append("  Pending: ").append(pending).append("\n");
		if (failed > 0)
			sb.append("  Failed: ").append(failed).append("\n");
		if (skipped > 0)
			sb.append("  Skipped: ").append(skipped).append("\n");

		if (!instance.getGlobalVariables().isEmpty()) {
			sb.append("\n--- Global Variables ---\n");
			instance.getGlobalVariables()
					.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
		}

		boolean isRunning = running > 0;
		titleLabel.setText(
				isRunning ? "Flow: Running" : (failed > 0 ? "Flow: Completed (with errors)" : "Flow: Completed"));
		content.setText(sb.toString());
	}

	public void showEdgeContext(UUID edgeId, GraphInstance instance) {
		if (instance == null) {
			showEmpty();
			return;
		}

		Message msg = instance.getEdgeMessage(edgeId);
		StringBuilder sb = new StringBuilder();
		sb.append("Edge: ").append(edgeId).append("\n\n");

		if (msg != null) {
			sb.append("--- Message ---\n");
			sb.append("ID: ").append(msg.getId()).append("\n");
			sb.append("Created: ").append(msg.getCreatedAt()).append("\n");
			sb.append("Payload: ").append(msg.getPayload()).append("\n");
			if (!msg.getHeaders().isEmpty()) {
				sb.append("Headers:\n");
				msg.getHeaders().forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
			}
		} else {
			sb.append("No message (edge not yet traversed).\n");
		}

		titleLabel.setText(msg != null ? "Edge: Traversed" : "Edge: Not traversed");
		content.setText(sb.toString());
	}

	public void showNodeContext(UUID nodeId, GraphInstance instance) {
		if (instance == null) {
			showEmpty();
			return;
		}

		NodeState state = instance.getNodeState(nodeId);
		ProcessingContext ctx = instance.getNodeContext(nodeId);

		StringBuilder sb = new StringBuilder();
		sb.append("Node: ").append(nodeId).append("\n");
		sb.append("State: ").append(state).append("\n\n");

		if (ctx != null) {
			Message input = ctx.getInputMessage();
			if (input != null) {
				sb.append("--- Input Message ---\n");
				sb.append("ID: ").append(input.getId()).append("\n");
				sb.append("Created: ").append(input.getCreatedAt()).append("\n");
				sb.append("Payload: ").append(input.getPayload()).append("\n");
				if (!input.getHeaders().isEmpty()) {
					sb.append("Headers:\n");
					input.getHeaders()
							.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
				}
				sb.append("\n");
			}

			if (!ctx.getOutputMessages().isEmpty()) {
				sb.append("--- Output Messages ---\n");
				ctx.getOutputMessages().forEach((name, msg) -> {
					sb.append("[").append(name).append("] id=").append(msg.getId());
					sb.append(" created=").append(msg.getCreatedAt());
					sb.append(" payload=").append(msg.getPayload()).append("\n");
				});
				sb.append("\n");
			}

			if (!ctx.getVariables().isEmpty()) {
				sb.append("--- Variables ---\n");
				ctx.getVariables().forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
				sb.append("\n");
			}

			if (ctx.hasError()) {
				sb.append("--- Error ---\n");
				sb.append(ctx.getError().getClass().getSimpleName()).append(": ").append(ctx.getError().getMessage())
						.append("\n");
			}
		} else {
			sb.append("No context available (node not yet executed).\n");
		}

		if (!instance.getGlobalVariables().isEmpty()) {
			sb.append("\n--- Global Variables ---\n");
			instance.getGlobalVariables()
					.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
		}

		titleLabel.setText("Context: " + state);
		content.setText(sb.toString());
	}

	private static String formatDuration(Duration d) {
		long s = d.getSeconds();
		if (s < 1)
			return d.toMillis() + "ms";
		if (s < 60)
			return s + "s " + (d.toMillisPart()) + "ms";
		return (s / 60) + "m " + (s % 60) + "s";
	}
}
