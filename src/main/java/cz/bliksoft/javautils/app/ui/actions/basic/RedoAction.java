package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IRedo;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class RedoAction extends BasicContextUIAction<IRedo> {

	public RedoAction() {
		super(IRedo.class);
	}

	@Override
	protected void execute(IRedo current) {
		current.redo();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IRedo current) {
		return current.getRedoEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IRedo current) {
		return current.getRedoIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/redo"); //$NON-NLS-1$
	}

	@Override
	public String getKey() {
		return "Redo";
	}
}
