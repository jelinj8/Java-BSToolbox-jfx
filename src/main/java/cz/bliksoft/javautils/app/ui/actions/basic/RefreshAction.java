package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IRefresh;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class RefreshAction extends BasicContextUIAction<IRefresh> {

	public RefreshAction() {
		super(IRefresh.class);
	}

	@Override
	protected void execute(IRefresh current) {
		current.refresh();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IRefresh current) {
		return current.getRefreshEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IRefresh current) {
		return current.getRefreshIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return "24/REFRESH.png";
	}

	@Override
	public String getKey() {
		return "Refresh";
	}
}
