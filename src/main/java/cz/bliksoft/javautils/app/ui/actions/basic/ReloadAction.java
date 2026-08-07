package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IReload;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

public class ReloadAction extends BasicContextUIAction<IReload> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("ReloadAction.text")); //$NON-NLS-1$

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
		return IconspecUtils.getIconspec("action/reload"); //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "Reload";
	}
}
