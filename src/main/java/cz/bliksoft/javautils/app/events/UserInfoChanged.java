package cz.bliksoft.javautils.app.events;

import cz.bliksoft.javautils.app.rights.UserInfo;
import cz.bliksoft.javautils.context.Context;

public class UserInfoChanged {

	UserInfo userInfo;

	public UserInfoChanged(UserInfo user) {
		this.userInfo = user;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public static void fire(UserInfo userInfo) {
		Context.getRoot().fireGUIEvent(new UserInfoChanged(userInfo));
	}
}
