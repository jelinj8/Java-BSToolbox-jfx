package cz.bliksoft.javautils.app.events;

import cz.bliksoft.javautils.context.Context;

public class AppClosedEvent {

	private String reason;
	
	private AppClosedEvent(String reason) {
		this.reason = reason;
	}
	
	public String getReason() {
		return reason;
	}
	
	public static void fire(String reason) {
		Context.getRoot().fireGUIEvent(new AppClosedEvent(reason));
	}
}
