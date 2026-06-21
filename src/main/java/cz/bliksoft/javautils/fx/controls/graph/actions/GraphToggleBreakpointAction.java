package cz.bliksoft.javautils.fx.controls.graph.actions;

import java.util.Set;
import java.util.UUID;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;

public class GraphToggleBreakpointAction extends AbstractGraphAction {

	public GraphToggleBreakpointAction() {
		super("Ctrl+F8");
		setText("Toggle Breakpoint");
	}

	@Override
	protected void execute(IGraphEditor current) {
		GraphRuntimeBridge bridge = current.getRuntimeBridge();
		if (bridge == null)
			return;
		Set<UUID> sel = current.getCanvas().getSelectionModel().getSelection();
		if (sel.size() == 1)
			bridge.toggleBreakpoint(sel.iterator().next());
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return current.singleSelectionProperty();
	}

	@Override
	public String getKey() {
		return "GraphToggleBP";
	}
}
