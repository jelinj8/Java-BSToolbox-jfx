package cz.bliksoft.javautils.fx.controls.graph.render;

import java.util.Map;

import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.types.NodeType;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class ShapeNodeRenderer implements INodeRenderer {

	@Override
	public Region createNodeVisual(Node node, NodeType type, RenderContext ctx) {
		double w = node.getWidth();
		double h = node.getHeight();

		Shape shape = createShape(type.getShapeType(), w, h);

		shape.getStyleClass().add("graph-node-shape");
		shape.setFill(Color.WHITE);
		shape.setStroke(Color.web("#555555"));
		shape.setStrokeWidth(1.5);

		String labelText = getLabel(node, type);
		Label label = new Label(labelText);
		label.getStyleClass().add("graph-node-label");
		label.setMaxWidth(w - 10);

		boolean wrapText = getBoolProperty(node, "wrapText", true);
		label.setWrapText(wrapText);

		StringBuilder style = new StringBuilder();
		Integer fontSize = getIntProperty(node, "fontSize");
		if (fontSize != null && fontSize > 0)
			style.append("-fx-font-size: ").append(fontSize).append("px;");

		String alignment = getStringProperty(node, "alignment");
		if (alignment != null) {
			switch (alignment.toUpperCase()) {
			case "LEFT" -> style.append("-fx-text-alignment: left;");
			case "CENTER" -> style.append("-fx-text-alignment: center;");
			case "RIGHT" -> style.append("-fx-text-alignment: right;");
			}
		}

		if (!style.isEmpty())
			label.setStyle(style.toString());

		Pos containerAlignment = Pos.CENTER;
		if (alignment != null) {
			switch (alignment.toUpperCase()) {
			case "LEFT" -> containerAlignment = Pos.CENTER_LEFT;
			case "RIGHT" -> containerAlignment = Pos.CENTER_RIGHT;
			}
		}

		StackPane container = new StackPane(shape, label);
		container.setAlignment(containerAlignment);
		container.setPrefSize(w, h);
		container.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		container.getStyleClass().add("graph-node");

		return container;
	}

	private Shape createShape(String shapeType, double w, double h) {
		if (shapeType == null)
			shapeType = "rectangle";

		return switch (shapeType) {
		case "rounded-rectangle" -> {
			Rectangle r = new Rectangle(w, h);
			r.setArcWidth(15);
			r.setArcHeight(15);
			yield r;
		}
		case "diamond" -> new Polygon(w / 2, 0, w, h / 2, w / 2, h, 0, h / 2);
		case "circle" -> {
			double radius = Math.min(w, h) / 2;
			Circle c = new Circle(radius);
			c.setTranslateX(w / 2);
			c.setTranslateY(h / 2);
			yield c;
		}
		case "parallelogram" -> {
			double skew = w * 0.15;
			yield new Polygon(skew, 0, w, 0, w - skew, h, 0, h);
		}
		case "bracket" -> {
			Rectangle r = new Rectangle(w, h);
			r.setFill(Color.TRANSPARENT);
			r.setStroke(Color.web("#555555"));
			r.setStrokeWidth(1.5);
			r.getStrokeDashArray().addAll(6.0, 3.0);
			yield r;
		}
		case "triangle-right" -> new Polygon(0, 0, w, h / 2, 0, h);
		case "triangle-left" -> new Polygon(w, 0, 0, h / 2, w, h);
		case "none" -> {
			Rectangle r = new Rectangle(w, h);
			r.setFill(Color.TRANSPARENT);
			r.setStroke(Color.TRANSPARENT);
			yield r;
		}
		default -> new Rectangle(w, h);
		};
	}

	private String getLabel(Node node, NodeType type) {
		Map<String, Object> props = node.getProperties();
		if (props != null) {
			Object label = props.get("label");
			if (label != null && !label.toString().isEmpty())
				return label.toString();
		}
		return type.getDisplayName();
	}

	private Integer getIntProperty(Node node, String key) {
		Map<String, Object> props = node.getProperties();
		if (props == null)
			return null;
		Object val = props.get(key);
		if (val instanceof Integer i)
			return i;
		if (val != null) {
			try {
				return Integer.parseInt(val.toString());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private boolean getBoolProperty(Node node, String key, boolean defaultValue) {
		Map<String, Object> props = node.getProperties();
		if (props == null)
			return defaultValue;
		Object val = props.get(key);
		if (val instanceof Boolean b)
			return b;
		if (val != null)
			return Boolean.parseBoolean(val.toString());
		return defaultValue;
	}

	private String getStringProperty(Node node, String key) {
		Map<String, Object> props = node.getProperties();
		if (props == null)
			return null;
		Object val = props.get(key);
		return val != null ? val.toString() : null;
	}
}
