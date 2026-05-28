package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.interfaces.IConfigurable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class OpenLocalConfigurationAction extends BasicContextUIAction<IConfigurable> {

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
		return "24/CONFIGURE.png";
	}

	@Override
	public String getKey() {
		return "OpenLocalConfiguration";
	}
}
