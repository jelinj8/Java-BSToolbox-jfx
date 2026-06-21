package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import javafx.beans.property.BooleanProperty;

public class GraphGroupAction extends AbstractGraphAction {

	public GraphGroupAction() {
		super("Ctrl+G");
		setText("Group");
	}

	@Override
	protected void execute(IGraphEditor current) {
		current.getCanvas().getGroupHandler().groupSelected();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.hasSelectionProperty();
	}

	@Override
	public String getKey() {
		return "GraphGroup";
	}
}
