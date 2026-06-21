package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;

public class GraphPauseAction extends AbstractGraphAction {

	public GraphPauseAction() {
		setText("Pause");
	}

	@Override
	protected void execute(IGraphEditor current) {
		GraphRuntimeBridge bridge = current.getRuntimeBridge();
		if (bridge != null && bridge.isRunning())
			bridge.pause();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.runningProperty();
	}

	@Override
	public String getKey() {
		return "GraphPause";
	}
}
