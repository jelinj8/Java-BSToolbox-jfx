package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IDelete;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class DeleteAction extends BasicContextUIAction<IDelete> {

	public DeleteAction() {
		super(IDelete.class);
	}

	@Override
	protected void execute(IDelete current) {
		current.delete();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IDelete current) {
		return current.getDeleteEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IDelete current) {
		return current.getDeleteIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return "/icons/base/REMOVE_16.png";
	}

	@Override
	public String getKey() {
		return "Delete";
	}
}
