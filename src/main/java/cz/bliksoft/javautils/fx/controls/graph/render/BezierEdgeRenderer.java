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
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.QuadCurve;

public class BezierEdgeRenderer implements IEdgeRenderer {

	@Override
	public Group createEdgeVisual(Edge edge, EdgeType type, Point2D sourcePos, Point2D targetPos,
			List<Point2D> waypoints, RenderContext ctx) {

		Group group = new Group();
		group.getStyleClass().add("graph-edge");
		group.getProperties().put("edgeId", edge.getId());

		double sx = sourcePos.getX(), sy = sourcePos.getY();
		double tx = targetPos.getX(), ty = targetPos.getY();

		if (waypoints != null && waypoints.size() >= 2) {
			CubicCurve curve = new CubicCurve(sx, sy, waypoints.get(0).getX(), waypoints.get(0).getY(),
					waypoints.get(1).getX(), waypoints.get(1).getY(), tx, ty);
			styleCurve(curve);
			group.getChildren().add(curve);

			double angle = ArrowHead.angle(waypoints.get(1).getX(), waypoints.get(1).getY(), tx, ty);
			group.getChildren().add(ArrowHead.create(tx, ty, angle));

			if (edge.getDirectionality() == Directionality.BIDIRECTIONAL) {
				double revAngle = ArrowHead.angle(waypoints.get(0).getX(), waypoints.get(0).getY(), sx, sy);
				group.getChildren().add(ArrowHead.create(sx, sy, revAngle));
			}
		} else if (waypoints != null && waypoints.size() == 1) {
			QuadCurve curve = new QuadCurve(sx, sy, waypoints.get(0).getX(), waypoints.get(0).getY(), tx, ty);
			styleCurve(curve);
			group.getChildren().add(curve);

			double angle = ArrowHead.angle(waypoints.get(0).getX(), waypoints.get(0).getY(), tx, ty);
			group.getChildren().add(ArrowHead.create(tx, ty, angle));

			if (edge.getDirectionality() == Directionality.BIDIRECTIONAL) {
				double revAngle = ArrowHead.angle(waypoints.get(0).getX(), waypoints.get(0).getY(), sx, sy);
				group.getChildren().add(ArrowHead.create(sx, sy, revAngle));
			}
		} else {
			double cx1 = sx + (tx - sx) * 0.4;
			double cy1 = sy;
			double cx2 = tx - (tx - sx) * 0.4;
			double cy2 = ty;

			CubicCurve curve = new CubicCurve(sx, sy, cx1, cy1, cx2, cy2, tx, ty);
			styleCurve(curve);
			group.getChildren().add(curve);

			double angle = ArrowHead.angle(cx2, cy2, tx, ty);
			group.getChildren().add(ArrowHead.create(tx, ty, angle));

			if (edge.getDirectionality() == Directionality.BIDIRECTIONAL) {
				double revAngle = ArrowHead.angle(cx1, cy1, sx, sy);
				group.getChildren().add(ArrowHead.create(sx, sy, revAngle));
			}
		}

		addLabel(group, edge, sx, sy, tx, ty);
		return group;
	}

	private void styleCurve(javafx.scene.shape.Shape curve) {
		curve.setStroke(Color.web("#555555"));
		curve.setStrokeWidth(1.5);
		curve.setFill(Color.TRANSPARENT);
		curve.getStyleClass().add("graph-edge-line");
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
