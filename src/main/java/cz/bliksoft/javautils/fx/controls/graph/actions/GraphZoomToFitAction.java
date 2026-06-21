package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import javafx.beans.property.BooleanProperty;

public class GraphZoomToFitAction extends AbstractGraphAction {

	public GraphZoomToFitAction() {
		setText("Zoom to Fit");
	}

	@Override
	protected void execute(IGraphEditor current) {
		current.getCanvas().zoomToFit();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return null;
	}

	@Override
	public String getKey() {
		return "GraphZoomToFit";
	}
}
