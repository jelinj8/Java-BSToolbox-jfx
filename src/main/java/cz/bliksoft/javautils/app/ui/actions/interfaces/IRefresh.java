package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IRefresh {
	void refresh();

	BooleanProperty getRefreshEnabled();

	default StringProperty getRefreshIconProperty() {
		return null;
	}
}
