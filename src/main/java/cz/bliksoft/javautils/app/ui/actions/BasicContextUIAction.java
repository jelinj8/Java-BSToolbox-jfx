package cz.bliksoft.javautils.app.ui.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ContextChangedEvent;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;

/**
 * Base class for context-aware singleton UI actions.
 *
 * <p>
 * Monitors {@link Context#getCurrentContext()} for an object implementing the
 * marker interface {@code I}. When such an object is present:
 * <ul>
 * <li>{@link #visibleProperty()} becomes {@code true}</li>
 * <li>{@link #enabledProperty()} mirrors the property returned by
 * {@link #getEnabledProperty(Object)}</li>
 * <li>{@link #iconSpecProperty()} combines the action's own base icon (see
 * {@link #getBaseIconSpec()}) with any overlay returned by
 * {@link #getIconOverlay(Object)}, separated by {@code #}</li>
 * </ul>
 *
 * <p>
 * When no matching object is in context the action is invisible and disabled.
 *
 * <p>
 * Subclasses must supply:
 * <ul>
 * <li>{@link #getKey()} — unique action key for the {@link UIActions}
 * registry</li>
 * <li>{@link #getBaseIconSpec()} — icon spec string (may be {@code null})</li>
 * <li>{@link #getEnabledProperty(Object)} — extracts the enabled property from
 * the context value</li>
 * <li>{@link #execute(Object)} — action body, receives the current context
 * value</li>
 * </ul>
 *
 * @param <I> marker interface that must be implemented by context objects
 *            triggering this action
 */
public abstract class BasicContextUIAction<I> implements IUIAction {

	private static final Logger log = LogManager.getLogger(BasicContextUIAction.class);

	private final SimpleBooleanProperty visible = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
	private final SimpleStringProperty iconSpec = new SimpleStringProperty();

	private I currentValue = null;
	private ChangeListener<String> iconChangeListener = null;

	/**
	 * Creates a new action that watches the current context for {@code I} objects.
	 *
	 * @param markerInterface the {@code Class} token for {@code I}; used as the
	 *                        context listener key — objects assignable to this type
	 *                        will trigger the action
	 */
	protected BasicContextUIAction(Class<I> markerInterface) {
		AbstractContextListener<I> contextListener = new AbstractContextListener<I>(markerInterface,
				getKey() + " context listener") {
			@Override
			public void fired(ContextChangedEvent<I> event) {
				onContextChanged(event.getNewValue());
			}
		};
		Context.getCurrentContext().addContextListener(contextListener, true);
	}

	// =========================================================================
	// Context tracking
	// =========================================================================

	private void onContextChanged(I newValue) {
		// Detach icon overlay listener from old value
		if (currentValue != null && iconChangeListener != null) {
			StringProperty oldIcon = getIconOverlay(currentValue);
			if (oldIcon != null)
				oldIcon.removeListener(iconChangeListener);
			iconChangeListener = null;
		}

		I oldValue = currentValue;
		currentValue = newValue;
		visible.set(newValue != null);

		if (newValue != null) {
			BooleanProperty ep = getEnabledProperty(newValue);
			if (ep != null) {
				enabled.bind(ep);
			} else {
				enabled.unbind();
				enabled.set(true);
				log.warn(
						"Action '{}': getEnabledProperty() returned null for context value {} — action will remain permanently enabled.",
						getKey(), newValue.getClass().getSimpleName());
			}
			updateIconSpec(newValue);
			StringProperty iconProp = getIconOverlay(newValue);
			if (iconProp != null) {
				iconChangeListener = (obs, o, n) -> updateIconSpec(currentValue);
				iconProp.addListener(iconChangeListener);
			}
		} else {
			enabled.unbind();
			enabled.set(false);
			iconSpec.set(getBaseIconSpec());
		}

		onValueChanged(oldValue, newValue);
	}

	/**
	 * Called after the context value changes. Override to react to value
	 * transitions (e.g. mirror an observable sub-list). The base implementation
	 * does nothing.
	 *
	 * <p>
	 * <strong>Important:</strong> this method may be called from the
	 * {@link BasicContextUIAction} constructor (during context listener
	 * initialization), before subclass field initializers have run. Subclasses that
	 * override this method and access their own fields must:
	 * <ol>
	 * <li>Guard against {@code null} fields at the top of the override.</li>
	 * <li>Call {@link #refreshContext()} as the last statement of their own
	 * constructor to replay the initial value once all fields are ready.</li>
	 * </ol>
	 *
	 * @param oldValue previous context value, may be {@code null}
	 * @param newValue new context value, may be {@code null}
	 */
	protected void onValueChanged(I oldValue, I newValue) {
	}

	/**
	 * Re-fires {@link #onValueChanged} with the currently tracked context value.
	 *
	 * <p>
	 * Call this as the <em>last</em> statement of any subclass constructor that
	 * overrides {@link #onValueChanged} and accesses subclass-owned fields, so that
	 * the initial context value is applied after those fields are fully
	 * initialized.
	 */
	protected final void refreshContext() {
		onValueChanged(null, currentValue);
	}

	private void updateIconSpec(I value) {
		String base = getBaseIconSpec();
		StringProperty iconProp = getIconOverlay(value);
		String overlay = (iconProp != null) ? iconProp.get() : null;
		if (base != null && overlay != null && !overlay.isBlank()) {
			iconSpec.set(base + "#" + overlay);
		} else {
			iconSpec.set(base);
		}
	}

	// =========================================================================
	// IUIAction
	// =========================================================================

	@Override
	public final void execute() {
		if (currentValue != null && enabled.get()) {
			execute(currentValue);
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
	 * Performs the action on the currently active context value.
	 *
	 * @param current the object from context that triggered visibility; never
	 *                {@code null} and always enabled when called
	 */
	protected abstract void execute(I current);

	/**
	 * Returns the base icon spec string for this action (may be {@code null}). If
	 * {@link #getIconOverlay(Object)} also returns a property with a value, the two
	 * specs are joined as {@code baseSpec#overlaySpec}.
	 *
	 * @return icon spec string, or {@code null}
	 */
	protected abstract String getBaseIconSpec();

	/**
	 * Extracts the enabled {@link BooleanProperty} from the given context value.
	 * Return {@code null} to leave the action permanently enabled while the context
	 * value is present.
	 *
	 * @param current the current context value; never {@code null}
	 *
	 * @return the enabled property, or {@code null} for unconditionally enabled
	 */
	protected abstract BooleanProperty getEnabledProperty(I current);

	/**
	 * Returns the optional icon-overlay {@link StringProperty} from the context
	 * value, or {@code null} if no overlay is provided. The default returns
	 * {@code null}.
	 *
	 * @param current the current context value; never {@code null}
	 *
	 * @return the overlay property, or {@code null}
	 */
	protected StringProperty getIconOverlay(I current) {
		return null;
	}
}
