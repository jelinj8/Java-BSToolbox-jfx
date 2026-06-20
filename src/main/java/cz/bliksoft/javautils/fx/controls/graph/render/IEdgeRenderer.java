package cz.bliksoft.javautils.fx.controls.graph.render;

import java.util.List;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Point2D;
import cz.bliksoft.dataflow.types.EdgeType;
import javafx.scene.Group;

public interface IEdgeRenderer {

	Group createEdgeVisual(Edge edge, EdgeType type, Point2D sourcePos, Point2D targetPos, List<Point2D> waypoints,
			RenderContext ctx);
}
