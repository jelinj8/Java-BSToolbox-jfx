package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IUndo {
	void undo();

	BooleanProperty getUndoEnabled();

	default StringProperty getUndoIconProperty() {
		return null;
	}
}
