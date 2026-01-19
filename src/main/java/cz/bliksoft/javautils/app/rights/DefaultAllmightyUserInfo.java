package cz.bliksoft.javautils.app.rights;

import java.util.Set;

public class DefaultAllmightyUserInfo extends UserInfo {

	@Override
	public Set<Class<? extends Right>> getCurrentRightSet() {
		return Rights.getFullSet();
	}

}
