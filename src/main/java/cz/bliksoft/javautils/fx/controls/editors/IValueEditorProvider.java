package cz.bliksoft.javautils.fx.controls.editors;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Provides an inline editor node (and optionally a dialog) for a typed value
 * property. Built-in implementations are in the {@code providers} sub-package;
 * create custom implementations for specialised types (e.g. file paths, colour
 * pickers).
 *
 * @param <V> the value type this provider edits
 */
public interface IValueEditorProvider<V> {

	/**
	 * Creates a Node that edits the given property in-place. The node must stay in
	 * sync with the property bidirectionally. Use bidirectional binding where
	 * possible so old editor nodes become GC-eligible.
	 *
	 * @param valueProperty the property to bind the editor to
	 *
	 * @return a JavaFX node that edits the property; never {@code null}
	 */
	Node createEditor(ObjectProperty<V> valueProperty);

	/**
	 * Converts a value to its display string.
	 *
	 * @param value the value to convert; may be {@code null}
	 *
	 * @return display string; never {@code null}
	 */
	String toDisplayString(V value);

	/**
	 * Parses a string back to the value type, or returns {@code null} if not
	 * supported.
	 *
	 * @param s the string to parse; may be {@code null}
	 *
	 * @return the parsed value, or {@code null}
	 */
	V fromString(String s);

	/**
	 * Returns {@code true} if this provider can show a full dialog editor in
	 * addition to the inline node.
	 *
	 * @return {@code true} if {@link #showDialog} is supported
	 */
	default boolean supportsDialog() {
		return false;
	}

	/**
	 * Shows a dialog editor for the given value property. Only called when
	 * {@link #supportsDialog()} returns {@code true}.
	 *
	 * @param owner         the owning window for the dialog
	 * @param valueProperty the property to edit
	 */
	default void showDialog(Window owner, ObjectProperty<V> valueProperty) {
	}

	/**
	 * Stable key used to detect provider type changes without cross-package
	 * instanceof. Override when two instances of the same class represent different
	 * types (e.g. EnumEditorProvider).
	 *
	 * @return a unique string key for this provider type
	 */
	default String providerKey() {
		return getClass().getName();
	}
}
