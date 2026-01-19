package cz.bliksoft.javautils.fx.tools;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;

public final class FxBinder {

	private FxBinder() {
	}

	public static <T> BindingHandle bindProperty( //
			Property<T> prop, //
			Supplier<? extends T> getter //
	) {
		return bindProperty(prop, getter, Function.identity(), null, null);
	}

	public static <V, M> BindingHandle bindProperty( //
			Property<V> prop, //
			Supplier<? extends M> getter, //
			Function<? super M, ? extends V> toView //
	) {
		return bindProperty(prop, getter, toView, null, null);
	}

	public static <T> BindingHandle bindProperty( //
			Property<T> prop, //
			Supplier<? extends T> getter, //
			Consumer<? super T> setter //
	) {
		return bindProperty(prop, getter, Function.identity(), setter, setter == null ? null : Function.identity());
	}

	public static <V, M> BindingHandle bindProperty( //
			Property<V> prop, // property of the view
			Supplier<? extends M> getter, // getter for the model
			Function<? super M, ? extends V> toView, // model -> view (for init / refresh)
			Consumer<? super M> setter, // setter of the model
			Function<? super V, ? extends M> toModel // view -> model
	) {
		Objects.requireNonNull(prop, "prop");
		Objects.requireNonNull(getter, "getter");
		Objects.requireNonNull(toView, "toView");

		if (setter != null)
			Objects.requireNonNull(toModel, "toModel");


		prop.setValue(toView.apply(getter.get()));

		if (setter != null) {
			ChangeListener<V> listener = (obs, oldV, newV) -> setter.accept(toModel.apply(newV));
			prop.addListener(listener);
			return () -> prop.removeListener(listener);
		} else {
			return () -> {
			};
		}
	}

	@FunctionalInterface
	public interface BindingHandle {
		void dispose();
	}
}
