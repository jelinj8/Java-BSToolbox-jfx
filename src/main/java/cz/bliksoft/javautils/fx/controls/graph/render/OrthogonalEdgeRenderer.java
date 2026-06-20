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
import javafx.scene.shape.Polyline;

public class OrthogonalEdgeRenderer implements IEdgeRenderer {

	@Override
	public Group createEdgeVisual(Edge edge, EdgeType type, Point2D sourcePos, Point2D targetPos,
			List<Point2D> waypoints, RenderContext ctx) {

		Group group = new Group();
		group.getStyleClass().add("graph-edge");
		group.getProperties().put("edgeId", edge.getId());

		double sx = sourcePos.getX(), sy = sourcePos.getY();
		double tx = targetPos.getX(), ty = targetPos.getY();

		double midX = (sx + tx) / 2;

		Polyline polyline = new Polyline(sx, sy, midX, sy, midX, ty, tx, ty);
		polyline.setStroke(Color.web("#555555"));
		polyline.setStrokeWidth(1.5);
		polyline.setFill(Color.TRANSPARENT);
		polyline.getStyleClass().add("graph-edge-line");
		group.getChildren().add(polyline);

		double angle = ArrowHead.angle(midX, ty, tx, ty);
		group.getChildren().add(ArrowHead.create(tx, ty, angle));

		if (edge.getDirectionality() == Directionality.BIDIRECTIONAL) {
			double reverseAngle = ArrowHead.angle(midX, sy, sx, sy);
			group.getChildren().add(ArrowHead.create(sx, sy, reverseAngle));
		}

		Map<String, Object> props = edge.getProperties();
		if (props != null) {
			Object labelObj = props.get("label");
			if (labelObj != null) {
				Label label = new Label(labelObj.toString());
				label.getStyleClass().add("graph-edge-label");
				label.setLayoutX(midX + 4);
				label.setLayoutY((sy + ty) / 2 - 12);
				group.getChildren().add(label);
			}
		}

		return group;
	}
}
