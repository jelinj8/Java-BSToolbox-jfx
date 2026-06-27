package cz.bliksoft.javautils.fx.controls.graph.properties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.model.schema.ComputedVariable;
import cz.bliksoft.dataflow.model.schema.PropertyDefinition;
import cz.bliksoft.dataflow.model.schema.PropertySchema;
import cz.bliksoft.dataflow.model.schema.ValueDefinition;
import cz.bliksoft.dataflow.types.EdgeType;
import cz.bliksoft.dataflow.types.EdgeTypeRegistry;
import cz.bliksoft.dataflow.types.NodeType;
import cz.bliksoft.dataflow.types.NodeTypeRegistry;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateMeta;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.multivalue.KeyValueEditor;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.command.PropertyChangeCommand;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GraphPropertyPanel extends VBox {

	private final Label titleLabel = new Label();
	private final VBox headerBox = new VBox(4);
	private final VBox contentArea = new VBox();
	private GraphCanvas canvas;
	private UUID currentElementId;
	private Runnable onGraphNameChanged;

	public GraphPropertyPanel() {
		getStyleClass().add("graph-property-panel");
		setSpacing(4);
		setPadding(new Insets(8));

		titleLabel.getStyleClass().add("graph-property-title");
		titleLabel.setMaxWidth(Double.MAX_VALUE);

		headerBox.setPadding(new Insets(4));
		VBox.setVgrow(contentArea, Priority.ALWAYS);

		getChildren().addAll(titleLabel, headerBox, contentArea);
		showGraphProperties();
	}

	public void setOnGraphNameChanged(Runnable callback) {
		this.onGraphNameChanged = callback;
	}

	public void setCanvas(GraphCanvas canvas) {
		this.canvas = canvas;
		canvas.getSelectionModel().observableSelection().addListener(
				(javafx.collections.SetChangeListener.Change<? extends UUID> change) -> onSelectionChanged());
		showGraphProperties();
	}

	private void onSelectionChanged() {
		if (canvas == null || canvas.getGraph() == null) {
			showGraphProperties();
			return;
		}
		var selection = canvas.getSelectionModel().getSelection();
		if (selection.size() != 1) {
			showGraphProperties();
			return;
		}
		showElement(selection.iterator().next());
	}

	private void clearAll() {
		headerBox.getChildren().clear();
		contentArea.getChildren().clear();
	}

	private void showGraphProperties() {
		currentElementId = null;
		clearAll();

		if (canvas == null || canvas.getGraph() == null) {
			titleLabel.setText("No graph");
			return;
		}

		titleLabel.setText("Graph");

		Group g = canvas.getGraph();
		addReadOnlyRow("id", g.getId().toString());

		Map<String, Class<?>> registry = new LinkedHashMap<>();
		registry.put("name", String.class);
		registry.put("grid", cz.bliksoft.javautils.fx.controls.graph.GridStyle.class);
		registry.put("grid spacing", String.class);
		registry.put("snap to grid", Boolean.class);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("name", g.getName() != null ? g.getName() : "");
		data.put("grid", canvas.getGridStyle() != null ? canvas.getGridStyle().name() : "DOT");
		data.put("grid spacing", String.valueOf((int) canvas.getGridSpacing()));
		data.put("snap to grid", String.valueOf(canvas.isSnapToGrid()));

		KeyValueEditor<Object> editor = new KeyValueEditor<>();
		editor.propertyRegistryProperty().set(registry);
		editor.setKeysRestrictedToRegistry(true);
		editor.setKeysEditable(false);
		editor.setAddAction(null);
		editor.setRemoveAction(null);
		editor.loadFrom(data);

		editor.getValues().addListener((javafx.collections.MapChangeListener<String, Object>) change -> {
			if (!change.wasAdded())
				return;
			String key = change.getKey();
			String val = change.getValueAdded() != null ? change.getValueAdded().toString() : "";
			switch (key) {
			case "name" -> {
				String oldName = g.getName() != null ? g.getName() : "";
				if (oldName.equals(val))
					break;
				g.setName(val);
				canvas.getCommandHistory().execute(new cz.bliksoft.javautils.fx.controls.graph.command.IGraphCommand() {
					@Override
					public void execute() {
					}

					@Override
					public void undo() {
						g.setName(oldName);
						if (onGraphNameChanged != null)
							onGraphNameChanged.run();
					}

					@Override
					public void redo() {
						g.setName(val);
						if (onGraphNameChanged != null)
							onGraphNameChanged.run();
					}

					@Override
					public String getDescription() {
						return "Change graph name";
					}
				});
				if (onGraphNameChanged != null)
					onGraphNameChanged.run();
			}
			case "grid" -> {
				try {
					canvas.setGridStyle(cz.bliksoft.javautils.fx.controls.graph.GridStyle.valueOf(val.toUpperCase()));
				} catch (IllegalArgumentException ignore) {
				}
			}
			case "grid spacing" -> {
				try {
					canvas.setGridSpacing(Double.parseDouble(val));
				} catch (NumberFormatException ignore) {
				}
			}
			case "snap to grid" -> canvas.setSnapToGrid(Boolean.parseBoolean(val));
			}
		});

		contentArea.getChildren().add(editor);
	}

	public void showElement(UUID elementId) {
		currentElementId = elementId;
		clearAll();

		Group graph = canvas != null ? canvas.getGraph() : null;
		if (graph == null) {
			showGraphProperties();
			return;
		}

		Node node = findNode(graph, elementId);
		if (node != null) {
			showNodeProperties(node);
			return;
		}

		Edge edge = findEdge(graph, elementId);
		if (edge != null) {
			showEdgeProperties(edge);
			return;
		}

		Group group = findGroup(graph, elementId);
		if (group != null) {
			showGroupProperties(group);
			return;
		}

		showGraphProperties();
	}

	private void showNodeProperties(Node node) {
		NodeType type = NodeTypeRegistry.getInstance().get(node.getTypeId());
		titleLabel.setText(type != null ? type.getDisplayName() : node.getTypeId());

		addReadOnlyRow("id", node.getId().toString());

		if (node.getProperties() == null)
			node.setProperties(new LinkedHashMap<>());

		PropertySchema schema = type != null ? type.getPropertySchema() : null;

		KeyValueEditor<Object> propsEditor = buildPropertyEditor(node.getId(), node.getProperties(), schema);
		KeyValueEditor<ValueDefinition> varsEditor = buildComputedVariablesEditor(node);

		if (propsEditor != null && varsEditor != null) {
			SplitPane split = FxStateMeta.key(new SplitPane(), "nodeEditorSplit");
			split.setOrientation(Orientation.VERTICAL);
			split.getItems().addAll(propsEditor, varsEditor);
			split.setDividerPositions(0.6);
			VBox.setVgrow(split, Priority.ALWAYS);
			contentArea.getChildren().add(split);
		} else if (propsEditor != null) {
			VBox.setVgrow(propsEditor, Priority.ALWAYS);
			contentArea.getChildren().add(propsEditor);
		} else if (varsEditor != null) {
			VBox.setVgrow(varsEditor, Priority.ALWAYS);
			contentArea.getChildren().add(varsEditor);
		}

		if (!node.getJoinPoints().isEmpty()) {
			Label jpHeader = new Label("Join Points");
			jpHeader.setStyle("-fx-font-weight: bold; -fx-padding: 6 0 2 0;");
			headerBox.getChildren().add(jpHeader);

			for (cz.bliksoft.dataflow.model.JoinPoint jp : node.getJoinPoints()) {
				addDirectPropertyRow(jp.getDirection().name() + " (" + cardinalityLabel(jp) + ")", jp.getName(),
						val -> {
							jp.setName(val);
							canvas.refreshNodeVisual(node.getId());
						});
			}
		}
	}

	private String cardinalityLabel(cz.bliksoft.dataflow.model.JoinPoint jp) {
		return jp.getMaxConnections() < 0 ? "N"
				: jp.getMaxConnections() == 1 ? "1" : String.valueOf(jp.getMaxConnections());
	}

	private void showGroupProperties(Group group) {
		titleLabel.setText("Group");

		addReadOnlyRow("id", group.getId().toString());
		addDirectPropertyRow("name", group.getName() != null ? group.getName() : "", val -> {
			group.setName(val);
			canvas.refreshGraph();
		});

		if (group.getProperties() == null)
			group.setProperties(new LinkedHashMap<>());

		PropertySchema schema = new PropertySchema(java.util.List
				.of(new PropertyDefinition("description", "String", "", false, true, "Group description")));
		KeyValueEditor<Object> editor = buildPropertyEditor(group.getId(), group.getProperties(), schema);
		if (editor != null) {
			VBox.setVgrow(editor, Priority.ALWAYS);
			contentArea.getChildren().add(editor);
		}
	}

	private void showEdgeProperties(Edge edge) {
		EdgeType type = EdgeTypeRegistry.getInstance().get(edge.getTypeId());
		titleLabel.setText(type != null ? type.getDisplayName() + " (edge)" : edge.getTypeId());

		addReadOnlyRow("id", edge.getId().toString());

		if (edge.getProperties() == null)
			edge.setProperties(new LinkedHashMap<>());

		PropertySchema schema = type != null ? type.getPropertySchema() : null;
		KeyValueEditor<Object> editor = buildPropertyEditor(edge.getId(), edge.getProperties(), schema);
		if (editor != null) {
			VBox.setVgrow(editor, Priority.ALWAYS);
			contentArea.getChildren().add(editor);
		}
	}

	// =========================================================================
	// KeyValueEditor for properties
	// =========================================================================

	private KeyValueEditor<Object> buildPropertyEditor(UUID elementId, Map<String, Object> properties,
			PropertySchema schema) {
		if (schema == null || schema.getDefinitions().isEmpty())
			return null;

		Map<String, Class<?>> registry = new LinkedHashMap<>();
		for (PropertyDefinition def : schema.getDefinitions())
			registry.put(def.getName(), resolveType(def.getType()));

		Map<String, Object> editorData = new LinkedHashMap<>();
		for (PropertyDefinition def : schema.getDefinitions()) {
			if (def.isMandatory()) {
				Object val = properties.get(def.getName());
				editorData.put(def.getName(),
						val != null ? val : (def.getDefaultValue() != null ? def.getDefaultValue() : ""));
			}
		}
		for (var entry : properties.entrySet()) {
			if (registry.containsKey(entry.getKey()) && !editorData.containsKey(entry.getKey()))
				editorData.put(entry.getKey(), entry.getValue());
		}

		KeyValueEditor<Object> editor = new KeyValueEditor<>();
		editor.propertyRegistryProperty().set(registry);
		@SuppressWarnings("unchecked")
		IValueEditorProvider<Object> vdProvider = (IValueEditorProvider<Object>) (IValueEditorProvider<?>) new ValueDefinitionEditorProvider();
		editor.getTypeProviders().put(ValueDefinition.class, vdProvider);

		// give ValueDefinition properties that fix an output type and/or restrict
		// their sources a dedicated locked editor (the shared provider above is used
		// for unconstrained ones)
		for (PropertyDefinition def : schema.getDefinitions()) {
			boolean constrained = def.getValueType() != null
					|| (def.getAllowedSources() != null && !def.getAllowedSources().isEmpty());
			if (resolveType(def.getType()) == ValueDefinition.class && constrained) {
				@SuppressWarnings("unchecked")
				IValueEditorProvider<Object> lockedProvider = (IValueEditorProvider<Object>) (IValueEditorProvider<?>) new ValueDefinitionEditorProvider(
						def.getValueType(), def.getAllowedSources());
				editor.getKeyProviders().put(def.getName(), lockedProvider);
			}
		}
		editor.setKeysRestrictedToRegistry(true);
		editor.setKeysEditable(false);
		editor.setTitle("Properties");
		editor.setPlaceholderText("No properties");
		editor.loadFrom(editorData);

		editor.setAddAction(() -> {
			java.util.List<String> available = new java.util.ArrayList<>();
			for (PropertyDefinition def : schema.getDefinitions()) {
				if (!def.isMandatory() && !editor.getValues().containsKey(def.getName()))
					available.add(def.getName());
			}
			if (available.isEmpty())
				return;
			javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(available.get(0),
					available);
			dialog.setTitle("Add Property");
			dialog.setHeaderText(null);
			dialog.setContentText("Property:");
			dialog.showAndWait().ifPresent(name -> {
				PropertyDefinition def = schema.getDefinition(name);
				Object defaultVal = def != null && def.getDefaultValue() != null ? def.getDefaultValue() : "";
				editor.getValues().put(name, defaultVal);
				properties.put(name, defaultVal);
				showElement(currentElementId);
			});
		});

		editor.setRemoveAction(() -> {
			String key = editor.getSelectedKey();
			if (key == null)
				return;
			PropertyDefinition def = schema.getDefinition(key);
			if (def != null && def.isMandatory())
				return;
			properties.remove(key);
			editor.getValues().remove(key);
		});

		editor.getValues().addListener((javafx.collections.MapChangeListener<String, Object>) change -> {
			if (canvas == null)
				return;
			String key = change.getKey();
			if (change.wasAdded()) {
				Object oldVal = properties.get(key);
				Object newVal = change.getValueAdded();
				if (java.util.Objects.equals(oldVal, newVal))
					return;
				properties.put(key, newVal);
				PropertyChangeCommand cmd = new PropertyChangeCommand(canvas.getGraph(), elementId, key, oldVal,
						newVal);
				canvas.getCommandHistory().execute(cmd);
				canvas.refreshNodeVisual(elementId);
			} else if (change.wasRemoved() && !change.wasAdded()) {
				properties.remove(key);
			}
		});

		return editor;
	}

	// =========================================================================
	// KeyValueEditor for computed variables
	// =========================================================================

	private KeyValueEditor<ValueDefinition> buildComputedVariablesEditor(Node node) {
		KeyValueEditor<ValueDefinition> editor = new KeyValueEditor<>(new ValueDefinitionEditorProvider());
		editor.setKeysRestrictedToRegistry(false);
		editor.setKeysEditable(true);
		editor.setInlineEditing(false);
		editor.setTitle("Computed Variables");
		editor.setPlaceholderText("No variables");

		Map<String, ValueDefinition> cvMap = new LinkedHashMap<>();
		for (ComputedVariable cv : node.getComputedVariables()) {
			if (cv.getName() != null && !cv.getName().isEmpty())
				cvMap.put(cv.getName(), cv.getValue() != null ? cv.getValue() : new ValueDefinition());
		}
		editor.loadFrom(cvMap);

		editor.getValues().addListener((javafx.collections.MapChangeListener<String, ValueDefinition>) change -> {
			syncComputedVariables(node, editor.getValues());
		});

		return editor;
	}

	private void syncComputedVariables(Node node, Map<String, ValueDefinition> values) {
		node.getComputedVariables().clear();
		for (var entry : values.entrySet()) {
			if (entry.getKey() != null && !entry.getKey().isEmpty())
				node.getComputedVariables().add(new ComputedVariable(entry.getKey(), entry.getValue()));
		}
	}

	// =========================================================================
	// Simple property rows (for header-level fields)
	// =========================================================================

	private void addReadOnlyRow(String key, String value) {
		Label label = new Label(key);
		label.getStyleClass().add("graph-property-label");
		Label valueLabel = new Label(value);
		valueLabel.setStyle(
				"-fx-background-color: #f0f0f0; -fx-text-fill: #888888; -fx-font-size: 11px; -fx-padding: 4; -fx-cursor: hand;");
		valueLabel.setMaxWidth(Double.MAX_VALUE);
		valueLabel.setOnMouseClicked(e -> {
			javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
			cc.putString(value);
			javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
			valueLabel.setStyle(
					"-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-font-size: 11px; -fx-padding: 4; -fx-cursor: hand;");
			javafx.animation.PauseTransition flash = new javafx.animation.PauseTransition(
					javafx.util.Duration.millis(600));
			flash.setOnFinished(ev -> valueLabel.setStyle(
					"-fx-background-color: #f0f0f0; -fx-text-fill: #888888; -fx-font-size: 11px; -fx-padding: 4; -fx-cursor: hand;"));
			flash.play();
		});
		VBox fieldBox = new VBox(2, label, valueLabel);
		headerBox.getChildren().add(fieldBox);
	}

	private void addDirectPropertyRow(String key, String currentValue, java.util.function.Consumer<String> setter) {
		Label label = new Label(key);
		label.getStyleClass().add("graph-property-label");

		javafx.scene.control.TextField tf = new javafx.scene.control.TextField(currentValue);
		tf.setMaxWidth(Double.MAX_VALUE);

		Runnable commit = () -> {
			String newVal = tf.getText();
			if (!java.util.Objects.equals(currentValue, newVal))
				setter.accept(newVal);
		};
		tf.setOnAction(e -> commit.run());
		tf.focusedProperty().addListener((obs, was, is) -> {
			if (!is)
				commit.run();
		});

		VBox fieldBox = new VBox(2, label, tf);
		headerBox.getChildren().add(fieldBox);
	}

	// =========================================================================
	// Utilities
	// =========================================================================

	private Class<?> resolveType(String typeName) {
		if (typeName == null)
			return String.class;
		return switch (typeName) {
		case "String", "string" -> String.class;
		case "Integer", "integer", "int" -> Integer.class;
		case "Double", "double" -> Double.class;
		case "Boolean", "boolean" -> Boolean.class;
		case "ValueDefinition" -> ValueDefinition.class;
		default -> String.class;
		};
	}

	private Node findNode(Group graph, UUID id) {
		return graph.findNode(id);
	}

	private Edge findEdge(Group graph, UUID id) {
		return graph.findEdge(id);
	}

	private Group findGroup(Group graph, UUID id) {
		return graph.findGroup(id);
	}

	public void refresh() {
		if (currentElementId != null)
			showElement(currentElementId);
		else
			showGraphProperties();
	}
}
