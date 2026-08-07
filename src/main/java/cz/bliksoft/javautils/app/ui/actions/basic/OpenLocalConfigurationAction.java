package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.app.ui.interfaces.IConfigurable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;

public class OpenLocalConfigurationAction extends BasicContextUIAction<IConfigurable> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("OpenLocalConfigurationAction.text")); //$NON-NLS-1$

	public OpenLocalConfigurationAction() {
		super(IConfigurable.class);
	}

	@Override
	protected void execute(IConfigurable current) {
		current.configure();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IConfigurable current) {
		return new SimpleBooleanProperty(current.isConfigurable());
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/local-settings"); //$NON-NLS-1$
	}

	@Override
	protected String getBaseMenuIconSpec() {
		return IconspecUtils.getMenuIconspec("action/local-settings"); //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "OpenLocalConfiguration";
	}
}
