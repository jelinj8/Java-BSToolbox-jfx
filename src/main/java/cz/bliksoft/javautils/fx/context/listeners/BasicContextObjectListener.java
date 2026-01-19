package cz.bliksoft.javautils.fx.context.listeners;

import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.ContextChangedEvent;
import cz.bliksoft.javautils.fx.context.IContextPropertyProvider;

/**
 * listener pro kontextové objekty přejímající obsah z kontextu
 * 
 * @author hroch
 * 
 */
public class BasicContextObjectListener<T> extends AbstractContextListener<T> {

	IContextPropertyProvider<T> owner;
	AbstractContextListener<T> additionalListener = null;

	public BasicContextObjectListener(Class<?> key, IContextPropertyProvider<T> owner, String comment) {
		this(key, owner, comment, null);
	}

	public BasicContextObjectListener(Class<?> key, IContextPropertyProvider<T> owner, String comment,
			AbstractContextListener<T> additionalContextListener) {
		super(key, "ContextObject updater (" + owner.getClass().getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		this.owner = owner;
		this.comment = comment;
		this.additionalListener = additionalContextListener;
	}

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