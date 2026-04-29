package cz.bliksoft.javautils.fx.controls.editors.providers;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class StringEditorProvider implements IValueEditorProvider<String> {

	@Override
	public Node createEditor(ObjectProperty<String> prop) {
		TextField tf = new TextField();
		tf.setMaxWidth(Double.MAX_VALUE);
		tf.textProperty().bindBidirectional(prop);
		return tf;
	}

	@Override
	public String toDisplayString(String value) {
		return value != null ? value : "";
	}

	@Override
	public String fromString(String s) {
		return s;
	}
}
