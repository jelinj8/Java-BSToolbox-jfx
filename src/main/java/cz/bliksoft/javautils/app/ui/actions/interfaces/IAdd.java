package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IAdd {
	void add();

	BooleanProperty getAddEnabled();

	default StringProperty getAddIconProperty() {
		return null;
	}
}
