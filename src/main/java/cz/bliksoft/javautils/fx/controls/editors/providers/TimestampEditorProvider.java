package cz.bliksoft.javautils.fx.controls.editors.providers;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class TimestampEditorProvider implements IValueEditorProvider<Timestamp> {

    private static final StringConverter<Timestamp> CONV = new StringConverter<>() {
        @Override
        public String toString(Timestamp v) {
            return v != null ? LocalDateTimeEditorProvider.FMT.format(v.toLocalDateTime()) : "";
        }

        @Override
        public Timestamp fromString(String s) {
            if (s == null || s.isBlank())
                return null;
            try {
                return Timestamp.valueOf(LocalDateTime.parse(s.trim(), LocalDateTimeEditorProvider.FMT));
            } catch (Exception e) {
                return null;
            }
        }
    };

    @Override
    public Node createEditor(ObjectProperty<Timestamp> prop) {
        TextField tf = new TextField();
        tf.setMaxWidth(Double.MAX_VALUE);
        Bindings.bindBidirectional(tf.textProperty(), prop, CONV);
        return tf;
    }

    @Override
    public String toDisplayString(Timestamp value) {
        return CONV.toString(value);
    }

    @Override
    public Timestamp fromString(String s) {
        return CONV.fromString(s);
    }
}
