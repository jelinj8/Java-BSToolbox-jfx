package cz.bliksoft.javautils.fx.controls.graph.runtime;

import java.util.UUID;

import cz.bliksoft.dataflow.engine.GraphInstance;
import cz.bliksoft.dataflow.engine.Message;
import cz.bliksoft.dataflow.engine.NodeState;
import cz.bliksoft.dataflow.engine.ProcessingContext;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
					sb.append("[").append(name).append("] payload=").append(msg.getPayload()).append("\n");
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
}
