package cz.bliksoft.javautils.fx.controls.graph.render;

import java.util.List;
import java.util.Map;

import cz.bliksoft.dataflow.model.Directionality;
import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Point2D;
import cz.bliksoft.dataflow.types.EdgeType;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;

public class PolylineEdgeRenderer implements IEdgeRenderer {

	@Override
	public Group createEdgeVisual(Edge edge, EdgeType type, Point2D sourcePos, Point2D targetPos,
			List<Point2D> waypoints, RenderContext ctx) {

		Group group = new Group();
		group.getStyleClass().add("graph-edge");
		group.getProperties().put("edgeId", edge.getId());

		double sx = sourcePos.getX(), sy = sourcePos.getY();
		double tx = targetPos.getX(), ty = targetPos.getY();

		double[] coords = buildCoords(sx, sy, waypoints, tx, ty);
		Polyline polyline = new Polyline(coords);
		polyline.setStroke(Color.web("#555555"));
		polyline.setStrokeWidth(1.5);
		polyline.setFill(Color.TRANSPARENT);
		polyline.getStyleClass().add("graph-edge-line");
		group.getChildren().add(polyline);

		double lastFromX, lastFromY;
		if (waypoints != null && !waypoints.isEmpty()) {
			Point2D last = waypoints.get(waypoints.size() - 1);
			lastFromX = last.getX();
			lastFromY = last.getY();
		} else {
			lastFromX = sx;
			lastFromY = sy;
		}

		double angle = ArrowHead.angle(lastFromX, lastFromY, tx, ty);
		group.getChildren().add(ArrowHead.create(tx, ty, angle));

		if (edge.getDirectionality() == Directionality.BIDIRECTIONAL) {
			double firstToX, firstToY;
			if (waypoints != null && !waypoints.isEmpty()) {
				firstToX = waypoints.get(0).getX();
				firstToY = waypoints.get(0).getY();
			} else {
				firstToX = tx;
				firstToY = ty;
			}
			double revAngle = ArrowHead.angle(firstToX, firstToY, sx, sy);
			group.getChildren().add(ArrowHead.create(sx, sy, revAngle));
		}

		if (waypoints != null) {
			for (Point2D wp : waypoints) {
				Circle handle = new Circle(wp.getX(), wp.getY(), 3);
				handle.setFill(Color.web("#888888"));
				handle.setStroke(Color.WHITE);
				handle.setStrokeWidth(1);
				handle.getStyleClass().add("graph-edge-waypoint");
				group.getChildren().add(handle);
			}
		}

		addLabel(group, edge, sx, sy, tx, ty);
		return group;
	}

	private double[] buildCoords(double sx, double sy, List<Point2D> waypoints, double tx, double ty) {
		int wpCount = waypoints != null ? waypoints.size() : 0;
		int total = wpCount + 2;
		double[] coords = new double[total * 2];
		coords[0] = sx;
		coords[1] = sy;
		if (waypoints != null) {
			for (int i = 0; i < waypoints.size(); i++) {
				coords[(i + 1) * 2] = waypoints.get(i).getX();
				coords[(i + 1) * 2 + 1] = waypoints.get(i).getY();
			}
		}
		coords[(total - 1) * 2] = tx;
		coords[(total - 1) * 2 + 1] = ty;
		return coords;
	}

	private void addLabel(Group group, Edge edge, double sx, double sy, double tx, double ty) {
		Map<String, Object> props = edge.getProperties();
		if (props == null)
			return;
		Object labelObj = props.get("label");
		if (labelObj == null)
			return;

		Label label = new Label(labelObj.toString());
		label.getStyleClass().add("graph-edge-label");
		label.setLayoutX((sx + tx) / 2);
		label.setLayoutY((sy + ty) / 2 - 12);
		group.getChildren().add(label);
	}
}
