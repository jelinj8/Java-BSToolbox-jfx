package cz.bliksoft.javautils.fx.controls.editors.providers;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

public class IntegerEditorProvider implements IValueEditorProvider<Integer> {

    @Override
    public Node createEditor(ObjectProperty<Integer> prop) {
        TextField tf = new TextField();
        tf.setMaxWidth(Double.MAX_VALUE);
        TextFormatter<Integer> formatter = new TextFormatter<>(new IntegerStringConverter(), prop.get());
        tf.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(prop);
        return tf;
    }

    @Override
    public String toDisplayString(Integer value) {
        return value != null ? value.toString() : "";
    }

    @Override
    public Integer fromString(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
