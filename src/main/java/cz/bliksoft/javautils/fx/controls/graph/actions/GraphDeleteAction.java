package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import cz.bliksoft.javautils.fx.controls.graph.command.DeleteElementsCommand;
import javafx.beans.property.BooleanProperty;

public class GraphDeleteAction extends AbstractGraphAction {

	public GraphDeleteAction() {
		super("Delete");
		setText("Delete");
	}

	@Override
	protected void execute(IGraphEditor current) {
		GraphCanvas canvas = current.getCanvas();
		if (canvas.getSelectionModel().isEmpty())
			return;
		DeleteElementsCommand cmd = new DeleteElementsCommand(canvas.getGraph(),
				canvas.getSelectionModel().getSelection());
		canvas.getCommandHistory().execute(cmd);
		canvas.getSelectionModel().clear();
		canvas.refreshGraph();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.hasSelectionProperty();
	}

	@Override
	public String getKey() {
		return "GraphDelete";
	}
}
