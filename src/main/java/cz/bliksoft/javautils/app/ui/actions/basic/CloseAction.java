package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IClose;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class CloseAction extends BasicContextUIAction<IClose> {

	public CloseAction() {
		super(IClose.class);
	}

	@Override
	protected void execute(IClose current) {
		current.close();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IClose current) {
		return current.getCloseEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IClose current) {
		return current.getCloseIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/close"); //$NON-NLS-1$
	}

	@Override
	public String getKey() {
		return "Close";
	}
}
