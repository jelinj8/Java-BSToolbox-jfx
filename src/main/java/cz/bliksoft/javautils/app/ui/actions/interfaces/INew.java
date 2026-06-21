package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface INew {
	void newDocument();

	BooleanProperty getNewEnabled();

	default StringProperty getNewIconProperty() {
		return null;
	}
}
