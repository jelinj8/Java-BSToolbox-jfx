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

public abstract class BasicBeanWrapper<T> implements IStatusProvider, IStatusBean {

	protected T value;

	public BasicBeanWrapper(T source) {
		this.value = source;
	}

	protected void init() {
		bindValues();
		setWatched(true);
	}

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

	protected List<Property<?>> monitoredProperties = new ArrayList<>();

	protected List<FxBinder.BindingHandle> bindingHandles = new ArrayList<>();

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
	 * initialize and bind a property, register for status monitoring and cleanup on
	 * unbind
	 * 
	 * @param <Q>
	 * @param prop
	 * @param getter
	 * @param setter
	 */
	protected <Q> void bindProperty(Property<Q> prop, Supplier<? extends Q> getter, Consumer<? super Q> setter) {
		BindingHandle h = FxBinder.bindProperty(prop, getter, setter);
		bindingHandles.add(h);
		watchProperties(prop);
	}

	/**
	 * initialize and bind a property, register for status monitoring and cleanup,
	 * use converter method
	 * 
	 * @param <V>
	 * @param <M>
	 * @param prop
	 * @param getter
	 * @param toView
	 * @param setter
	 * @param toModel
	 */
	protected <V, M> void bindProperty(Property<V> prop, Supplier<? extends M> getter,
			Function<? super M, ? extends V> toView, Consumer<? super M> setter,
			Function<? super V, ? extends M> toModel) {
		BindingHandle h = FxBinder.bindProperty(prop, getter, toView, setter, toModel);
		bindingHandles.add(h);
		watchProperties(prop);
	}

	protected void watchProperty(Property<?> prop) {
		prop.addListener(touchListener);
		monitoredProperties.add(prop);
	}

	protected void watchProperties(Property<?>... prop) {
		for (Property<?> p : prop) {
			p.addListener(touchListener);
			monitoredProperties.add(p);
		}
	}

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
//		commit();
	}

	/**
	 * mark object as saved
	 */
	protected void save() {
		status.setValue(ObjectStatus.SAVED);
	}

//	/**
//	 * opportunity to check validity before commiting to base object
//	 * 
//	 * @return canContinue (returning false blocks update)
//	 */
//	protected boolean beforeCommit() {
//		return true;
//	}
//
//	protected void commit() {
//		if (!beforeCommit())
//			return;
//	}

}
