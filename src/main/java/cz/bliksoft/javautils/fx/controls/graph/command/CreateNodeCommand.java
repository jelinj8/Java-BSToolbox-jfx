package cz.bliksoft.javautils.fx.controls.graph.command;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Node;

public class CreateNodeCommand implements IGraphCommand {

	private final Graph graph;
	private final Node node;

	public CreateNodeCommand(Graph graph, Node node) {
		this.graph = graph;
		this.node = node;
	}

	@Override
	public void execute() {
		graph.getNodes().add(node);
	}

	@Override
	public void undo() {
		graph.getNodes().remove(node);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public String getDescription() {
		return "Create node";
	}

	public Node getNode() {
		return node;
	}
}
