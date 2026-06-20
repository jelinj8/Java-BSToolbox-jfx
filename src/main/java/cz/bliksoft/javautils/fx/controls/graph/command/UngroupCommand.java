package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.JoinPointMapping;

public class UngroupCommand implements IGraphCommand {

	private final Graph graph;
	private final Group group;
	private final List<Edge> bridgeEdges;
	private final List<ExternalRelink> externalRelinks;
	private final Group parentGroup;

	public UngroupCommand(Graph graph, Group group) {
		this.graph = graph;
		this.group = group;

		bridgeEdges = new ArrayList<>();
		externalRelinks = new ArrayList<>();

		for (Edge edge : graph.getEdges()) {
			if (group.getMemberEdgeIds().contains(edge.getId())) {
				boolean srcIsExposed = isExposedJoinPoint(edge.getSourceJoinPointId());
				boolean tgtIsExposed = isExposedJoinPoint(edge.getTargetJoinPointId());
				if (srcIsExposed || tgtIsExposed)
					bridgeEdges.add(edge);
			}
		}

		for (JoinPointMapping mapping : group.getJoinPointMappings()) {
			for (Edge edge : graph.getEdges()) {
				if (group.getMemberEdgeIds().contains(edge.getId()))
					continue;
				if (edge.getSourceJoinPointId().equals(mapping.getExposedId())) {
					externalRelinks.add(
							new ExternalRelink(edge.getId(), mapping.getExposedId(), mapping.getInternalId(), true));
				}
				if (edge.getTargetJoinPointId().equals(mapping.getExposedId())) {
					externalRelinks.add(
							new ExternalRelink(edge.getId(), mapping.getExposedId(), mapping.getInternalId(), false));
				}
			}
		}

		parentGroup = findParentGroup();
	}

	@Override
	public void execute() {
		for (ExternalRelink r : externalRelinks) {
			for (Edge edge : graph.getEdges()) {
				if (edge.getId().equals(r.edgeId)) {
					if (r.isSource)
						edge.setSourceJoinPointId(r.internalId);
					else
						edge.setTargetJoinPointId(r.internalId);
				}
			}
		}
		graph.getEdges().removeAll(bridgeEdges);

		if (parentGroup != null) {
			parentGroup.getMemberGroupIds().remove(group.getId());
			parentGroup.getMemberNodeIds().addAll(group.getMemberNodeIds());
		}

		graph.getGroups().remove(group);
	}

	@Override
	public void undo() {
		graph.getGroups().add(group);

		if (parentGroup != null) {
			parentGroup.getMemberNodeIds().removeAll(group.getMemberNodeIds());
			parentGroup.getMemberGroupIds().add(group.getId());
		}

		graph.getEdges().addAll(bridgeEdges);
		for (ExternalRelink r : externalRelinks) {
			for (Edge edge : graph.getEdges()) {
				if (edge.getId().equals(r.edgeId)) {
					if (r.isSource)
						edge.setSourceJoinPointId(r.exposedId);
					else
						edge.setTargetJoinPointId(r.exposedId);
				}
			}
		}
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public String getDescription() {
		return "Ungroup '" + group.getName() + "'";
	}

	private boolean isExposedJoinPoint(java.util.UUID jpId) {
		return group.getExposedJoinPoints().stream().anyMatch(jp -> jp.getId().equals(jpId));
	}

	private Group findParentGroup() {
		for (Group g : graph.getGroups()) {
			if (g.getMemberGroupIds().contains(group.getId()))
				return g;
		}
		return null;
	}

	private record ExternalRelink(java.util.UUID edgeId, java.util.UUID exposedId, java.util.UUID internalId,
			boolean isSource) {
	}
}
