package cz.bliksoft.javautils.app.ui.actions.interfaces;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface IPreview {
	void preview();

	BooleanProperty getPreviewEnabled();

	default StringProperty getPreviewIconProperty() {
		return null;
	}
}
