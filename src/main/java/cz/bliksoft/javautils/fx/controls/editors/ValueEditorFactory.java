package cz.bliksoft.javautils.fx.controls.editors;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import cz.bliksoft.javautils.fx.controls.editors.providers.BooleanEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.providers.DoubleEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.providers.EnumEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.providers.IntegerEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.providers.LocalDateEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.providers.LocalDateTimeEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.providers.StringBridgingEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.providers.StringEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.providers.TimestampEditorProvider;

/**
 * Resolves the appropriate built-in {@link IValueEditorProvider} for a given
 * type.
 */
public final class ValueEditorFactory {

	private ValueEditorFactory() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <V> IValueEditorProvider<V> forType(Class<V> type) {
		if (type == null || type == String.class || type == Object.class)
			return (IValueEditorProvider<V>) new StringEditorProvider();
		if (type == Integer.class || type == int.class)
			return (IValueEditorProvider<V>) new IntegerEditorProvider();
		if (type == Double.class || type == double.class)
			return (IValueEditorProvider<V>) new DoubleEditorProvider();
		if (type == Boolean.class || type == boolean.class)
			return (IValueEditorProvider<V>) new BooleanEditorProvider();
		if (type == LocalDate.class)
			return (IValueEditorProvider<V>) new LocalDateEditorProvider();
		if (type == LocalDateTime.class)
			return (IValueEditorProvider<V>) new LocalDateTimeEditorProvider();
		if (type == Timestamp.class)
			return (IValueEditorProvider<V>) new TimestampEditorProvider();
		if (type.isEnum())
			return (IValueEditorProvider<V>) new EnumEditorProvider(type);
		return (IValueEditorProvider<V>) new StringEditorProvider();
	}

	public static IValueEditorProvider<String> stringProvider() {
		return new StringEditorProvider();
	}

	/**
	 * Resolves a provider that edits a {@link String}-backed value (as stored in a
	 * {@code Map<String,String>}) using the inline editor/dialog appropriate for
	 * {@code type}. For non-{@code String} types (e.g. {@code Integer.class},
	 * {@code Boolean.class}), the corresponding typed provider is wrapped with
	 * {@link StringBridgingEditorProvider} so it can be used where the underlying
	 * value is actually a {@link String}.
	 */
	public static IValueEditorProvider<String> forStringType(Class<?> type) {
		if (type == null || type == String.class || type == Object.class)
			return new StringEditorProvider();
		return bridge(forType(type));
	}

	private static <T> IValueEditorProvider<String> bridge(IValueEditorProvider<T> inner) {
		return new StringBridgingEditorProvider<>(inner);
	}
}
