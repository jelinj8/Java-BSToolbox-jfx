package cz.bliksoft.javautils.fx.controls.editors.providers;

import java.time.LocalDate;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;

public class LocalDateEditorProvider implements IValueEditorProvider<LocalDate> {

	@Override
	public Node createEditor(ObjectProperty<LocalDate> prop) {
		DatePicker dp = new DatePicker(prop.get());
		dp.setMaxWidth(Double.MAX_VALUE);
		dp.valueProperty().bindBidirectional(prop);
		return dp;
	}

	@Override
	public String toDisplayString(LocalDate value) {
		return value != null ? value.toString() : "";
	}

	@Override
	public LocalDate fromString(String s) {
		if (s == null || s.isBlank())
			return null;
		try {
			return LocalDate.parse(s.trim());
		} catch (Exception e) {
			return null;
		}
	}
}
