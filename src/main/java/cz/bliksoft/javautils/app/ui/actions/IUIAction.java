package cz.bliksoft.javautils.app.ui.actions;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCombination;

public interface IUIAction {
	/**
	 * Runs the action. Called by the framework when the bound control is activated.
	 */
	void execute();

	/**
	 * Returns a property controlling whether the bound control is enabled.
	 *
	 * @return enabled property, or {@code null} if the action is always enabled
	 */
	default ObservableBooleanValue enabledProperty() {
		return null;
	}

	/**
	 * Returns a property controlling visibility of the bound control. When
	 * {@code false}, the control is hidden and removed from layout.
	 *
	 * @return visible property, or {@code null} if the action is always visible
	 */
	default ObservableBooleanValue visibleProperty() {
		return null;
	}

	/**
	 * Returns a property supplying the label text for the bound control.
	 *
	 * @return text property, or {@code null} if the action provides no text
	 */
	default ReadOnlyStringProperty textProperty() {
		return null;
	}

	/**
	 * Returns a string specification of the action icon, interpreted by
	 * {@code ImageUtils} (e.g. {@code "res:/icons/save@{scale}x.png"}). Returns
	 * {@code null} if the action provides no icon spec.
	 *
	 * @return icon spec property, or {@code null}
	 */
	default ReadOnlyStringProperty iconSpecProperty() {
		return null;
	}

	/**
	 * Returns a property holding a pre-built graphic {@link Node} for this action,
	 * used as a compatibility fallback when no icon spec is available. Returns
	 * {@code null} if the action provides no graphic.
	 *
	 * @return graphic node property, or {@code null}
	 */
	default ReadOnlyObjectProperty<Node> graphicProperty() {
		return null;
	}

	/**
	 * Returns a property supplying the tooltip hint for bound controls.
	 *
	 * @return hint property, or {@code null} if no tooltip is needed
	 */
	default ReadOnlyStringProperty hintProperty() {
		return null;
	}

	/**
	 * Returns a property supplying the keyboard accelerator for
	 * {@link javafx.scene.control.MenuItem} bindings.
	 *
	 * @return accelerator property, or {@code null} if no accelerator is used
	 */
	default ReadOnlyObjectProperty<KeyCombination> acceleratorProperty() {
		return null;
	}

	/**
	 * Returns a unique key used to register this action in the {@link UIActions}
	 * registry.
	 *
	 * @return non-null unique action key
	 */
	String getKey();

}
