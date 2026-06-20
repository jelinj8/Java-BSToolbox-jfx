package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;

public class MoveNodesCommand implements IGraphCommand {

	private final Graph graph;
	private final Map<UUID, double[]> oldPositions;
	private final Map<UUID, double[]> newPositions;

	public MoveNodesCommand(Graph graph, Map<UUID, double[]> oldPositions, Map<UUID, double[]> newPositions) {
		this.graph = graph;
		this.oldPositions = new LinkedHashMap<>(oldPositions);
		this.newPositions = new LinkedHashMap<>(newPositions);
	}

	@Override
	public void execute() {
		applyPositions(newPositions);
	}

	@Override
	public void undo() {
		applyPositions(oldPositions);
	}

	@Override
	public void redo() {
		applyPositions(newPositions);
	}

	@Override
	public String getDescription() {
		return "Move " + newPositions.size() + " element(s)";
	}

	private void applyPositions(Map<UUID, double[]> positions) {
		for (Node node : graph.getNodes()) {
			double[] pos = positions.get(node.getId());
			if (pos != null) {
				node.setX(pos[0]);
				node.setY(pos[1]);
			}
		}
		for (Group group : graph.getGroups()) {
			double[] pos = positions.get(group.getId());
			if (pos != null) {
				group.setX(pos[0]);
				group.setY(pos[1]);
			}
		}
	}
}
