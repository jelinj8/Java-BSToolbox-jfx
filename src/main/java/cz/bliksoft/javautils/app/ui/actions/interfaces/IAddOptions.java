package cz.bliksoft.javautils.app.ui.actions.interfaces;

import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

public interface IAddOptions {
	ObservableList<IUIAction> getOptions();
	BooleanProperty getAddSelectEnabled();
	default StringProperty getAddSelectIconProperty() { return null; }
}
