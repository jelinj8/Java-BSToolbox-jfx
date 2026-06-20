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
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;

public class StraightLineEdgeRenderer implements IEdgeRenderer {

	@Override
	public Group createEdgeVisual(Edge edge, EdgeType type, Point2D sourcePos, Point2D targetPos,
			List<Point2D> waypoints, RenderContext ctx) {

		Group group = new Group();
		group.getStyleClass().add("graph-edge");
		group.getProperties().put("edgeId", edge.getId());

		if (waypoints == null || waypoints.isEmpty()) {
			Line line = new Line(sourcePos.getX(), sourcePos.getY(), targetPos.getX(), targetPos.getY());
			line.setStroke(Color.web("#555555"));
			line.setStrokeWidth(1.5);
			line.getStyleClass().add("graph-edge-line");
			group.getChildren().add(line);

			addArrowheads(group, edge, sourcePos.getX(), sourcePos.getY(), targetPos.getX(), targetPos.getY());
		} else {
			double[] coords = buildPolylineCoords(sourcePos, waypoints, targetPos);
			Polyline polyline = new Polyline(coords);
			polyline.setStroke(Color.web("#555555"));
			polyline.setStrokeWidth(1.5);
			polyline.setFill(Color.TRANSPARENT);
			polyline.getStyleClass().add("graph-edge-line");
			group.getChildren().add(polyline);

			Point2D lastWp = waypoints.get(waypoints.size() - 1);
			addArrowheads(group, edge, lastWp.getX(), lastWp.getY(), targetPos.getX(), targetPos.getY());

			if (edge.getDirectionality() == Directionality.BIDIRECTIONAL) {
				Point2D firstWp = waypoints.get(0);
				double angle = ArrowHead.angle(firstWp.getX(), firstWp.getY(), sourcePos.getX(), sourcePos.getY());
				group.getChildren().add(ArrowHead.create(sourcePos.getX(), sourcePos.getY(), angle));
			}
		}

		addLabel(group, edge, sourcePos, targetPos);

		return group;
	}

	private void addArrowheads(Group group, Edge edge, double fromX, double fromY, double toX, double toY) {
		double angle = ArrowHead.angle(fromX, fromY, toX, toY);
		group.getChildren().add(ArrowHead.create(toX, toY, angle));

		if (edge.getDirectionality() == Directionality.BIDIRECTIONAL) {
			double reverseAngle = ArrowHead.angle(toX, toY, fromX, fromY);
			group.getChildren().add(ArrowHead.create(fromX, fromY, reverseAngle));
		}
	}

	private void addLabel(Group group, Edge edge, Point2D sourcePos, Point2D targetPos) {
		Map<String, Object> props = edge.getProperties();
		if (props == null)
			return;
		Object labelObj = props.get("label");
		if (labelObj == null)
			return;

		Label label = new Label(labelObj.toString());
		label.getStyleClass().add("graph-edge-label");
		label.setLayoutX((sourcePos.getX() + targetPos.getX()) / 2);
		label.setLayoutY((sourcePos.getY() + targetPos.getY()) / 2 - 12);
		group.getChildren().add(label);
	}

	private double[] buildPolylineCoords(Point2D source, List<Point2D> waypoints, Point2D target) {
		int count = waypoints.size() + 2;
		double[] coords = new double[count * 2];
		coords[0] = source.getX();
		coords[1] = source.getY();
		for (int i = 0; i < waypoints.size(); i++) {
			coords[(i + 1) * 2] = waypoints.get(i).getX();
			coords[(i + 1) * 2 + 1] = waypoints.get(i).getY();
		}
		coords[(count - 1) * 2] = target.getX();
		coords[(count - 1) * 2 + 1] = target.getY();
		return coords;
	}
}
