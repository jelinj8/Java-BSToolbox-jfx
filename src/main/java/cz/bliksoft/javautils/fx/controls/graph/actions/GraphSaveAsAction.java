package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import javafx.beans.property.BooleanProperty;

public class GraphSaveAsAction extends AbstractGraphAction {

	public GraphSaveAsAction() {
		super("Ctrl+Shift+S");
		setText("Save As...");
	}

	@Override
	protected void execute(IGraphEditor current) {
		current.saveDocumentAs();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return null;
	}

	@Override
	public String getKey() {
		return "GraphSaveAs";
	}
}
