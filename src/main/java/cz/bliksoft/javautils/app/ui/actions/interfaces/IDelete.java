package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IDelete {
	void delete();

	BooleanProperty getDeleteEnabled();

	default StringProperty getDeleteIconProperty() {
		return null;
	}
}
