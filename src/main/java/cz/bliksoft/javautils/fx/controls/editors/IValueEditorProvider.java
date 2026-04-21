package cz.bliksoft.javautils.fx.controls.editors;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Provides an inline editor node (and optionally a dialog) for a typed value property.
 * Built-in implementations are in the {@code providers} sub-package; create custom
 * implementations for specialised types (e.g. file paths, colour pickers).
 */
public interface IValueEditorProvider<V> {

    /**
     * Creates a Node that edits the given property in-place.
     * The node must stay in sync with the property bidirectionally.
     * Use bidirectional binding where possible so old editor nodes become GC-eligible.
     */
    Node createEditor(ObjectProperty<V> valueProperty);

    String toDisplayString(V value);

    /** Returns null if String conversion is not supported. */
    V fromString(String s);

    default boolean supportsDialog() {
        return false;
    }

    default void showDialog(Window owner, ObjectProperty<V> valueProperty) {
    }

    /**
     * Stable key used to detect provider type changes without cross-package instanceof.
     * Override when two instances of the same class represent different types (e.g. EnumEditorProvider).
     */
    default String providerKey() {
        return getClass().getName();
    }
}
