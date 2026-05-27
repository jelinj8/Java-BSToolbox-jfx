package cz.bliksoft.javautils.app.permissions;

import java.util.HashMap;
import java.util.Map;

public abstract class SessionManager {

	public abstract UserInfo getUserInfo();

	protected Map<String, Object> sessionProperties = new HashMap<>();

	public Object getSessionProperty(String name) {
		return sessionProperties.get(name);
	}
}
