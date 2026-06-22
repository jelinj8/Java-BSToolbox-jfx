package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;

public class DeleteElementsCommand implements IGraphCommand {

	private final Group root;
	private final List<NodeRemoval> deletedNodes;
	private final List<EdgeRemoval> deletedEdges;

	public DeleteElementsCommand(Group root, Set<UUID> selectedIds) {
		this.root = root;

		deletedNodes = new ArrayList<>();
		for (UUID id : selectedIds) {
			Group parent = root.findParentOf(id);
			if (parent != null) {
				Node node = null;
				for (Node n : parent.getNodes()) {
					if (n.getId().equals(id)) {
						node = n;
						break;
					}
				}
				if (node != null)
					deletedNodes.add(new NodeRemoval(parent, node));
			}
		}

		Set<UUID> deletedNodeIds = new java.util.HashSet<>();
		for (NodeRemoval nr : deletedNodes)
			deletedNodeIds.add(nr.node.getId());

		deletedEdges = new ArrayList<>();
		for (UUID id : selectedIds) {
			EdgeRemoval er = findEdge(root, id);
			if (er != null)
				deletedEdges.add(er);
		}

		for (Node node : deletedNodes.stream().map(nr -> nr.node).toList()) {
			collectOrphanedEdges(root, node, deletedEdges);
		}
	}

	private static EdgeRemoval findEdge(Group group, UUID edgeId) {
		for (Edge e : group.getEdges()) {
			if (e.getId().equals(edgeId))
				return new EdgeRemoval(group, e);
		}
		for (Group child : group.getGroups()) {
			EdgeRemoval found = findEdge(child, edgeId);
			if (found != null)
				return found;
		}
		return null;
	}

	private static void collectOrphanedEdges(Group group, Node deletedNode, List<EdgeRemoval> result) {
		Set<UUID> jpIds = new java.util.HashSet<>();
		for (var jp : deletedNode.getJoinPoints())
			jpIds.add(jp.getId());

		for (Edge e : group.getEdges()) {
			if (jpIds.contains(e.getSourceJoinPointId()) || jpIds.contains(e.getTargetJoinPointId())) {
				if (result.stream().noneMatch(er -> er.edge.getId().equals(e.getId())))
					result.add(new EdgeRemoval(group, e));
			}
		}
		for (Group child : group.getGroups())
			collectOrphanedEdges(child, deletedNode, result);
	}

	@Override
	public void execute() {
		for (NodeRemoval nr : deletedNodes)
			nr.parent.getNodes().remove(nr.node);
		for (EdgeRemoval er : deletedEdges)
			er.parent.getEdges().remove(er.edge);
	}

	@Override
	public void undo() {
		for (NodeRemoval nr : deletedNodes)
			nr.parent.getNodes().add(nr.node);
		for (EdgeRemoval er : deletedEdges)
			er.parent.getEdges().add(er.edge);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public String getDescription() {
		return "Delete " + (deletedNodes.size() + deletedEdges.size()) + " element(s)";
	}

	private record NodeRemoval(Group parent, Node node) {
	}

	private record EdgeRemoval(Group parent, Edge edge) {
	}
}
