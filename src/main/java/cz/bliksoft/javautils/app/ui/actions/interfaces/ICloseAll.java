package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface ICloseAll {
	void closeAll();

	BooleanProperty getCloseAllEnabled();

	default StringProperty getCloseAllIconProperty() {
		return null;
	}
}
