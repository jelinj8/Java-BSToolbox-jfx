package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.ICloseAll;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class CloseAllAction extends BasicContextUIAction<ICloseAll> {

	public CloseAllAction() {
		super(ICloseAll.class);
	}

	@Override
	protected void execute(ICloseAll current) {
		current.closeAll();
	}

	@Override
	protected BooleanProperty getEnabledProperty(ICloseAll current) {
		return current.getCloseAllEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(ICloseAll current) {
		return current.getCloseAllIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return "24/CLOSE.png";
	}

	@Override
	public String getKey() {
		return "CloseAll";
	}
}
