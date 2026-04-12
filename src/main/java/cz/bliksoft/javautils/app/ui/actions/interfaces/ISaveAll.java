package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface ISaveAll {
	void saveAll();
	BooleanProperty getSaveAllEnabled();
	default StringProperty getSaveAllIconProperty() { return null; }
}
