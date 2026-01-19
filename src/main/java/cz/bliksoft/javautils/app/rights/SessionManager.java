package cz.bliksoft.javautils.app.rights;

import java.util.HashMap;
import java.util.Map;

/**
 * base class to implement as session manager. It has to provide at least
 * UserInfo used to determine current user right set.
 * 
 * @author jakub
 *
 */
public abstract class SessionManager {

	public abstract UserInfo getUserInfo();

	protected Map<String, Object> sessionProperties = new HashMap<>();

	public Object getSessionProperty(String name) {
		return sessionProperties.get(name);
	}
}
