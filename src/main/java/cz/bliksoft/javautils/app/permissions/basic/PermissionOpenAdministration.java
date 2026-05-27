package cz.bliksoft.javautils.app.permissions.basic;

import cz.bliksoft.javautils.app.permissions.Permission;
import cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages;

public class PermissionOpenAdministration extends Permission {

	@Override
	public String getName() {
		return BSAppAdministrationMessages.getString("PermissionOpenAdministration.name");
	}

	@Override
	public String getAlias() {
		return "OpenAdministration";
	}
}
