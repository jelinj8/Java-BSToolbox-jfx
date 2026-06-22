package cz.bliksoft.javautils.fx.controls.graph.properties;

import cz.bliksoft.dataflow.model.schema.ValueDefinition;
import cz.bliksoft.dataflow.model.schema.ValueSource;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.stage.Window;

public class ValueDefinitionEditorProvider implements IValueEditorProvider<ValueDefinition> {

	@Override
	public Node createEditor(ObjectProperty<ValueDefinition> valueProperty) {
		Label display = new Label(toDisplayString(safeGet(valueProperty)));
		display.setMaxWidth(Double.MAX_VALUE);
		display.setStyle("-fx-padding: 2 4 2 4; -fx-cursor: hand;");
		valueProperty.addListener((obs, o, n) -> display.setText(toDisplayString(safeGet(valueProperty))));
		display.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2)
				showDialog(display.getScene() != null ? display.getScene().getWindow() : null, valueProperty);
		});
		return display;
	}

	@SuppressWarnings("unchecked")
	private static ValueDefinition safeGet(ObjectProperty<ValueDefinition> prop) {
		Object raw = ((ObjectProperty<Object>) (ObjectProperty<?>) prop).get();
		if (raw instanceof ValueDefinition vd)
			return vd;
		return null;
	}

	@Override
	public String toDisplayString(ValueDefinition value) {
		if (value == null)
			return "";
		String expr = value.getExpression() != null ? value.getExpression() : "";
		if (value.getSource() == null)
			return expr;
		return switch (value.getSource()) {
		case SCALAR -> expr;
		case VARIABLE -> "$" + expr;
		case XPATH -> "xpath: " + expr;
		case EXPRESSION -> "=" + expr;
		};
	}

	@Override
	public ValueDefinition fromString(String s) {
		if (s == null || s.isEmpty())
			return new ValueDefinition();
		return ValueDefinition.scalar(cz.bliksoft.dataflow.model.schema.ValueType.STRING, s);
	}

	@Override
	public boolean supportsDialog() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void showDialog(Window owner, ObjectProperty<ValueDefinition> valueProperty) {
		Object raw = ((ObjectProperty<Object>) (ObjectProperty<?>) valueProperty).get();
		ValueDefinition current;
		if (raw instanceof ValueDefinition vd)
			current = vd;
		else
			current = fromString(raw != null ? raw.toString() : null);

		ValueDefinitionEditor editor = new ValueDefinitionEditor();
		ValueDefinition copy = copyDef(current);
		editor.setValue(copy);

		Dialog<ValueDefinition> dialog = new Dialog<>();
		dialog.setTitle("Edit Value");
		dialog.initOwner(owner);
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(editor);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dialog.getDialogPane().setPrefWidth(350);
		dialog.setResultConverter(bt -> bt == ButtonType.OK ? editor.getValue() : null);

		editor.heightProperty().addListener((obs, o, n) -> {
			if (dialog.getDialogPane().getScene() != null)
				dialog.getDialogPane().getScene().getWindow().sizeToScene();
		});

		dialog.showAndWait().ifPresent(valueProperty::set);
	}

	private static ValueDefinition copyDef(ValueDefinition src) {
		ValueDefinition copy = new ValueDefinition(src.getSource(), src.getValueType(), src.getExpression());
		copy.setXpathSource(src.getXpathSource());
		copy.setInputJoinPoint(src.getInputJoinPoint());
		copy.setDefaultValue(src.getDefaultValue());
		return copy;
	}
}
