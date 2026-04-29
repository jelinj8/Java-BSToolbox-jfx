package cz.bliksoft.javautils.fx.controls.editors.providers;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;

public class EnumEditorProvider<E extends Enum<E>> implements IValueEditorProvider<E> {

	private final Class<E> type;

	public EnumEditorProvider(Class<E> type) {
		this.type = type;
	}

	@Override
	public Node createEditor(ObjectProperty<E> prop) {
		ComboBox<E> cb = new ComboBox<>();
		cb.getItems().setAll(type.getEnumConstants());
		cb.setMaxWidth(Double.MAX_VALUE);
		cb.setValue(prop.get());
		cb.valueProperty().bindBidirectional(prop);
		return cb;
	}

	@Override
	public String toDisplayString(E value) {
		return value != null ? value.name() : "";
	}

	@Override
	public E fromString(String s) {
		if (s == null)
			return null;
		try {
			return Enum.valueOf(type, s.trim());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String providerKey() {
		return "enum:" + type.getName();
	}
}
