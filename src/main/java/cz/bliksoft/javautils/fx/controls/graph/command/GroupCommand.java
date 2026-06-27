package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder.EdgeRelink;

public class GroupCommand implements IGraphCommand {

	private final Group parent;
	private final Group group;
	private final List<Node> movedNodes;
	private final List<Edge> movedEdges;
	private final List<Edge> bridgeEdges;
	private final List<EdgeRelink> relinks;

	public GroupCommand(Group parent, Group group, List<Edge> bridgeEdges, List<EdgeRelink> relinks) {
		this.parent = parent;
		this.group = group;
		this.bridgeEdges = bridgeEdges;
		this.relinks = relinks;
		this.movedNodes = new ArrayList<>(group.getNodes());
		this.movedEdges = new ArrayList<>(group.getEdges());
	}

	@Override
	public void execute() {
	}

	@Override
	public void undo() {
		parent.getGroups().remove(group);
		for (Node node : movedNodes) {
			group.getNodes().remove(node);
			parent.getNodes().add(node);
		}
		for (Edge edge : movedEdges) {
			if (!bridgeEdges.contains(edge)) {
				group.getEdges().remove(edge);
				parent.getEdges().add(edge);
			}
		}
		for (Edge bridge : bridgeEdges)
			group.getEdges().remove(bridge);
		for (EdgeRelink r : relinks)
			applyRelink(r, false);
	}

	@Override
	public void redo() {
		for (Node node : movedNodes) {
			parent.getNodes().remove(node);
			group.getNodes().add(node);
		}
		for (Edge edge : movedEdges) {
			if (!bridgeEdges.contains(edge)) {
				parent.getEdges().remove(edge);
				group.getEdges().add(edge);
			}
		}
		for (Edge bridge : bridgeEdges)
			group.getEdges().add(bridge);
		for (EdgeRelink r : relinks)
			applyRelink(r, true);
		parent.getGroups().add(group);
	}

	@Override
	public String getDescription() {
		return "Group " + movedNodes.size() + " node(s)";
	}

	private void applyRelink(EdgeRelink r, boolean forward) {
		for (Edge edge : parent.getAllEdgesRecursive()) {
			if (edge.getId().equals(r.getEdgeId())) {
				if (r.isSource())
					edge.setSourceJoinPointId(forward ? r.getNewJoinPointId() : r.getOriginalJoinPointId());
				else
					edge.setTargetJoinPointId(forward ? r.getNewJoinPointId() : r.getOriginalJoinPointId());
				return;
			}
		}
	}

	public Group getGroup() {
		return group;
	}
}
