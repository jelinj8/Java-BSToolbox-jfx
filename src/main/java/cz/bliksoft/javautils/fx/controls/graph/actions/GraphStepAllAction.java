package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;

public class GraphStepAllAction extends AbstractGraphAction {

	public GraphStepAllAction() {
		super("F7");
		setText("Step All");
	}

	@Override
	protected void execute(IGraphEditor current) {
		GraphRuntimeBridge bridge = current.getRuntimeBridge();
		if (bridge != null && bridge.isPaused())
			bridge.stepAll();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.pausedProperty();
	}

	@Override
	public String getKey() {
		return "GraphStepAll";
	}
}
