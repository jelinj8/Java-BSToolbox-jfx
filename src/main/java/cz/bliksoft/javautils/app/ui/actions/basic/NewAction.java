package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.INew;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class NewAction extends BasicContextUIAction<INew> {

	public NewAction() {
		super(INew.class);
	}

	@Override
	protected void execute(INew current) {
		current.newDocument();
	}

	@Override
	protected BooleanProperty getEnabledProperty(INew current) {
		return current.getNewEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(INew current) {
		return current.getNewIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/new"); //$NON-NLS-1$
	}

	@Override
	public String getKey() {
		return "New";
	}
}
