package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IClose {
	void close();

	BooleanProperty getCloseEnabled();

	default StringProperty getCloseIconProperty() {
		return null;
	}
}
