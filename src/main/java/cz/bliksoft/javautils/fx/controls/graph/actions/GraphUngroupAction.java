package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import javafx.beans.property.BooleanProperty;

public class GraphUngroupAction extends AbstractGraphAction {

	public GraphUngroupAction() {
		super("Ctrl+Shift+G");
		setText("Ungroup");
	}

	@Override
	protected void execute(IGraphEditor current) {
		current.getCanvas().getGroupHandler().ungroupSelected();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.hasSelectionProperty();
	}

	@Override
	public String getKey() {
		return "GraphUngroup";
	}
}
