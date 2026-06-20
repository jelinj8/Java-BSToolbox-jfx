package cz.bliksoft.javautils.fx.controls.graph.render;

import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.types.NodeType;
import javafx.scene.layout.Region;

public interface INodeRenderer {

	Region createNodeVisual(Node node, NodeType type, RenderContext ctx);
}
