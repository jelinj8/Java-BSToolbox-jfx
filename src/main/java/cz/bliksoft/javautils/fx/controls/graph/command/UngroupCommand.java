package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder;

public class UngroupCommand implements IGraphCommand {

	private final Group parent;
	private final Group group;
	private final List<Node> movedNodes;
	private final List<Edge> movedEdges;
	private final List<Group> movedGroups;
	private final List<Edge> bridgeEdges;
	private final List<ExternalRelink> externalRelinks;

	public UngroupCommand(Group parent, Group group) {
		this.parent = parent;
		this.group = group;

		movedNodes = new ArrayList<>(group.getNodes());
		movedEdges = new ArrayList<>(group.getEdges());
		movedGroups = new ArrayList<>(group.getGroups());

		bridgeEdges = new ArrayList<>();
		for (Edge edge : group.getEdges()) {
			boolean srcExposed = isExposedJoinPoint(edge.getSourceJoinPointId());
			boolean tgtExposed = isExposedJoinPoint(edge.getTargetJoinPointId());
			if (srcExposed || tgtExposed)
				bridgeEdges.add(edge);
		}

		externalRelinks = new ArrayList<>();
		for (cz.bliksoft.dataflow.model.JoinPoint ejp : group.getExposedJoinPoints()) {
			java.util.UUID internalId = GroupBuilder.findInternalJpForExposed(group, ejp.getId());
			if (internalId == null)
				continue;
			for (Edge edge : parent.getEdges()) {
				if (edge.getSourceJoinPointId().equals(ejp.getId()))
					externalRelinks.add(new ExternalRelink(edge.getId(), ejp.getId(), internalId, true));
				if (edge.getTargetJoinPointId().equals(ejp.getId()))
					externalRelinks.add(new ExternalRelink(edge.getId(), ejp.getId(), internalId, false));
			}
		}
	}

	@Override
	public void execute() {
		for (ExternalRelink r : externalRelinks) {
			for (Edge edge : parent.getEdges()) {
				if (edge.getId().equals(r.edgeId)) {
					if (r.isSource)
						edge.setSourceJoinPointId(r.internalId);
					else
						edge.setTargetJoinPointId(r.internalId);
				}
			}
		}

		for (Node node : movedNodes) {
			group.getNodes().remove(node);
			parent.getNodes().add(node);
		}
		for (Edge edge : movedEdges) {
			group.getEdges().remove(edge);
			if (!bridgeEdges.contains(edge))
				parent.getEdges().add(edge);
		}
		for (Group child : movedGroups) {
			group.getGroups().remove(child);
			parent.getGroups().add(child);
		}

		parent.getGroups().remove(group);
	}

	@Override
	public void undo() {
		parent.getGroups().add(group);

		for (Node node : movedNodes) {
			parent.getNodes().remove(node);
			group.getNodes().add(node);
		}
		for (Edge edge : movedEdges) {
			if (!bridgeEdges.contains(edge))
				parent.getEdges().remove(edge);
			group.getEdges().add(edge);
		}
		for (Group child : movedGroups) {
			parent.getGroups().remove(child);
			group.getGroups().add(child);
		}

		for (ExternalRelink r : externalRelinks) {
			for (Edge edge : parent.getEdges()) {
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

	private record ExternalRelink(java.util.UUID edgeId, java.util.UUID exposedId, java.util.UUID internalId,
			boolean isSource) {
	}
}
