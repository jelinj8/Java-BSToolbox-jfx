package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.ICloseAll;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

public class CloseAllAction extends BasicContextUIAction<ICloseAll> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("CloseAllAction.text")); //$NON-NLS-1$

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
		return IconspecUtils.getIconspec("action/close-all"); //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "CloseAll";
	}
}
