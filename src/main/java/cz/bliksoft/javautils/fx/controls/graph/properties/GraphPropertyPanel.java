package cz.bliksoft.javautils.fx.controls.graph.properties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.model.schema.PropertyDefinition;
import cz.bliksoft.dataflow.model.schema.PropertySchema;
import cz.bliksoft.dataflow.types.EdgeType;
import cz.bliksoft.dataflow.types.EdgeTypeRegistry;
import cz.bliksoft.dataflow.types.NodeType;
import cz.bliksoft.dataflow.types.NodeTypeRegistry;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.ValueEditorFactory;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.command.PropertyChangeCommand;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GraphPropertyPanel extends VBox {

	private final Label titleLabel = new Label();
	private final VBox propertiesBox = new VBox(6);
	private final ScrollPane scrollPane = new ScrollPane(propertiesBox);
	private GraphCanvas canvas;
	private UUID currentElementId;

	public GraphPropertyPanel() {
		getStyleClass().add("graph-property-panel");
		setSpacing(4);
		setPadding(new Insets(8));

		titleLabel.getStyleClass().add("graph-property-title");
		titleLabel.setMaxWidth(Double.MAX_VALUE);

		propertiesBox.setPadding(new Insets(4));

		scrollPane.setFitToWidth(true);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		VBox.setVgrow(scrollPane, Priority.ALWAYS);

		getChildren().addAll(titleLabel, scrollPane);
		showEmpty();
	}

	public void setCanvas(GraphCanvas canvas) {
		this.canvas = canvas;
		canvas.getSelectionModel().observableSelection().addListener(
				(javafx.collections.SetChangeListener.Change<? extends UUID> change) -> onSelectionChanged());
	}

	private void onSelectionChanged() {
		if (canvas == null || canvas.getGraph() == null) {
			showEmpty();
			return;
		}

		var selection = canvas.getSelectionModel().getSelection();
		if (selection.size() != 1) {
			showEmpty();
			return;
		}

		UUID id = selection.iterator().next();
		showElement(id);
	}

	private void showEmpty() {
		currentElementId = null;
		titleLabel.setText("No selection");
		propertiesBox.getChildren().clear();
	}

	public void showElement(UUID elementId) {
		currentElementId = elementId;
		propertiesBox.getChildren().clear();

		Graph graph = canvas != null ? canvas.getGraph() : null;
		if (graph == null) {
			showEmpty();
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

		showEmpty();
	}

	private void showNodeProperties(Node node) {
		NodeType type = NodeTypeRegistry.getInstance().get(node.getTypeId());
		titleLabel.setText(type != null ? type.getDisplayName() : node.getTypeId());

		addReadOnlyRow("id", node.getId().toString());

		if (node.getProperties() == null)
			node.setProperties(new LinkedHashMap<>());

		PropertySchema schema = type != null ? type.getPropertySchema() : null;
		buildPropertyEditors(node.getId(), node.getProperties(), schema);

		if (!node.getJoinPoints().isEmpty()) {
			Label jpHeader = new Label("Join Points");
			jpHeader.setStyle("-fx-font-weight: bold; -fx-padding: 6 0 2 0;");
			propertiesBox.getChildren().add(jpHeader);

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

		PropertySchema schema = new PropertySchema(
				java.util.List.of(new PropertyDefinition("description", "String", "", false, "Group description")));

		buildPropertyEditors(group.getId(), group.getProperties(), schema);
	}

	private void addReadOnlyRow(String key, String value) {
		Label label = new Label(key);
		label.getStyleClass().add("graph-property-label");
		javafx.scene.control.TextField tf = new javafx.scene.control.TextField(value);
		tf.setEditable(false);
		tf.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #888888; -fx-font-size: 11px;");
		tf.setMaxWidth(Double.MAX_VALUE);
		VBox fieldBox = new VBox(2, label, tf);
		propertiesBox.getChildren().add(fieldBox);
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
		propertiesBox.getChildren().add(fieldBox);
	}

	private void showEdgeProperties(Edge edge) {
		EdgeType type = EdgeTypeRegistry.getInstance().get(edge.getTypeId());
		titleLabel.setText(type != null ? type.getDisplayName() + " (edge)" : edge.getTypeId());

		addReadOnlyRow("id", edge.getId().toString());

		if (edge.getProperties() == null)
			edge.setProperties(new LinkedHashMap<>());

		PropertySchema schema = type != null ? type.getPropertySchema() : null;
		buildPropertyEditors(edge.getId(), edge.getProperties(), schema);
	}

	private void buildPropertyEditors(UUID elementId, Map<String, Object> properties, PropertySchema schema) {
		if (schema != null) {
			for (PropertyDefinition def : schema.getDefinitions())
				addPropertyRow(elementId, properties, def.getName(), resolveType(def.getType()));
		}

		for (String key : properties.keySet()) {
			if (schema != null && schema.getDefinition(key) != null)
				continue;
			addPropertyRow(elementId, properties, key, String.class);
		}
	}

	private void addPropertyRow(UUID elementId, Map<String, Object> properties, String key, Class<?> type) {
		Label label = new Label(key);
		label.getStyleClass().add("graph-property-label");

		Object currentValue = properties.get(key);
		String strValue = currentValue != null ? currentValue.toString() : "";
		final Object originalValue = convertValue(strValue, type);

		ObjectProperty<String> valueProp = new SimpleObjectProperty<>(strValue);

		IValueEditorProvider<String> provider = ValueEditorFactory.forStringType(type);
		javafx.scene.Node editor = provider.createEditor(valueProp);

		Runnable commitEdit = () -> {
			if (canvas == null)
				return;
			Object typedNew = convertValue(valueProp.get(), type);
			if (Objects.equals(originalValue, typedNew))
				return;
			properties.put(key, typedNew);
			PropertyChangeCommand cmd = new PropertyChangeCommand(canvas.getGraph(), elementId, key, originalValue,
					typedNew);
			canvas.getCommandHistory().execute(cmd);
			canvas.refreshNodeVisual(elementId);
		};

		if (editor instanceof javafx.scene.control.TextField tf) {
			tf.setOnAction(e -> commitEdit.run());
			tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
				if (!isFocused)
					commitEdit.run();
			});
		} else {
			valueProp.addListener((obs, oldVal, newVal) -> {
				if (canvas == null || Objects.equals(oldVal, newVal))
					return;
				Object typedNew = convertValue(newVal, type);
				properties.put(key, typedNew);
				PropertyChangeCommand cmd = new PropertyChangeCommand(canvas.getGraph(), elementId, key, originalValue,
						typedNew);
				canvas.getCommandHistory().execute(cmd);
				canvas.refreshNodeVisual(elementId);
			});
		}

		VBox fieldBox = new VBox(2, label, editor);
		propertiesBox.getChildren().add(fieldBox);
	}

	private Object convertValue(String value, Class<?> type) {
		if (value == null || value.isEmpty())
			return null;
		if (type == Integer.class || type == int.class) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return value;
			}
		}
		if (type == Double.class || type == double.class) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				return value;
			}
		}
		if (type == Boolean.class || type == boolean.class)
			return Boolean.parseBoolean(value);
		return value;
	}

	private Class<?> resolveType(String typeName) {
		if (typeName == null)
			return String.class;
		return switch (typeName) {
		case "String", "string" -> String.class;
		case "Integer", "integer", "int" -> Integer.class;
		case "Double", "double" -> Double.class;
		case "Boolean", "boolean" -> Boolean.class;
		default -> String.class;
		};
	}

	private Node findNode(Graph graph, UUID id) {
		return graph.getNodes().stream().filter(n -> n.getId().equals(id)).findFirst().orElse(null);
	}

	private Edge findEdge(Graph graph, UUID id) {
		return graph.getEdges().stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
	}

	private Group findGroup(Graph graph, UUID id) {
		return graph.getGroups().stream().filter(g -> g.getId().equals(id)).findFirst().orElse(null);
	}

	public void refresh() {
		if (currentElementId != null)
			showElement(currentElementId);
		else
			showEmpty();
	}
}
