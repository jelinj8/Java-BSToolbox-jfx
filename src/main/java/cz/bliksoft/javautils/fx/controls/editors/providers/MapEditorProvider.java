package cz.bliksoft.javautils.fx.controls.editors.providers;

import java.util.LinkedHashMap;
import java.util.Map;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.multivalue.KeyValueEditor;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.stage.Window;

/**
 * Edits a {@code Map<String,String>} value as
 * {@code name1=value1;name2=value2;...} via a dialog hosting a nested
 * {@link KeyValueEditor}, rather than a flat inline text field. Row order is
 * preserved (see {@link KeyValueEditor#getOrderedValues()}) since callers may
 * treat the first entry as a default.
 */
public class MapEditorProvider implements IValueEditorProvider<Map<String, String>> {

	@Override
	public Node createEditor(ObjectProperty<Map<String, String>> valueProperty) {
		Label display = new Label(toDisplayString(valueProperty.get()));
		display.setMaxWidth(Double.MAX_VALUE);
		display.setStyle("-fx-padding: 2 4 2 4; -fx-cursor: hand;");
		valueProperty.addListener((obs, o, n) -> display.setText(toDisplayString(n)));
		display.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2)
				showDialog(display.getScene() != null ? display.getScene().getWindow() : null, valueProperty);
		});
		return display;
	}

	@Override
	public String toDisplayString(Map<String, String> value) {
		if (value == null || value.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : value.entrySet()) {
			if (sb.length() > 0)
				sb.append(';');
			sb.append(e.getKey()).append('=').append(e.getValue() != null ? e.getValue() : "");
		}
		return sb.toString();
	}

	@Override
	public Map<String, String> fromString(String s) {
		Map<String, String> map = new LinkedHashMap<>();
		if (s == null || s.isBlank())
			return map;
		for (String part : s.split(";")) {
			int eq = part.indexOf('=');
			if (eq > 0)
				map.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
		}
		return map;
	}

	@Override
	public boolean supportsDialog() {
		return true;
	}

	@Override
	public boolean dialogOnly() {
		return true;
	}

	@Override
	public void showDialog(Window owner, ObjectProperty<Map<String, String>> valueProperty) {
		KeyValueEditor<String> inner = new KeyValueEditor<>();
		inner.setPrefWidth(360);
		inner.setOrderingEnabled(true);
		Map<String, String> current = valueProperty.get();
		inner.loadFrom(current != null ? current : Map.of());

		Dialog<Map<String, String>> dialog = new Dialog<>();
		dialog.setTitle("Edit Values");
		dialog.initOwner(owner);
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(inner);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dialog.getDialogPane().setPrefWidth(380);
		dialog.setResultConverter(bt -> bt == ButtonType.OK ? inner.getOrderedValues() : null);

		dialog.showAndWait().ifPresent(valueProperty::set);
	}
}
