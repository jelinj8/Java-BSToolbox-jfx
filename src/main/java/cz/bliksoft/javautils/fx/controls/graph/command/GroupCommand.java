package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.List;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder.EdgeRelink;

public class GroupCommand implements IGraphCommand {

	private final Graph graph;
	private final Group group;
	private final List<Edge> bridgeEdges;
	private final List<EdgeRelink> relinks;
	private final Group parentGroup;

	public GroupCommand(Graph graph, Group group, List<Edge> bridgeEdges, List<EdgeRelink> relinks) {
		this.graph = graph;
		this.group = group;
		this.bridgeEdges = bridgeEdges;
		this.relinks = relinks;

		UUID anyMember = group.getMemberNodeIds().isEmpty() ? null : group.getMemberNodeIds().iterator().next();
		this.parentGroup = anyMember != null ? GroupBuilder.findGroupContaining(graph, anyMember) : null;
	}

	@Override
	public void execute() {
		graph.getGroups().add(group);
		graph.getEdges().addAll(bridgeEdges);
		for (EdgeRelink r : relinks)
			applyRelink(r, true);

		if (parentGroup != null) {
			parentGroup.getMemberNodeIds().removeAll(group.getMemberNodeIds());
			parentGroup.getMemberGroupIds().add(group.getId());
		}
	}

	@Override
	public void undo() {
		if (parentGroup != null) {
			parentGroup.getMemberGroupIds().remove(group.getId());
			parentGroup.getMemberNodeIds().addAll(group.getMemberNodeIds());
		}

		for (EdgeRelink r : relinks)
			applyRelink(r, false);
		graph.getEdges().removeAll(bridgeEdges);
		graph.getGroups().remove(group);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public String getDescription() {
		return "Group " + group.getMemberNodeIds().size() + " node(s)";
	}

	private void applyRelink(EdgeRelink r, boolean forward) {
		for (Edge edge : graph.getEdges()) {
			if (edge.getId().equals(r.getEdgeId())) {
				if (r.isSource()) {
					edge.setSourceJoinPointId(forward ? r.getNewJoinPointId() : r.getOriginalJoinPointId());
				} else {
					edge.setTargetJoinPointId(forward ? r.getNewJoinPointId() : r.getOriginalJoinPointId());
				}
				return;
			}
		}
	}

	public Group getGroup() {
		return group;
	}
}
