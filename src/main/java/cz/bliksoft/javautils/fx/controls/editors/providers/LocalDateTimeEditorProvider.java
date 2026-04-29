package cz.bliksoft.javautils.fx.controls.editors.providers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class LocalDateTimeEditorProvider implements IValueEditorProvider<LocalDateTime> {

	public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final StringConverter<LocalDateTime> CONV = new StringConverter<>() {
		@Override
		public String toString(LocalDateTime v) {
			return v != null ? FMT.format(v) : "";
		}

		@Override
		public LocalDateTime fromString(String s) {
			if (s == null || s.isBlank())
				return null;
			try {
				return LocalDateTime.parse(s.trim(), FMT);
			} catch (Exception e) {
				return null;
			}
		}
	};

	@Override
	public Node createEditor(ObjectProperty<LocalDateTime> prop) {
		TextField tf = new TextField();
		tf.setMaxWidth(Double.MAX_VALUE);
		Bindings.bindBidirectional(tf.textProperty(), prop, CONV);
		return tf;
	}

	@Override
	public String toDisplayString(LocalDateTime value) {
		return value != null ? FMT.format(value) : "";
	}

	@Override
	public LocalDateTime fromString(String s) {
		return CONV.fromString(s);
	}
}
