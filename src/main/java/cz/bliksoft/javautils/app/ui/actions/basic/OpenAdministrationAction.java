package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.permissions.UserInfo;
import cz.bliksoft.javautils.app.permissions.basic.PermissionOpenAdministration;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.administration.AdministrationPanel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class OpenAdministrationAction extends BasicContextUIAction<UserInfo> {

	public OpenAdministrationAction() {
		super(UserInfo.class);
	}

	@Override
	protected void execute(UserInfo current) {
		if (AdministrationPanel.isOpen())
			return;
		BSAppUI.pushUI(new AdministrationPanel());
	}

	@Override
	protected BooleanProperty getEnabledProperty(UserInfo current) {
		return new SimpleBooleanProperty(current.isAllowed(PermissionOpenAdministration.class));
	}

	@Override
	protected String getBaseIconSpec() {
		return "/icons/base/ADMINISTRATION_24.png";
	}

	@Override
	public String getKey() {
		return "OpenAdministration";
	}
}
