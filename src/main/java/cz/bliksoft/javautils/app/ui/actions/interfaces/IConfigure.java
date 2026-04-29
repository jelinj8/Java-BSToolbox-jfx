package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IConfigure {
	void configure();

	BooleanProperty getConfigureEnabled();

	default StringProperty getConfigureIconProperty() {
		return null;
	}
}
