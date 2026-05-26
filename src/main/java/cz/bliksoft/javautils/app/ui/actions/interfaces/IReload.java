package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IReload {
	void reload();

	BooleanProperty getReloadEnabled();

	default StringProperty getReloadIconProperty() {
		return null;
	}
}
