package cz.bliksoft.javautils.app.permissions;

public class DefaultUnrestrictedSessionManager extends SessionManager {

	UserInfo ui = new DefaultAllmightyUserInfo();

	@Override
	public UserInfo getUserInfo() {
		return ui;
	}

	@Override
	public String toString() {
		return "DefaultUnrestrictedSessionManager";
	}
}
