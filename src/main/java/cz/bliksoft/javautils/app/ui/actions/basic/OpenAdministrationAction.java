package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.permissions.Permissions;
import cz.bliksoft.javautils.app.permissions.basic.PermissionOpenAdministration;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.administration.AdministrationPanel;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.property.SimpleBooleanProperty;

public class OpenAdministrationAction implements IUIAction {

	private final SimpleBooleanProperty visible = new SimpleBooleanProperty(
			Permissions.isAllowed(PermissionOpenAdministration.class));
	private final ReadOnlyStringProperty iconSpec = new SimpleStringProperty("/icons/base/ADMINISTRATION_24.png");

	@Override
	public void execute() {
		BSAppUI.pushUI(new AdministrationPanel());
	}

	@Override
	public ObservableBooleanValue visibleProperty() {
		return visible;
	}

	@Override
	public ObservableBooleanValue enabledProperty() {
		return visible;
	}

	@Override
	public ReadOnlyStringProperty iconSpecProperty() {
		return iconSpec;
	}

	@Override
	public String getKey() {
		return "OpenAdministration";
	}
}
