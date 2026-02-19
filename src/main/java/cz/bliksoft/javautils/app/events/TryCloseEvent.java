package cz.bliksoft.javautils.app.events;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.context.Context;

public class TryCloseEvent {

	static Logger log = LogManager.getLogger();

	private String closeReason;
	private ArrayList<String> blockedReasons = new ArrayList<>();

	private boolean blocked = false;

	private TryCloseEvent(String reason) {
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

	public static void fire(String reason) {
		TryCloseEvent evt = new TryCloseEvent(reason);
		Context.getRoot().fireGUIEvent(evt);
		if (evt.isBlocked()) {
			log.info("Closing blocked:\n" + evt.getBlockingReasons().stream().collect(Collectors.joining("\n")));
		} else {
			AppClosedEvent.fire(reason);
		}
	}

}
