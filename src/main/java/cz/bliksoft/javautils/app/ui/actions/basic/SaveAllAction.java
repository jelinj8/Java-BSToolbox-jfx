package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.ISaveAll;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class SaveAllAction extends BasicContextUIAction<ISaveAll> {

	public SaveAllAction() {
		super(ISaveAll.class);
	}

	@Override
	protected void execute(ISaveAll current) {
		current.saveAll();
	}

	@Override
	protected BooleanProperty getEnabledProperty(ISaveAll current) {
		return current.getSaveAllEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(ISaveAll current) {
		return current.getSaveAllIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return "/icons/base/SAVE_ALL_24.png";
	}

	@Override
	public String getKey() {
		return "SaveAll";
	}
}
