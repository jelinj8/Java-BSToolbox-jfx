package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;

public class GraphRunToEndAction extends AbstractGraphAction {

	public GraphRunToEndAction() {
		setText("Run to End");
	}

	@Override
	protected void execute(IGraphEditor current) {
		GraphRuntimeBridge bridge = current.getRuntimeBridge();
		if (bridge != null && bridge.isPaused())
			bridge.resume();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.pausedProperty();
	}

	@Override
	public String getKey() {
		return "GraphRunToEnd";
	}
}
