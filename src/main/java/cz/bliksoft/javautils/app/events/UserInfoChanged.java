package cz.bliksoft.javautils.app.events;

import cz.bliksoft.javautils.app.rights.UserInfo;
import cz.bliksoft.javautils.context.Context;

public class UserInfoChanged {

	public static void fire(UserInfo oldUserInfo, UserInfo newUserInfo) {
		Context.getGlobal().fireGUIEvent(new UserInfoChanged());
	}
}
