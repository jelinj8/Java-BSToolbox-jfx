package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import javafx.beans.property.BooleanProperty;

public class GraphZoom100Action extends AbstractGraphAction {

	public GraphZoom100Action() {
		setText("Zoom 100%");
	}

	@Override
	protected void execute(IGraphEditor current) {
		current.getCanvas().resetZoom();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return null;
	}

	@Override
	public String getKey() {
		return "GraphZoom100";
	}
}
