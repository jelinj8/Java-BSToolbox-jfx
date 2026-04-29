package cz.bliksoft.javautils.app.ui.actions;

import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ContextChangedEvent;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;

/**
 * Base class for UI actions that are visible and enabled only when a specific
 * context value is <em>absent</em> — the logical inverse of
 * {@link BasicContextUIAction}.
 *
 * <p>
 * Monitors {@link Context#getCurrentContext()} for an object of type {@code I}.
 * While no such object is present the action is visible and enabled. As soon as
 * one is added the action becomes invisible and disabled.
 *
 * <p>
 * Typical use-case: a "Login" action that should only appear when the user is
 * not yet authenticated.
 *
 * <p>
 * Subclasses must supply:
 * <ul>
 * <li>{@link #getKey()} — unique action key for the {@link UIActions}
 * registry</li>
 * <li>{@link #getBaseIconSpec()} — icon spec string (may be {@code null})</li>
 * <li>{@link #execute()} — action body (no context value, since value is
 * absent)</li>
 * </ul>
 *
 * @param <I> the context type whose absence triggers visibility
 */
public abstract class BasicAbsentContextUIAction<I> implements IUIAction {

	private final SimpleBooleanProperty visible = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
	private final SimpleStringProperty iconSpec = new SimpleStringProperty();

	/**
	 * Creates a new action that watches the current context for {@code I} objects.
	 *
	 * @param markerInterface the {@code Class} token for {@code I}; context objects
	 *                        assignable to this type will suppress the action
	 */
	protected BasicAbsentContextUIAction(Class<I> markerInterface) {
		AbstractContextListener<I> listener = new AbstractContextListener<I>(markerInterface,
				getKey() + " absent-context listener") {
			@Override
			public void fired(ContextChangedEvent<I> event) {
				onContextChanged(event.getNewValue());
			}
		};
		Context.getCurrentContext().addContextListener(listener, true);
	}

	private void onContextChanged(I newValue) {
		boolean absent = (newValue == null);
		visible.set(absent);
		enabled.set(absent);
		iconSpec.set(getBaseIconSpec());
	}

	// =========================================================================
	// IUIAction
	// =========================================================================

	@Override
	public final void execute() {
		if (enabled.get()) {
			executeAbsent();
		}
	}

	@Override
	public ObservableBooleanValue visibleProperty() {
		return visible;
	}

	@Override
	public ObservableBooleanValue enabledProperty() {
		return enabled;
	}

	@Override
	public ReadOnlyStringProperty iconSpecProperty() {
		return iconSpec;
	}

	// =========================================================================
	// Abstract / overridable API for subclasses
	// =========================================================================

	/**
	 * Performs the action. Called only when the context value is absent (i.e. the
	 * action is visible and enabled).
	 */
	protected abstract void executeAbsent();

	/**
	 * Returns the base icon spec string for this action (may be {@code null}).
	 *
	 * @return icon spec string, or {@code null}
	 */
	protected abstract String getBaseIconSpec();
}
