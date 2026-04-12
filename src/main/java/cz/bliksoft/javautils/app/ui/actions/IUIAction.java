package cz.bliksoft.javautils.app.ui.actions;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCombination;

public interface IUIAction {
	void execute();

	/** controls enabling of a Control (optional) */
	default ObservableBooleanValue enabledProperty() {
		return null;
	}

	/** If false, hide the control/menu entry. (optional) */
	default ObservableBooleanValue visibleProperty() {
		return null;
	}

	/** UI presentation (optional). */
	default ReadOnlyStringProperty textProperty() {
		return null;
	}

	/**
	 * String specification of an icon
	 * 
	 * @return
	 */
	default ReadOnlyStringProperty iconSpecProperty() {
		return null;
	}

	/**
	 * graphical Node factory
	 * 
	 * @return
	 */
	default ReadOnlyObjectProperty<Node> graphicProperty() {
		return null;
	}

	/** Mouse-hover hint shown as a tooltip on bound controls (optional). */
	default ReadOnlyStringProperty hintProperty() {
		return null;
	}

	/** Useful for MenuItem (optional). */
	default ReadOnlyObjectProperty<KeyCombination> acceleratorProperty() {
		return null;
	}

	/**
	 * Key to be used for action registry
	 * 
	 * @return
	 */
	String getKey();

}
