package cz.bliksoft.javautils.app.events;

import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.javautils.context.Context;

public class ClosingEvent {

	private String closeReason;
	private ArrayList<String> blockedReasons = new ArrayList<>();

	private boolean blocked = false;

	private ClosingEvent(String reason) {
		closeReason = reason;
	}

	public void blockClosing(String reason) {
		blocked = true;
		blockedReasons.add(reason);
	}

	public boolean isBlocked() {
		return blocked;
	}

	public List<String> getBlockingReasons() {
		return blockedReasons;
	}

	public String getReason() {
		return closeReason;
	}

	public static ClosingEvent fire(String reason) {
		ClosingEvent evt = new ClosingEvent(reason);
		Context.getGlobal().fireGUIEvent(evt);
		return evt;
	}

}
