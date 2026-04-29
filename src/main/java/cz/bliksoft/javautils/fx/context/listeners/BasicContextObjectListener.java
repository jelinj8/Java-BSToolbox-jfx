package cz.bliksoft.javautils.fx.context.listeners;

import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.ContextChangedEvent;
import cz.bliksoft.javautils.fx.context.IContextPropertyProvider;

/**
 * Context listener that forwards context change events to an
 * {@link IContextPropertyProvider}, updating its context property with the new
 * value. An optional secondary listener can be chained to receive the same
 * event.
 *
 * @param <T> the type of context object being tracked
 */
public class BasicContextObjectListener<T> extends AbstractContextListener<T> {

	IContextPropertyProvider<T> owner;
	AbstractContextListener<T> additionalListener = null;

	/**
	 * Creates a listener without an additional chained listener.
	 *
	 * @param key     the context key class to listen for
	 * @param owner   the property provider whose context property will be updated
	 * @param comment optional description used in log messages
	 */
	public BasicContextObjectListener(Class<?> key, IContextPropertyProvider<T> owner, String comment) {
		this(key, owner, comment, null);
	}

	/**
	 * Creates a listener with an optional additional chained listener.
	 *
	 * @param key                       the context key class to listen for
	 * @param owner                     the property provider whose context property
	 *                                  will be updated
	 * @param comment                   optional description used in log messages
	 * @param additionalContextListener secondary listener to notify, or
	 *                                  {@code null}
	 */
	public BasicContextObjectListener(Class<?> key, IContextPropertyProvider<T> owner, String comment,
			AbstractContextListener<T> additionalContextListener) {
		super(key, "ContextObject updater (" + owner.getClass().getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		this.owner = owner;
		this.comment = comment;
		this.additionalListener = additionalContextListener;
	}

	/**
	 * Creates a listener keyed on a specific {@link ContextChangedEvent} subtype.
	 *
	 * @param key   the event class to listen for
	 * @param owner the property provider whose context property will be updated
	 */
	public BasicContextObjectListener(Class<? extends ContextChangedEvent<T>> key, IContextPropertyProvider<T> owner) {
		this(key, owner, null);
	}

	@Override
	public void fired(ContextChangedEvent<T> event) {
		owner.getContextProperty().setValue((T) event.getNewValue());
		if (additionalListener != null)
			additionalListener.fired(event);
	}

}