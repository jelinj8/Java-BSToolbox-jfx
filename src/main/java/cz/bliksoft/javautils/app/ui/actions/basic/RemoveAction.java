package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IRemove;
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
		return "16/REMOVE.png";
	}

	@Override
	public String getKey() {
		return "Remove";
	}
}
