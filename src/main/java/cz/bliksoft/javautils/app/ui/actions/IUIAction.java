package cz.bliksoft.javautils.app.ui.actions;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCombination;

public interface IUIAction {
	void execute();

	ObservableBooleanValue enabledProperty();

	/** If false, hide the control/menu entry. */
	ObservableBooleanValue visibleProperty();

	/** UI presentation (optional, but nice). */
	ReadOnlyStringProperty textProperty();

	ReadOnlyStringProperty iconSpecProperty();
	default ReadOnlyObjectProperty<Node> graphicProperty() { return null; }

	/** Useful for MenuItem (optional). */
	default ReadOnlyObjectProperty<KeyCombination> acceleratorProperty() {
		return null;
	}
}
