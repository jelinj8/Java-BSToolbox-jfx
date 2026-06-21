package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IOpen;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class OpenAction extends BasicContextUIAction<IOpen> {

	public OpenAction() {
		super(IOpen.class);
	}

	@Override
	protected void execute(IOpen current) {
		current.open();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IOpen current) {
		return current.getOpenEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IOpen current) {
		return current.getOpenIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/open"); //$NON-NLS-1$
	}

	@Override
	public String getKey() {
		return "Open";
	}
}
