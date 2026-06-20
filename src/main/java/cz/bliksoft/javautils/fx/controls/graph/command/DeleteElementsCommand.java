package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Node;

public class DeleteElementsCommand implements IGraphCommand {

	private final Graph graph;
	private final List<Node> deletedNodes;
	private final List<Edge> deletedEdges;

	public DeleteElementsCommand(Graph graph, Set<UUID> selectedIds) {
		this.graph = graph;

		deletedNodes = new ArrayList<>();
		for (Node n : graph.getNodes()) {
			if (selectedIds.contains(n.getId()))
				deletedNodes.add(n);
		}

		Set<UUID> deletedNodeIds = new java.util.HashSet<>();
		for (Node n : deletedNodes)
			deletedNodeIds.add(n.getId());

		deletedEdges = new ArrayList<>();
		for (Edge e : graph.getEdges()) {
			if (selectedIds.contains(e.getId())) {
				deletedEdges.add(e);
			} else {
				boolean sourceOrphaned = isJoinPointOwnedBy(e.getSourceJoinPointId(), deletedNodeIds);
				boolean targetOrphaned = isJoinPointOwnedBy(e.getTargetJoinPointId(), deletedNodeIds);
				if (sourceOrphaned || targetOrphaned)
					deletedEdges.add(e);
			}
		}
	}

	private boolean isJoinPointOwnedBy(UUID joinPointId, Set<UUID> nodeIds) {
		for (Node n : graph.getNodes()) {
			if (nodeIds.contains(n.getId())) {
				if (n.getJoinPoints().stream().anyMatch(jp -> jp.getId().equals(joinPointId)))
					return true;
			}
		}
		return false;
	}

	@Override
	public void execute() {
		graph.getNodes().removeAll(deletedNodes);
		graph.getEdges().removeAll(deletedEdges);
	}

	@Override
	public void undo() {
		graph.getNodes().addAll(deletedNodes);
		graph.getEdges().addAll(deletedEdges);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public String getDescription() {
		return "Delete " + (deletedNodes.size() + deletedEdges.size()) + " element(s)";
	}
}
