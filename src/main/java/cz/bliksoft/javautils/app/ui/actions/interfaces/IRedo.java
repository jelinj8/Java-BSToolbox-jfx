package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IRedo {
	void redo();

	BooleanProperty getRedoEnabled();

	default StringProperty getRedoIconProperty() {
		return null;
	}
}
