package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IReload;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class ReloadAction extends BasicContextUIAction<IReload> {

	public ReloadAction() {
		super(IReload.class);
	}

	@Override
	protected void execute(IReload current) {
		current.reload();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IReload current) {
		return current.getReloadEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IReload current) {
		return current.getReloadIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return "24/REFRESH.png";
	}

	@Override
	public String getKey() {
		return "Reload";
	}
}
