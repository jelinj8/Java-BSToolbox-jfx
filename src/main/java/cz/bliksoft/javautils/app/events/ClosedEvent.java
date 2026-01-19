package cz.bliksoft.javautils.app.events;

import cz.bliksoft.javautils.context.Context;

public class ClosedEvent {

	private String reason;
	
	private ClosedEvent(String reason) {
		this.reason = reason;
	}
	
	public String getReason() {
		return reason;
	}
	
	public static void fire(String reason) {
		Context.getGlobal().fireGUIEvent(new ClosedEvent(reason));
	}
}
