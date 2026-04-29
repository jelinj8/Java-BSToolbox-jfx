package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IRemove {
	void remove();

	BooleanProperty getRemoveEnabled();

	default StringProperty getRemoveIconProperty() {
		return null;
	}
}
