package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IOpen {
	void open();

	BooleanProperty getOpenEnabled();

	default StringProperty getOpenIconProperty() {
		return null;
	}
}
