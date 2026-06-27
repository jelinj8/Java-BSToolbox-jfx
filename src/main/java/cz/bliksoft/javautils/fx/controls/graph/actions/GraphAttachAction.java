package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import cz.bliksoft.javautils.fx.controls.graph.runtime.AttachDialog;
import javafx.beans.property.BooleanProperty;
import javafx.stage.Window;

/**
 * Opens the {@link AttachDialog} to pick a running managed graph and attach the
 * editor to it for read-only monitoring/inspection.
 */
public class GraphAttachAction extends AbstractGraphAction {

	public GraphAttachAction() {
		super();
		setText("Attach");
	}

	@Override
	protected void execute(IGraphEditor current) {
		Window owner = current.getCanvas().getScene() != null ? current.getCanvas().getScene().getWindow() : null;
		AttachDialog.show(owner).ifPresent(selection -> current.getMonitorController().attach(selection));
	}

	@Override
	protected BooleanProperty getEnabledProperty(IGraphEditor current) {
		return null; // always enabled while an editor is in context
	}

	@Override
	public String getKey() {
		return "GraphAttach";
	}
}
