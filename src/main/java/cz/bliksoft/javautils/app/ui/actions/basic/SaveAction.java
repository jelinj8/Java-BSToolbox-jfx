package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.ISave;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class SaveAction extends BasicContextUIAction<ISave> {

	public SaveAction() {
		super(ISave.class);
	}

	@Override
	protected void execute(ISave current) {
		current.save();
	}

	@Override
	protected BooleanProperty getEnabledProperty(ISave current) {
		return current.getSaveEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(ISave current) {
		return current.getSaveIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return "/icons/base/SAVE_24.png";
	}

	@Override
	public String getKey() {
		return "Save";
	}
}
