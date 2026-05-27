package cz.bliksoft.javautils.app.permissions;

import java.util.Set;

public class DefaultAllmightyUserInfo extends UserInfo {

	@Override
	public Set<Class<? extends Permission>> getCurrentPermissionSet() {
		return Permissions.getFullSet();
	}

}
