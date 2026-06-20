package cz.bliksoft.javautils.fx.controls.graph.render;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

abstract class ArrowHead {

	private static final double SIZE = 10;
	private static final double WIDTH = 6;

	static Polygon create(double x, double y, double angle) {
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);

		double x1 = x - SIZE * cos + WIDTH * sin;
		double y1 = y - SIZE * sin - WIDTH * cos;
		double x2 = x - SIZE * cos - WIDTH * sin;
		double y2 = y - SIZE * sin + WIDTH * cos;

		Polygon arrow = new Polygon(x, y, x1, y1, x2, y2);
		arrow.setFill(Color.web("#555555"));
		arrow.setStroke(Color.TRANSPARENT);
		arrow.getStyleClass().add("graph-edge-arrow");
		return arrow;
	}

	static double angle(double fromX, double fromY, double toX, double toY) {
		return Math.atan2(toY - fromY, toX - fromX);
	}
}
