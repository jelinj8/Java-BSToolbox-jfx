package cz.bliksoft.javautils.app.rights;

import java.util.Set;

public abstract class UserInfo {
	public abstract Set<Class<? extends Right>> getCurrentRightSet();
	
	public boolean isAllowed(Class<? extends Right> right) {
		return getCurrentRightSet().contains(right);
	}
}
