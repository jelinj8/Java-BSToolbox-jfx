package cz.bliksoft.javautils.app.events;

import cz.bliksoft.javautils.context.Context;

/**
 * Event fired on the root context when the application has finished closing.
 * Listeners registered on the root context receive this event after all modules
 * have been given a chance to veto via {@code TryCloseEvent}.
 */
public class AppClosedEvent {

	private String reason;

	private AppClosedEvent(String reason) {
		this.reason = reason;
	}

	/**
	 * Returns a human-readable description of why the application closed.
	 *
	 * @return the close reason; never {@code null}
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Fires an {@code AppClosedEvent} on the root context.
	 *
	 * @param reason human-readable reason for the close
	 */
	public static void fire(String reason) {
		Context.getRoot().fireGUIEvent(new AppClosedEvent(reason));
	}
}
