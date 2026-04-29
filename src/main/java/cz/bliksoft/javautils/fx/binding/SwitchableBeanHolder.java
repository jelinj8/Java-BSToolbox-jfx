package cz.bliksoft.javautils.fx.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;

public abstract class SwitchableBeanHolder<M> implements AutoCloseable {

	private final ObjectProperty<M> bean = new SimpleObjectProperty<>(this, "bean");
	private final List<Runnable> unbinders = new ArrayList<>();
	private final List<BindRule<M>> rules = new ArrayList<>();

	protected SwitchableBeanHolder() {
		bean.addListener((obs, oldM, newM) -> rebindAll(newM));
	}

	// ---- current model ----

	public final ObjectProperty<M> beanProperty() {
		return bean;
	}

	public final M getBean() {
		return bean.get();
	}

	public final void setBean(M m) {
		bean.set(m);
	}

	// ---- null/default policies ----

	public enum OnNull {
		/** Set proxy to default value (or null for boxed). */
		SET_DEFAULT,
		/** Leave proxy as-is. */
		KEEP
	}

	// ---- DSL entrypoint ----

	/** Small DSL so subclasses can declare bindings in one-liners. */
	protected final Bind bind() {
		return new Bind();
	}

	protected final class Bind {
		// Boxed / generic proxy
		public <T> ObjectProperty<T> object(String name, Function<M, Property<T>> extractor) {
			return object(name, extractor, OnNull.SET_DEFAULT, () -> null);
		}

		public <T> ObjectProperty<T> object(String name, Function<M, Property<T>> extractor, OnNull onNull,
				Supplier<? extends T> def) {
			var proxy = new SimpleObjectProperty<T>(SwitchableBeanHolder.this, name);
			registerBoxed(proxy, extractor, onNull, def);
			return proxy;
		}

		// String proxy
		public StringProperty string(String name, Function<M, Property<String>> extractor) {
			return string(name, extractor, OnNull.SET_DEFAULT, () -> "");
		}

		public StringProperty string(String name, Function<M, Property<String>> extractor, OnNull onNull,
				Supplier<String> def) {
			var proxy = new SimpleStringProperty(SwitchableBeanHolder.this, name);
			registerBoxed(proxy, extractor, onNull, def);
			return proxy;
		}

		// Primitive proxies (keep primitive types)
		public IntegerProperty integer(String name, Function<M, IntegerProperty> extractor) {
			return integer(name, extractor, OnNull.SET_DEFAULT, 0);
		}

		public IntegerProperty integer(String name, Function<M, IntegerProperty> extractor, OnNull onNull, int def) {
			var proxy = new SimpleIntegerProperty(SwitchableBeanHolder.this, name, def);
			registerPrimitive(proxy, extractor, onNull, def);
			return proxy;
		}

		public LongProperty lng(String name, Function<M, LongProperty> extractor) {
			return lng(name, extractor, OnNull.SET_DEFAULT, 0L);
		}

		public LongProperty lng(String name, Function<M, LongProperty> extractor, OnNull onNull, long def) {
			var proxy = new SimpleLongProperty(SwitchableBeanHolder.this, name, def);
			registerPrimitive(proxy, extractor, onNull, def);
			return proxy;
		}

		public DoubleProperty dbl(String name, Function<M, DoubleProperty> extractor) {
			return dbl(name, extractor, OnNull.SET_DEFAULT, 0d);
		}

		public DoubleProperty dbl(String name, Function<M, DoubleProperty> extractor, OnNull onNull, double def) {
			var proxy = new SimpleDoubleProperty(SwitchableBeanHolder.this, name, def);
			registerPrimitive(proxy, extractor, onNull, def);
			return proxy;
		}

		public FloatProperty flt(String name, Function<M, FloatProperty> extractor) {
			return flt(name, extractor, OnNull.SET_DEFAULT, 0f);
		}

		public FloatProperty flt(String name, Function<M, FloatProperty> extractor, OnNull onNull, float def) {
			var proxy = new SimpleFloatProperty(SwitchableBeanHolder.this, name, def);
			registerPrimitive(proxy, extractor, onNull, def);
			return proxy;
		}

		public BooleanProperty bool(String name, Function<M, BooleanProperty> extractor) {
			return bool(name, extractor, OnNull.SET_DEFAULT, false);
		}

		public BooleanProperty bool(String name, Function<M, BooleanProperty> extractor, OnNull onNull, boolean def) {
			var proxy = new SimpleBooleanProperty(SwitchableBeanHolder.this, name, def);
			registerPrimitive(proxy, extractor, onNull, def);
			return proxy;
		}

		public <E> ObservableList<E> list(String name, Function<M, ObservableList<E>> extractor) {
			return list(name, extractor, OnNull.SET_DEFAULT);
		}

		public <E> ObservableList<E> list(String name, Function<M, ObservableList<E>> extractor, OnNull onNull) {
			return registerListBidirectional(extractor, onNull);
		}

		public <E> ObservableList<E> listOneWay(String name, Function<M, ObservableList<E>> extractor) {
			return listOneWay(name, extractor, OnNull.SET_DEFAULT);
		}

		public <E> ObservableList<E> listOneWay(String name, Function<M, ObservableList<E>> extractor, OnNull onNull) {
			return registerListOneWay(extractor, onNull);
		}

		/**
		 * Returns a FilteredList view over the stable proxy list. You keep a Predicate
		 * property on the returned FilteredList via filtered.predicateProperty().
		 */
		public <E> FilteredList<E> filteredList(String name, Function<M, ObservableList<E>> extractor) {
			ObservableList<E> base = list(name, extractor);
			return new FilteredList<>(base);
		}

		/** Same, but with an initial predicate. */
		public <E> FilteredList<E> filteredList(String name, Function<M, ObservableList<E>> extractor,
				Predicate<? super E> predicate) {
			FilteredList<E> f = filteredList(name, extractor);
			f.setPredicate(predicate);
			return f;
		}

		/**
		 * Sorted view over the stable proxy list. You can bind comparatorProperty() to
		 * a TableView comparator if needed.
		 */
		public <E> SortedList<E> sortedList(String name, Function<M, ObservableList<E>> extractor) {
			ObservableList<E> base = list(name, extractor);
			return new SortedList<>(base);
		}

		public <E> SortedList<E> sortedList(String name, Function<M, ObservableList<E>> extractor,
				Comparator<? super E> comparator) {
			SortedList<E> s = sortedList(name, extractor);
			s.setComparator(comparator);
			return s;
		}

		public <S, T> ObjectBinding<T> map(ObservableValue<? extends S> source,
				Function<? super S, ? extends T> converter) {
			return Bindings.createObjectBinding(() -> converter.apply(source.getValue()), source);
		}

		public <S, T> ObjectBinding<T> map(ObservableValue<? extends S> source,
				Function<? super S, ? extends T> converter, T nullValue) {
			return Bindings.createObjectBinding(() -> {
				S v = source.getValue();
				return v == null ? nullValue : converter.apply(v);
			}, source);
		}

		public ObjectBinding<Node> iconFromString(ObservableValue<String> source,
				Function<? super String, ? extends Node> converter) {
			return map(source, s -> (s == null || s.isBlank()) ? null : converter.apply(s), null);
		}

		public ObjectBinding<Node> icon(ObservableValue<String> source,
				Function<? super String, ? extends Node> converter) {
			return Bindings.createObjectBinding(() -> converter.apply(source.getValue()), source);
		}
	}

	// ---- registration API (protected so subclasses can bind boxed manually via
	// proxy.asObject()) ----

	protected final <T> void registerBoxed(Property<T> proxy, Function<M, Property<T>> extractor, OnNull onNull,
			Supplier<? extends T> defaultValue) {
		Objects.requireNonNull(proxy, "proxy");
		Objects.requireNonNull(extractor, "extractor");
		Objects.requireNonNull(onNull, "onNull");
		Objects.requireNonNull(defaultValue, "defaultValue");

		rules.add(new BoxedRule<>(proxy, extractor, onNull, defaultValue));
		// if current is already set, apply immediately
		rebindOne(rules.get(rules.size() - 1), getBean());
	}

	// Generic primitive registration to reduce duplication:
	// Works for IntegerProperty, LongProperty, DoubleProperty, FloatProperty,
	// BooleanProperty
	protected final <P extends PropertyLike<P>> void registerPrimitive(P proxy, Function<M, P> extractor, OnNull onNull,
			Runnable setDefault) {
		Objects.requireNonNull(proxy, "proxy");
		Objects.requireNonNull(extractor, "extractor");
		Objects.requireNonNull(onNull, "onNull");
		Objects.requireNonNull(setDefault, "setDefault");

		rules.add(new PrimitiveRule<>(proxy, extractor, onNull, setDefault));
		rebindOne(rules.get(rules.size() - 1), getBean());
	}

	protected final <E> void registerList(ObservableList<E> proxy, Function<M, ObservableList<E>> extractor,
			OnNull onNull) {
		registerList(proxy, extractor, onNull, true);
	}

	protected final <E> void registerList(ObservableList<E> proxy, Function<M, ObservableList<E>> extractor,
			OnNull onNull, boolean biDirectional) {
		Objects.requireNonNull(proxy, "proxy");
		Objects.requireNonNull(extractor, "extractor");
		Objects.requireNonNull(onNull, "onNull");

		rules.add(new ListRule<M, E>(proxy, extractor, onNull, biDirectional));

		// If your holder supports "bind after current already set", call bind for
		// current here if you do that elsewhere.
		// Otherwise ignore (it will bind on next setCurrent()).
	}

	protected final <E> ObservableList<E> registerListBidirectional(Function<M, ObservableList<E>> extractor,
			OnNull onNull) {
		ObservableList<E> proxy = FXCollections.observableArrayList();
		rules.add(new ListRule<>(proxy, extractor, onNull, true));
		return proxy;
	}

	protected final <E> ObservableList<E> registerListOneWay(Function<M, ObservableList<E>> extractor, OnNull onNull) {
		ObservableList<E> proxy = FXCollections.observableArrayList();
		rules.add(new ListRule<>(proxy, extractor, onNull, false));
		return proxy;
	}

	// ---- rebinding ----

	private void rebindAll(M newM) {
		unbinders.forEach(Runnable::run);
		unbinders.clear();

		boundModelProps.clear();
		boundModelLists.clear();

		for (BindRule<M> r : rules) {
			r.bind(this, newM, unbinders);
		}
	}

	private void rebindOne(BindRule<M> rule, M model) {
		// don't unbind existing rules; just bind this new rule
		// (useful when rules are registered after current is set)
		rule.bind(this, model, unbinders);
	}

	// ---- rules ----

	private interface BindRule<M> {
		void bind(SwitchableBeanHolder<M> holder, M model, List<Runnable> unbinders);
	}

	private static final class BoxedRule<M, T> implements BindRule<M> {
		private final Property<T> proxy;
		private final Function<M, Property<T>> extractor;
		private final OnNull onNull;
		private final Supplier<? extends T> def;

		BoxedRule(Property<T> proxy, Function<M, Property<T>> extractor, OnNull onNull, Supplier<? extends T> def) {
			this.proxy = proxy;
			this.extractor = extractor;
			this.onNull = onNull;
			this.def = def;
		}

		@Override
		public void bind(SwitchableBeanHolder<M> holder, M model, List<Runnable> unbinders) {
			if (model == null) {
				if (onNull == OnNull.SET_DEFAULT)
					proxy.setValue(def.get());
				return;
			}
			Property<T> modelProp = extractor.apply(model);
			if (modelProp == null) {
				if (onNull == OnNull.SET_DEFAULT)
					proxy.setValue(def.get());
				return;
			}

			holder.boundModelProps.add(modelProp);

			proxy.setValue(modelProp.getValue());
			proxy.bindBidirectional(modelProp);
			unbinders.add(() -> proxy.unbindBidirectional(modelProp));
		}
	}

	/**
	 * A tiny abstraction so we can have ONE primitive rule instead of 5
	 * near-duplicates. Implemented by adapters for each primitive property type
	 * below.
	 *
	 * @param <P> the self-type of the property adapter
	 */
	protected interface PropertyLike<P> {
		void bindBidirectional(P other);

		void unbindBidirectional(P other);

		void copyFrom(P other);

		Property<?> boxedProperty();
	}

	private static final class PrimitiveRule<M, P extends PropertyLike<P>> implements BindRule<M> {
		private final P proxy;
		private final Function<M, P> extractor;
		private final OnNull onNull;
		private final Runnable setDefault;

		PrimitiveRule(P proxy, Function<M, P> extractor, OnNull onNull, Runnable setDefault) {
			this.proxy = proxy;
			this.extractor = extractor;
			this.onNull = onNull;
			this.setDefault = setDefault;
		}

		@Override
		public void bind(SwitchableBeanHolder<M> holder, M model, List<Runnable> unbinders) {
			if (model == null) {
				if (onNull == OnNull.SET_DEFAULT)
					setDefault.run();
				return;
			}
			P modelProp = extractor.apply(model);
			if (modelProp == null) {
				if (onNull == OnNull.SET_DEFAULT)
					setDefault.run();
				return;
			}

			holder.boundModelProps.add(modelProp.boxedProperty());

			proxy.copyFrom(modelProp);
			proxy.bindBidirectional(modelProp);
			unbinders.add(() -> proxy.unbindBidirectional(modelProp));
		}
	}

	private static final class ListRule<M, E> implements BindRule<M> {
		private final ObservableList<E> proxy;
		private final Function<M, ObservableList<E>> extractor;
		private final OnNull onNull;
		private final boolean bidirectional;

		ListRule(ObservableList<E> proxy, Function<M, ObservableList<E>> extractor, OnNull onNull,
				boolean bidirectional) {
			this.proxy = proxy;
			this.extractor = extractor;
			this.onNull = onNull;
			this.bidirectional = bidirectional;
		}

		@Override
		public void bind(SwitchableBeanHolder<M> holder, M model, List<Runnable> unbinders) {
			if (model == null) {
				if (onNull == OnNull.SET_DEFAULT)
					proxy.clear();
				return;
			}

			ObservableList<E> modelList = extractor.apply(model);
			if (modelList == null) {
				if (onNull == OnNull.SET_DEFAULT)
					proxy.clear();
				return;
			}

			// Track model list for snapshots/reset
			holder.registerBoundModelList(modelList);

			// Initial sync
			proxy.setAll(modelList);

			if (bidirectional) {
				Bindings.bindContentBidirectional(proxy, modelList);
				unbinders.add(() -> Bindings.unbindContentBidirectional(proxy, modelList));
			} else {
				Bindings.bindContent(proxy, modelList);
				unbinders.add(() -> Bindings.unbindContent(proxy, modelList));
			}
		}
	}

	// ---- adapters for JavaFX primitive properties ----

	protected static final class IntLike implements PropertyLike<IntLike> {
		final IntegerProperty p;

		public IntLike(IntegerProperty p) {
			this.p = p;
		}

		@Override
		public void bindBidirectional(IntLike other) {
			p.bindBidirectional(other.p);
		}

		@Override
		public void unbindBidirectional(IntLike other) {
			p.unbindBidirectional(other.p);
		}

		@Override
		public void copyFrom(IntLike other) {
			p.set(other.p.get());
		}

		@Override
		public Property<?> boxedProperty() {
			return p.asObject();
		}
	}

	protected static final class LongLike implements PropertyLike<LongLike> {
		final LongProperty p;

		public LongLike(LongProperty p) {
			this.p = p;
		}

		@Override
		public void bindBidirectional(LongLike other) {
			p.bindBidirectional(other.p);
		}

		@Override
		public void unbindBidirectional(LongLike other) {
			p.unbindBidirectional(other.p);
		}

		@Override
		public void copyFrom(LongLike other) {
			p.set(other.p.get());
		}

		@Override
		public Property<?> boxedProperty() {
			return p.asObject();
		}
	}

	protected static final class DoubleLike implements PropertyLike<DoubleLike> {
		final DoubleProperty p;

		public DoubleLike(DoubleProperty p) {
			this.p = p;
		}

		@Override
		public void bindBidirectional(DoubleLike other) {
			p.bindBidirectional(other.p);
		}

		@Override
		public void unbindBidirectional(DoubleLike other) {
			p.unbindBidirectional(other.p);
		}

		@Override
		public void copyFrom(DoubleLike other) {
			p.set(other.p.get());
		}

		@Override
		public Property<?> boxedProperty() {
			return p.asObject();
		}
	}

	protected static final class FloatLike implements PropertyLike<FloatLike> {
		final FloatProperty p;

		public FloatLike(FloatProperty p) {
			this.p = p;
		}

		@Override
		public void bindBidirectional(FloatLike other) {
			p.bindBidirectional(other.p);
		}

		@Override
		public void unbindBidirectional(FloatLike other) {
			p.unbindBidirectional(other.p);
		}

		@Override
		public void copyFrom(FloatLike other) {
			p.set(other.p.get());
		}

		@Override
		public Property<?> boxedProperty() {
			return p.asObject();
		}
	}

	protected static final class BoolLike implements PropertyLike<BoolLike> {
		final BooleanProperty p;

		public BoolLike(BooleanProperty p) {
			this.p = p;
		}

		@Override
		public void bindBidirectional(BoolLike other) {
			p.bindBidirectional(other.p);
		}

		@Override
		public void unbindBidirectional(BoolLike other) {
			p.unbindBidirectional(other.p);
		}

		@Override
		public void copyFrom(BoolLike other) {
			p.set(other.p.get());
		}

		@Override
		public Property<?> boxedProperty() {
			return p.asObject();
		}
	}

	// Convenience overloads so you can keep primitive proxies WITHOUT boxing in the
	// base class:
	protected final void registerPrimitive(IntegerProperty proxy, Function<M, IntegerProperty> extractor, OnNull onNull,
			int def) {
		registerPrimitive(new IntLike(proxy), m -> new IntLike(extractor.apply(m)), onNull, () -> proxy.set(def));
	}

	protected final void registerPrimitive(LongProperty proxy, Function<M, LongProperty> extractor, OnNull onNull,
			long def) {
		registerPrimitive(new LongLike(proxy), m -> new LongLike(extractor.apply(m)), onNull, () -> proxy.set(def));
	}

	protected final void registerPrimitive(DoubleProperty proxy, Function<M, DoubleProperty> extractor, OnNull onNull,
			double def) {
		registerPrimitive(new DoubleLike(proxy), m -> new DoubleLike(extractor.apply(m)), onNull, () -> proxy.set(def));
	}

	protected final void registerPrimitive(FloatProperty proxy, Function<M, FloatProperty> extractor, OnNull onNull,
			float def) {
		registerPrimitive(new FloatLike(proxy), m -> new FloatLike(extractor.apply(m)), onNull, () -> proxy.set(def));
	}

	protected final void registerPrimitive(BooleanProperty proxy, Function<M, BooleanProperty> extractor, OnNull onNull,
			boolean def) {
		registerPrimitive(new BoolLike(proxy), m -> new BoolLike(extractor.apply(m)), onNull, () -> proxy.set(def));
	}

	@Override
	public void close() {
		unbinders.forEach(Runnable::run);
		unbinders.clear();

		boundModelProps.clear();
		boundModelLists.clear();

		editSnapshot.clear();
		baselineSnapshot.clear();

		editListSnapshot.clear();
		baselineListSnapshot.clear();

		bean.set(null);
	}

	// ==================================== snapshot support

	/** Model properties currently bound (for current model). */
	private final Set<Property<?>> boundModelProps = Collections.newSetFromMap(new IdentityHashMap<>());

	/** Baseline snapshot (DB/original). */
	private final Map<Property<?>, Object> baselineSnapshot = new IdentityHashMap<>();

	/** Edit snapshot (Cancel). */
	private final Map<Property<?>, Object> editSnapshot = new IdentityHashMap<>();

	/**
	 * Call when you start editing (e.g. after current changes or when dialog
	 * opens).
	 */
	public final void snapshotEdit() {
		editSnapshot.clear();
		for (Property<?> p : boundModelProps) {
			editSnapshot.put(p, p.getValue());
		}

		editListSnapshot.clear();
		for (ObservableList<?> l : boundModelLists) {
			editListSnapshot.put(l, shallowCopy((ObservableList<?>) l));
		}
	}

	/** Capture baseline (DB/original) for current model properties if missing. */
	public final void snapshotBaselineIfAbsent() {
		for (Property<?> p : boundModelProps) {
			baselineSnapshot.putIfAbsent(p, p.getValue());
		}

		for (ObservableList<?> l : boundModelLists) {
			baselineListSnapshot.putIfAbsent(l, shallowCopy((ObservableList<?>) l));
		}
	}

	/** Overwrite baseline with current values (typically after Save). */
	public final void acceptEditsAsBaseline() {
		for (Property<?> p : boundModelProps) {
			baselineSnapshot.put(p, p.getValue());
		}

		for (ObservableList<?> l : boundModelLists) {
			baselineListSnapshot.put(l, shallowCopy((ObservableList<?>) l));
		}
		// After save, cancel snapshot typically becomes same as baseline:
		snapshotEdit();
	}

	/** Cancel edits: restore values captured by snapshotEdit(). */
	public final void resetToEdit() {
		restoreFrom(editSnapshot);
		restoreListsFrom(editListSnapshot);
	}

	/**
	 * Reset to DB/original: restore values captured by snapshotBaselineIfAbsent() /
	 * acceptEditsAsBaseline().
	 */
	public final void resetToBaseline() {
		restoreFrom(baselineSnapshot);
		restoreListsFrom(baselineListSnapshot);
	}

	private void restoreFrom(Map<Property<?>, Object> snap) {
		for (Property<?> p : boundModelProps) {
			if (!snap.containsKey(p))
				continue;
			@SuppressWarnings("unchecked")
			Property<Object> po = (Property<Object>) p;
			po.setValue(snap.get(p));
		}
	}

	@SuppressWarnings("unchecked")
	private void restoreListsFrom(Map<ObservableList<?>, List<?>> snap) {
		for (ObservableList<?> l : boundModelLists) {
			List<?> s = snap.get(l);
			if (s == null)
				continue;
			restoreListFrom((ObservableList<Object>) l, s);
		}
	}

	/**
	 * Optional: clear baseline for current model (if you want to recapture from
	 * DB).
	 */
	public final void clearBaselineForCurrent() {
		for (Property<?> p : boundModelProps) {
			baselineSnapshot.remove(p);
		}

		for (ObservableList<?> l : boundModelLists)
			baselineListSnapshot.remove(l);
	}

	/**
	 * Model lists currently bound for the current bean instance (identity-based).
	 */
	private final Set<ObservableList<?>> boundModelLists = Collections.newSetFromMap(new IdentityHashMap<>());

	/**
	 * Baseline snapshot for lists ("DB/original"), keyed by model list identity.
	 */
	private final Map<ObservableList<?>, List<?>> baselineListSnapshot = new IdentityHashMap<>();

	/** Edit snapshot for lists ("cancel"), keyed by model list identity. */
	private final Map<ObservableList<?>, List<?>> editListSnapshot = new IdentityHashMap<>();

	private void registerBoundModelList(ObservableList<?> modelList) {
		boundModelLists.add(modelList);
	}

	private static <E> List<E> shallowCopy(ObservableList<E> list) {
		return new ArrayList<>(list);
	}

	@SuppressWarnings("unchecked")
	private static <E> void restoreListFrom(ObservableList<E> target, List<?> snapshot) {
		target.setAll((List<? extends E>) snapshot);
	}
}
