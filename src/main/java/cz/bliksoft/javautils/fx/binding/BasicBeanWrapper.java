package cz.bliksoft.javautils.fx.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import cz.bliksoft.javautils.fx.tools.FxBinder;
import cz.bliksoft.javautils.fx.tools.FxBinder.BindingHandle;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Abstract wrapper that adapts a plain model object of type {@code T} into an
 * observable bean with lifecycle status tracking.
 *
 * <p>
 * Subclasses implement {@link #bindValues()} to create bidirectional property
 * bindings between the JavaFX properties on this wrapper and the fields of the
 * underlying model object. Once {@link #init()} is called the wrapper starts
 * observing those properties and propagates changes to the status via
 * {@link IStatusBean#touched()}.
 *
 * @param <T> type of the wrapped model object
 */
public abstract class BasicBeanWrapper<T> implements IStatusProvider, IStatusBean {

	/** The wrapped model object. */
	protected T value;

	/**
	 * Creates a wrapper around the given model object.
	 *
	 * @param source the model object to wrap; stored in {@link #value}
	 */
	public BasicBeanWrapper(T source) {
		this.value = source;
	}

	/**
	 * Returns the wrapped model object.
	 *
	 * @return the model object passed to the constructor; may be {@code null}
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Initializes the wrapper: creates property bindings via {@link #bindValues()}
	 * and starts status tracking.
	 */
	protected void init() {
		bindValues();
		setWatched(true);
	}

	/**
	 * The current lifecycle status of this bean; initial value is
	 * {@link ObjectStatus#DETACHED}.
	 */
	protected Property<ObjectStatus> status = new SimpleObjectProperty<>(ObjectStatus.DETACHED);

	@Override
	public Property<ObjectStatus> getStatus() {
		return status;
	}

	private boolean watch = false;

	@Override
	public void setWatched(boolean watch) {
		this.watch = watch;
	}

	@Override
	public boolean isWatched() {
		return watch;
	}

	/** Properties currently watched for status-change notifications. */
	protected List<Property<?>> monitoredProperties = new ArrayList<>();

	/**
	 * Handles for all active property bindings; disposed by {@link #unbindValue()}.
	 */
	protected List<FxBinder.BindingHandle> bindingHandles = new ArrayList<>();

	/**
	 * Change listener that transitions the bean status to
	 * {@link ObjectStatus#MODIFIED} when a property changes.
	 */
	protected ChangeListener<Object> touchListener = new ChangeListener<Object>() {
		@Override
		public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
			if (!watch)
				return;
			touched();
			onChange();
		}
	};

	/**
	 * Binds a JavaFX property bidirectionally to a model getter/setter, then
	 * registers it for status monitoring and disposal on {@link #unbindValue()}.
	 *
	 * @param <Q>    property value type
	 * @param prop   the JavaFX property to bind
	 * @param getter reads the current model value
	 * @param setter writes a changed value back to the model
	 */
	protected <Q> void bindProperty(Property<Q> prop, Supplier<? extends Q> getter, Consumer<? super Q> setter) {
		BindingHandle h = FxBinder.bindProperty(prop, getter, setter);
		bindingHandles.add(h);
		watchProperties(prop);
	}

	/**
	 * Binds a JavaFX property bidirectionally to a model getter/setter using
	 * explicit view↔model converters, then registers it for status monitoring and
	 * disposal.
	 *
	 * @param <V>     view (JavaFX property) value type
	 * @param <M>     model value type
	 * @param prop    the JavaFX property to bind
	 * @param getter  reads the current model value
	 * @param toView  converts a model value to the view type
	 * @param setter  writes a changed value back to the model
	 * @param toModel converts a view value to the model type
	 */
	protected <V, M> void bindProperty(Property<V> prop, Supplier<? extends M> getter,
			Function<? super M, ? extends V> toView, Consumer<? super M> setter,
			Function<? super V, ? extends M> toModel) {
		BindingHandle h = FxBinder.bindProperty(prop, getter, toView, setter, toModel);
		bindingHandles.add(h);
		watchProperties(prop);
	}

	/**
	 * Registers a single property for status-change tracking.
	 *
	 * @param prop the property to watch
	 */
	protected void watchProperty(Property<?> prop) {
		prop.addListener(touchListener);
		monitoredProperties.add(prop);
	}

	/**
	 * Registers one or more properties for status-change tracking.
	 *
	 * @param prop the properties to watch
	 */
	protected void watchProperties(Property<?>... prop) {
		for (Property<?> p : prop) {
			p.addListener(touchListener);
			monitoredProperties.add(p);
		}
	}

	/**
	 * Removes all registered change listeners and clears the monitored-properties
	 * list.
	 */
	protected void unwatchProperties() {
		for (Property<?> p : monitoredProperties) {
			p.removeListener(touchListener);
		}
		monitoredProperties.clear();
	}

	/**
	 * Make property bindings for the wrapped object.
	 */
	protected abstract void bindValues();

	/** Disposes all property bindings and stops status tracking. */
	protected void unbindValue() {
		unwatchProperties();
		for (FxBinder.BindingHandle h : bindingHandles) {
			h.dispose();
		}
		bindingHandles.clear();
	}

	/**
	 * called on each monitored property change
	 */
	protected void onChange() {
		// commit();
	}

	/**
	 * mark object as saved
	 */
	protected void save() {
		status.setValue(ObjectStatus.SAVED);
	}

	// /**
	// * opportunity to check validity before commiting to base object
	// *
	// * @return canContinue (returning false blocks update)
	// */
	// protected boolean beforeCommit() {
	// return true;
	// }
	//
	// protected void commit() {
	// if (!beforeCommit())
	// return;
	// }

}
