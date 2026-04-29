package cz.bliksoft.javautils.fx.controls.editors.providers;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.DoubleStringConverter;

public class DoubleEditorProvider implements IValueEditorProvider<Double> {

	@Override
	public Node createEditor(ObjectProperty<Double> prop) {
		TextField tf = new TextField();
		tf.setMaxWidth(Double.MAX_VALUE);
		TextFormatter<Double> formatter = new TextFormatter<>(new DoubleStringConverter(), prop.get());
		tf.setTextFormatter(formatter);
		formatter.valueProperty().bindBidirectional(prop);
		return tf;
	}

	@Override
	public String toDisplayString(Double value) {
		return value != null ? value.toString() : "";
	}

	@Override
	public Double fromString(String s) {
		if (s == null || s.isBlank())
			return null;
		try {
			return Double.parseDouble(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
