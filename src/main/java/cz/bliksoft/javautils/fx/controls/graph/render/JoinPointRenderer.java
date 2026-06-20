package cz.bliksoft.javautils.fx.controls.graph.render;

import java.util.List;

import cz.bliksoft.dataflow.model.Direction;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.JoinPointPosition;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public abstract class JoinPointRenderer {

	private static final double INDICATOR_RADIUS = 4.0;
	private static final double TRIANGLE_SIZE = 5.0;

	private static final Color COLOR_IN = Color.web("#4CAF50");
	private static final Color COLOR_OUT = Color.web("#2196F3");
	private static final Color COLOR_BIDIR = Color.web("#FF9800");
	private static final Color COLOR_IN_OUT = Color.web("#8BC34A");
	private static final Color COLOR_OUT_IN = Color.web("#03A9F4");

	public static void renderJoinPoints(Pane nodeContainer, List<JoinPoint> joinPoints, double nodeWidth,
			double nodeHeight) {
		renderJoinPoints(nodeContainer, joinPoints, nodeWidth, nodeHeight, false);
	}

	public static void renderJoinPoints(Pane nodeContainer, List<JoinPoint> joinPoints, double nodeWidth,
			double nodeHeight, boolean showLabels) {
		for (JoinPoint jp : joinPoints) {
			double[] pos = computePosition(jp.getPosition(), jp.getCustomX(), jp.getCustomY(), nodeWidth, nodeHeight);

			Shape indicator = createIndicator(jp);
			indicator.getStyleClass().add("graph-join-point");
			indicator.getProperties().put("joinPointId", jp.getId());
			indicator.setLayoutX(pos[0]);
			indicator.setLayoutY(pos[1]);

			nodeContainer.getChildren().add(indicator);

			if (showLabels && jp.getName() != null && !jp.getName().isEmpty()) {
				Label label = new Label(jp.getName());
				label.getStyleClass().add("graph-join-point-label");
				label.setMouseTransparent(true);
				label.setStyle("-fx-font-size: 9px; -fx-text-fill: #666666;");

				JoinPointPosition effectivePos = effectivePosition(jp.getPosition(), jp.getCustomX(), jp.getCustomY());
				positionLabelOutside(label, pos, effectivePos);
				nodeContainer.getChildren().add(label);
			}
		}
	}

	private static Shape createIndicator(JoinPoint jp) {
		boolean unlimited = jp.getMaxConnections() < 0 || jp.getMaxConnections() > 1;
		Color color = colorForDirection(jp.getDirection());

		if (unlimited) {
			double s = TRIANGLE_SIZE;
			Polygon triangle = new Polygon(-s, -s, s, 0, -s, s);
			triangle.setFill(color);
			triangle.setStroke(Color.WHITE);
			triangle.setStrokeWidth(1.0);
			return triangle;
		}

		Circle circle = new Circle(INDICATOR_RADIUS);
		circle.setFill(color);
		circle.setStroke(Color.WHITE);
		circle.setStrokeWidth(1.0);
		return circle;
	}

	private static JoinPointPosition effectivePosition(JoinPointPosition pos, double customX, double customY) {
		if (pos != JoinPointPosition.CUSTOM)
			return pos;
		if (customX == 0)
			return JoinPointPosition.LEFT;
		if (customX == 1)
			return JoinPointPosition.RIGHT;
		if (customY == 0)
			return JoinPointPosition.TOP;
		if (customY == 1)
			return JoinPointPosition.BOTTOM;
		return pos;
	}

	private static void positionLabelOutside(Label label, double[] pos, JoinPointPosition jpPos) {
		double lx = pos[0], ly = pos[1];

		switch (jpPos) {
		case RIGHT, TOP_RIGHT, BOTTOM_RIGHT -> {
			label.setLayoutX(lx + 8);
			label.setLayoutY(ly - 7);
		}
		case LEFT, TOP_LEFT, BOTTOM_LEFT -> {
			label.setLayoutX(lx - 8);
			label.setLayoutY(ly - 7);
			label.setStyle(label.getStyle() + "-fx-translate-x: -100%;");
			label.widthProperty().addListener((obs, o, n) -> label.setTranslateX(-n.doubleValue()));
		}
		case TOP -> {
			label.setLayoutX(lx + 6);
			label.setLayoutY(ly - 16);
		}
		case BOTTOM -> {
			label.setLayoutX(lx + 6);
			label.setLayoutY(ly + 4);
		}
		default -> {
			label.setLayoutX(lx + 8);
			label.setLayoutY(ly - 7);
		}
		}
	}

	public static double[] computePosition(JoinPointPosition position, double customX, double customY, double w,
			double h) {
		return switch (position) {
		case TOP -> new double[] { w / 2, 0 };
		case BOTTOM -> new double[] { w / 2, h };
		case LEFT -> new double[] { 0, h / 2 };
		case RIGHT -> new double[] { w, h / 2 };
		case TOP_LEFT -> new double[] { 0, 0 };
		case TOP_RIGHT -> new double[] { w, 0 };
		case BOTTOM_LEFT -> new double[] { 0, h };
		case BOTTOM_RIGHT -> new double[] { w, h };
		case CUSTOM -> new double[] { w * customX, h * customY };
		};
	}

	private static Color colorForDirection(Direction direction) {
		return switch (direction) {
		case IN -> COLOR_IN;
		case OUT -> COLOR_OUT;
		case BIDIR -> COLOR_BIDIR;
		case IN_OUT -> COLOR_IN_OUT;
		case OUT_IN -> COLOR_OUT_IN;
		};
	}
}
