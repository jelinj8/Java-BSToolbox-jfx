package cz.bliksoft.javautils.fx.controls.graph.render;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.types.NodeType;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
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

	private static final Logger log = LogManager.getLogger(ShapeNodeRenderer.class);

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

		StackPane container = new StackPane();
		container.getChildren().add(shape);
		javafx.scene.Node icon = resolveIcon(type, w, h);
		if (icon != null)
			container.getChildren().add(icon);
		container.getChildren().add(label);
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

	/**
	 * Resolves the node type's {@code iconSpec} (if any) into a scene-graph node
	 * via {@link ImageUtils}. The spec may reference the node's size through the
	 * {@code ${nodeWidth}} / {@code ${nodeHeight}} variables (substituted here
	 * before resolution), so e.g. {@code "myicon.svg|${nodeWidth}|${nodeHeight}"}
	 * scales the SVG to the node. Returns {@code null} when no icon is configured
	 * or resolution fails.
	 */
	private javafx.scene.Node resolveIcon(NodeType type, double w, double h) {
		String spec = type.getIconSpec();
		if (spec == null || spec.isBlank())
			return null;
		String resolved = resolveIconSpec(spec, w, h);
		try {
			javafx.scene.Node icon = ImageUtils.getIconNode(resolved);
			if (icon != null)
				icon.setMouseTransparent(true);
			return icon;
		} catch (Exception e) {
			log.warn("Failed to resolve node iconSpec '{}' (resolved '{}'): {}", spec, resolved, e.getMessage());
			return null;
		}
	}

	/** Substitutes the node-size variables into an icon spec (pixel-rounded). */
	static String resolveIconSpec(String spec, double w, double h) {
		return spec.replace("${nodeWidth}", String.valueOf((int) Math.round(w))).replace("${nodeHeight}",
				String.valueOf((int) Math.round(h)));
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
