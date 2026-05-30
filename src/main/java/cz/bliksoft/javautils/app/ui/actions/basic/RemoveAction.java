package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IRemove;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class RemoveAction extends BasicContextUIAction<IRemove> {

	public RemoveAction() {
		super(IRemove.class);
	}

	@Override
	protected void execute(IRemove current) {
		current.remove();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IRemove current) {
		return current.getRemoveEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IRemove current) {
		return current.getRemoveIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/remove"); //$NON-NLS-1$
	}

	@Override
	protected String getBaseMenuIconSpec() {
		return IconspecUtils.getMenuIconspec("action/remove"); //$NON-NLS-1$
	}

	@Override
	public String getKey() {
		return "Remove";
	}
}
