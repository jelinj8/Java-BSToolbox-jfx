package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IClose;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

public class CloseAction extends BasicContextUIAction<IClose> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("CloseAction.text")); //$NON-NLS-1$

	public CloseAction() {
		super(IClose.class);
	}

	@Override
	protected void execute(IClose current) {
		current.close();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IClose current) {
		return current.getCloseEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IClose current) {
		return current.getCloseIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/close"); //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "Close";
	}
}
