package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;

public class MoveNodesCommand implements IGraphCommand {

	private final Group graph;
	private final Map<UUID, double[]> oldPositions;
	private final Map<UUID, double[]> newPositions;

	public MoveNodesCommand(Group graph, Map<UUID, double[]> oldPositions, Map<UUID, double[]> newPositions) {
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
		applyInGroup(graph, positions);
	}

	private void applyInGroup(Group group, Map<UUID, double[]> positions) {
		for (Node node : group.getNodes()) {
			double[] pos = positions.get(node.getId());
			if (pos != null) {
				node.setX(pos[0]);
				node.setY(pos[1]);
			}
		}
		for (Group child : group.getGroups()) {
			double[] pos = positions.get(child.getId());
			if (pos != null) {
				double dx = pos[0] - child.getX();
				double dy = pos[1] - child.getY();
				child.setX(pos[0]);
				child.setY(pos[1]);
				offsetContents(child, dx, dy);
			} else {
				applyInGroup(child, positions);
			}
		}
	}

	private void offsetContents(Group group, double dx, double dy) {
		for (Node node : group.getNodes()) {
			node.setX(node.getX() + dx);
			node.setY(node.getY() + dy);
		}
		for (Group child : group.getGroups()) {
			child.setX(child.getX() + dx);
			child.setY(child.getY() + dy);
			offsetContents(child, dx, dy);
		}
	}
}
