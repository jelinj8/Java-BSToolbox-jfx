package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface ISaveAs {
	void saveAs();

	BooleanProperty getSaveAsEnabled();

	default StringProperty getSaveAsIconProperty() {
		return null;
	}
}
