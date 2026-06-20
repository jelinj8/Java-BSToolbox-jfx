package cz.bliksoft.javautils.fx.controls.graph.render;

import cz.bliksoft.dataflow.types.flowchart.FlowchartEdgeTypes;
import cz.bliksoft.dataflow.types.flowchart.FlowchartNodeTypes;

public abstract class FlowchartRenderers {

	public static void registerAll() {
		registerAll(NodeRendererRegistry.getInstance(), EdgeRendererRegistry.getInstance());
	}

	public static void registerAll(NodeRendererRegistry nodeRegistry, EdgeRendererRegistry edgeRegistry) {
		ShapeNodeRenderer shapeRenderer = new ShapeNodeRenderer();
		nodeRegistry.setFallback(shapeRenderer);
		FlowchartNodeTypes.ALL.forEach(nt -> nodeRegistry.register(nt.getTypeId(), shapeRenderer));

		StraightLineEdgeRenderer straightRenderer = new StraightLineEdgeRenderer();
		edgeRegistry.setFallback(straightRenderer);
		FlowchartEdgeTypes.ALL.forEach(et -> edgeRegistry.register(et.getTypeId(), straightRenderer));
	}
}
