package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface ISave {
	void save();
	BooleanProperty getSaveEnabled();
	default StringProperty getSaveIconProperty() { return null; }
}
