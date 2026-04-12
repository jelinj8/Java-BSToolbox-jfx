package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IPrint {
	void print();
	BooleanProperty getPrintEnabled();
	default StringProperty getPrintIconProperty() { return null; }
}
