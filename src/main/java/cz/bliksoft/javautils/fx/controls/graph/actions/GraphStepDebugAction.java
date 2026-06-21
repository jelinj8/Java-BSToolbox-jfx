package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.dataflow.engine.Message;
import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;

public class GraphStepDebugAction extends AbstractGraphAction {

	public GraphStepDebugAction() {
		super("F6");
		setText("Step Debug");
	}

	@Override
	protected void execute(IGraphEditor current) {
		GraphRuntimeBridge bridge = current.getRuntimeBridge();
		if (bridge != null && !bridge.isRunning())
			bridge.executeStepByStep(new Message("start"));
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.notRunningProperty();
	}

	@Override
	public String getKey() {
		return "GraphStepDebug";
	}
}
