package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.UUID;

import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;

public class ResizeNodeCommand implements IGraphCommand {

	private final Group graph;
	private final UUID nodeId;
	private final double oldX, oldY, oldW, oldH;
	private final double newX, newY, newW, newH;

	public ResizeNodeCommand(Group graph, UUID nodeId, double oldX, double oldY, double oldW, double oldH, double newX,
			double newY, double newW, double newH) {
		this.graph = graph;
		this.nodeId = nodeId;
		this.oldX = oldX;
		this.oldY = oldY;
		this.oldW = oldW;
		this.oldH = oldH;
		this.newX = newX;
		this.newY = newY;
		this.newW = newW;
		this.newH = newH;
	}

	@Override
	public void execute() {
		apply(newX, newY, newW, newH);
	}

	@Override
	public void undo() {
		apply(oldX, oldY, oldW, oldH);
	}

	@Override
	public void redo() {
		apply(newX, newY, newW, newH);
	}

	@Override
	public String getDescription() {
		return "Resize node";
	}

	private void apply(double x, double y, double w, double h) {
		for (Node node : graph.getNodes()) {
			if (node.getId().equals(nodeId)) {
				node.setX(x);
				node.setY(y);
				node.setWidth(w);
				node.setHeight(h);
				return;
			}
		}
	}
}
