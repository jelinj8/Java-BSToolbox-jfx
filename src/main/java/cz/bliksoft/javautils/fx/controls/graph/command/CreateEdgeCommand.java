package cz.bliksoft.javautils.fx.controls.graph.command;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;

public class CreateEdgeCommand implements IGraphCommand {

	private final Graph graph;
	private final Edge edge;

	public CreateEdgeCommand(Graph graph, Edge edge) {
		this.graph = graph;
		this.edge = edge;
	}

	@Override
	public void execute() {
		graph.getEdges().add(edge);
	}

	@Override
	public void undo() {
		graph.getEdges().remove(edge);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public String getDescription() {
		return "Create connection";
	}
}
