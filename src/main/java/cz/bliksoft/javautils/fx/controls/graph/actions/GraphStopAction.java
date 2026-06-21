package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;

public class GraphStopAction extends AbstractGraphAction {

	public GraphStopAction() {
		super("Shift+F5");
		setText("Stop");
	}

	@Override
	protected void execute(IGraphEditor current) {
		GraphRuntimeBridge bridge = current.getRuntimeBridge();
		if (bridge != null)
			bridge.stop();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.runningProperty();
	}

	@Override
	public String getKey() {
		return "GraphStop";
	}
}
