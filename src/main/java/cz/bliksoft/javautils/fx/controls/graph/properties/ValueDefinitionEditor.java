package cz.bliksoft.javautils.fx.controls.graph.properties;

import cz.bliksoft.dataflow.model.schema.ValueDefinition;
import cz.bliksoft.dataflow.model.schema.ValueSource;
import cz.bliksoft.dataflow.model.schema.ValueType;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.ValueEditorFactory;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class ValueDefinitionEditor extends VBox {

	private final ComboBox<ValueSource> sourceCombo = new ComboBox<>();
	private final VBox editorArea = new VBox(2);
	private final ValueType lockedType;
	private ValueDefinition current;
	private Consumer<ValueDefinition> onChange;

	public ValueDefinitionEditor() {
		this(null, null);
	}

	public ValueDefinitionEditor(List<ValueSource> allowedSources) {
		this(allowedSources, null);
	}

	/**
	 * @param allowedSources the value sources offered, or {@code null}/empty for
	 *                       all
	 * @param lockedType     when non-{@code null}, the output {@link ValueType} is
	 *                       preselected to this value and the type selector is
	 *                       disabled in every sub-editor
	 */
	public ValueDefinitionEditor(List<ValueSource> allowedSources, ValueType lockedType) {
		this.lockedType = lockedType;
		setSpacing(2);

		if (allowedSources != null && !allowedSources.isEmpty())
			sourceCombo.getItems().addAll(allowedSources);
		else
			sourceCombo.getItems().addAll(ValueSource.values());

		sourceCombo.setMaxWidth(Double.MAX_VALUE);
		sourceCombo.setOnAction(e -> {
			if (current != null) {
				current.setSource(sourceCombo.getValue());
				buildSubEditor();
				fireChange();
			}
		});

		getChildren().addAll(sourceCombo, editorArea);
	}

	public void setValue(ValueDefinition def) {
		this.current = def != null ? def : new ValueDefinition();
		if (lockedType != null)
			current.setValueType(lockedType);
		sourceCombo.setValue(current.getSource());
		buildSubEditor();
	}

	public ValueDefinition getValue() {
		return current;
	}

	public void setOnChange(Consumer<ValueDefinition> onChange) {
		this.onChange = onChange;
	}

	private void buildSubEditor() {
		editorArea.getChildren().clear();
		if (current == null)
			return;

		switch (current.getSource()) {
		case SCALAR -> buildScalarEditor();
		case VARIABLE -> buildVariableEditor();
		case XPATH -> buildXPathEditor();
		case EXPRESSION -> buildExpressionEditor();
		}
	}

	private void buildScalarEditor() {
		ComboBox<ValueType> typeCombo = new ComboBox<>();
		typeCombo.getItems().addAll(ValueType.values());
		ValueType vt = current.getValueType() != null ? current.getValueType() : ValueType.STRING;
		typeCombo.setValue(vt);
		typeCombo.setMaxWidth(Double.MAX_VALUE);

		VBox valueContainer = new VBox();
		buildTypedValueEditor(valueContainer, vt);

		typeCombo.setOnAction(e -> {
			current.setValueType(typeCombo.getValue());
			current.setExpression("");
			buildTypedValueEditor(valueContainer, typeCombo.getValue());
			fireChange();
		});
		lockTypeCombo(typeCombo);

		editorArea.getChildren().addAll(typeCombo, valueContainer);
	}

	/**
	 * When a locked type is configured, pins the combo to it and disables it.
	 */
	private void lockTypeCombo(ComboBox<ValueType> combo) {
		if (lockedType != null) {
			combo.setValue(lockedType);
			combo.setDisable(true);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void buildTypedValueEditor(VBox container, ValueType vt) {
		container.getChildren().clear();
		Class<?> javaType = vt != null ? vt.getJavaType() : String.class;
		IValueEditorProvider<String> provider = ValueEditorFactory.forStringType(javaType);

		ObjectProperty<String> valueProp = new SimpleObjectProperty<>(
				current.getExpression() != null ? current.getExpression() : "");
		javafx.scene.Node editor = provider.createEditor(valueProp);

		Runnable commit = () -> {
			current.setExpression(valueProp.get());
			fireChange();
		};
		if (editor instanceof TextField tf) {
			tf.setOnAction(e -> commit.run());
			tf.focusedProperty().addListener((obs, was, is) -> {
				if (!is)
					commit.run();
			});
		} else {
			valueProp.addListener((obs, o, n) -> commit.run());
		}

		container.getChildren().add(editor);
	}

	private void buildVariableEditor() {
		TextField nameField = new TextField(current.getExpression() != null ? current.getExpression() : "");
		nameField.setPromptText("Variable name");
		nameField.setMaxWidth(Double.MAX_VALUE);
		commitOnAction(nameField, v -> {
			current.setExpression(v);
			fireChange();
		});

		editorArea.getChildren().add(nameField);
	}

	private void buildXPathEditor() {
		ComboBox<ValueType> typeCombo = new ComboBox<>();
		typeCombo.getItems().addAll(ValueType.values());
		typeCombo.setValue(current.getValueType() != null ? current.getValueType() : ValueType.STRING);
		typeCombo.setMaxWidth(Double.MAX_VALUE);
		typeCombo.setOnAction(e -> {
			current.setValueType(typeCombo.getValue());
			fireChange();
		});
		lockTypeCombo(typeCombo);

		TextArea exprArea = new TextArea(current.getExpression() != null ? current.getExpression() : "");
		exprArea.setPromptText("XPath expression");
		exprArea.setWrapText(true);
		exprArea.setPrefRowCount(3);
		exprArea.setMinHeight(60);
		exprArea.setMaxWidth(Double.MAX_VALUE);
		VBox.setVgrow(exprArea, Priority.ALWAYS);
		exprArea.focusedProperty().addListener((obs, was, is) -> {
			if (!is) {
				current.setExpression(exprArea.getText());
				fireChange();
			}
		});

		ComboBox<String> xpathSourceCombo = new ComboBox<>();
		xpathSourceCombo.getItems().addAll("payload", "config");
		xpathSourceCombo.setEditable(true);
		xpathSourceCombo.setValue(current.getXpathSource() != null ? current.getXpathSource() : "payload");
		xpathSourceCombo.setPromptText("Source (payload, variable:name, config)");
		xpathSourceCombo.setMaxWidth(Double.MAX_VALUE);
		xpathSourceCombo.setOnAction(e -> {
			current.setXpathSource(xpathSourceCombo.getValue());
			fireChange();
		});

		Label sourceLabel = new Label("Source:");
		sourceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

		editorArea.getChildren().addAll(typeCombo, exprArea, sourceLabel, xpathSourceCombo);
	}

	private void buildExpressionEditor() {
		TextField exprField = new TextField(current.getExpression() != null ? current.getExpression() : "");
		exprField.setPromptText("Expression (e.g. x * 2 + 1)");
		exprField.setMaxWidth(Double.MAX_VALUE);
		commitOnAction(exprField, v -> {
			current.setExpression(v);
			fireChange();
		});

		TextField defaultField = new TextField(current.getDefaultValue() != null ? current.getDefaultValue() : "");
		defaultField.setPromptText("Default value (optional)");
		defaultField.setMaxWidth(Double.MAX_VALUE);
		commitOnAction(defaultField, v -> {
			current.setDefaultValue(v);
			fireChange();
		});

		ComboBox<ValueType> typeCombo = new ComboBox<>();
		typeCombo.getItems().addAll(ValueType.values());
		typeCombo.setValue(current.getValueType() != null ? current.getValueType() : ValueType.DOUBLE);
		typeCombo.setMaxWidth(Double.MAX_VALUE);
		typeCombo.setOnAction(e -> {
			current.setValueType(typeCombo.getValue());
			fireChange();
		});
		lockTypeCombo(typeCombo);

		Label defaultLabel = new Label("Default:");
		defaultLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

		editorArea.getChildren().addAll(typeCombo, exprField, defaultLabel, defaultField);
	}

	private void commitOnAction(TextField tf, Consumer<String> setter) {
		Runnable commit = () -> setter.accept(tf.getText());
		tf.setOnAction(e -> commit.run());
		tf.focusedProperty().addListener((obs, was, is) -> {
			if (!is)
				commit.run();
		});
	}

	private void fireChange() {
		if (onChange != null && current != null)
			onChange.accept(current);
	}
}
