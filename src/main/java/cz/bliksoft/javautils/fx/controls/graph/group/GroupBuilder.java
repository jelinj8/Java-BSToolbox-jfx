package cz.bliksoft.javautils.fx.controls.graph.group;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Direction;
import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.JoinPointMapping;
import cz.bliksoft.dataflow.model.JoinPointPosition;
import cz.bliksoft.dataflow.model.Node;

public abstract class GroupBuilder {

	public static class GroupResult {

		private final Group group;
		private final List<Edge> bridgeEdges;
		private final List<EdgeRelink> relinks;

		GroupResult(Group group, List<Edge> bridgeEdges, List<EdgeRelink> relinks) {
			this.group = group;
			this.bridgeEdges = bridgeEdges;
			this.relinks = relinks;
		}

		public Group getGroup() {
			return group;
		}

		public List<Edge> getBridgeEdges() {
			return bridgeEdges;
		}

		public List<EdgeRelink> getRelinks() {
			return relinks;
		}
	}

	public static class EdgeRelink {

		private final UUID edgeId;
		private final UUID originalJoinPointId;
		private final UUID newJoinPointId;
		private final boolean isSource;

		EdgeRelink(UUID edgeId, UUID originalJoinPointId, UUID newJoinPointId, boolean isSource) {
			this.edgeId = edgeId;
			this.originalJoinPointId = originalJoinPointId;
			this.newJoinPointId = newJoinPointId;
			this.isSource = isSource;
		}

		public UUID getEdgeId() {
			return edgeId;
		}

		public UUID getOriginalJoinPointId() {
			return originalJoinPointId;
		}

		public UUID getNewJoinPointId() {
			return newJoinPointId;
		}

		public boolean isSource() {
			return isSource;
		}
	}

	public static GroupResult createFromSelection(Graph graph, Set<UUID> selectedNodeIds, String name) {
		Group group = new Group(name);
		group.setMemberNodeIds(new LinkedHashSet<>(selectedNodeIds));

		Set<UUID> memberJpIds = new LinkedHashSet<>();
		for (Node node : graph.getNodes()) {
			if (selectedNodeIds.contains(node.getId())) {
				for (JoinPoint jp : node.getJoinPoints())
					memberJpIds.add(jp.getId());
			}
		}

		Set<UUID> internalEdgeIds = new LinkedHashSet<>();
		for (Edge edge : graph.getEdges()) {
			boolean sourceInside = memberJpIds.contains(edge.getSourceJoinPointId());
			boolean targetInside = memberJpIds.contains(edge.getTargetJoinPointId());
			if (sourceInside && targetInside)
				internalEdgeIds.add(edge.getId());
		}
		group.setMemberEdgeIds(internalEdgeIds);

		List<Edge> bridgeEdges = new ArrayList<>();
		List<EdgeRelink> relinks = new ArrayList<>();

		for (Edge edge : graph.getEdges()) {
			boolean sourceInside = memberJpIds.contains(edge.getSourceJoinPointId());
			boolean targetInside = memberJpIds.contains(edge.getTargetJoinPointId());

			if (sourceInside && !targetInside) {
				UUID exposedJpId = exposeJoinPoint(graph, group, edge.getSourceJoinPointId());
				if (exposedJpId != null) {
					Edge bridge = new Edge(edge.getTypeId(), edge.getSourceJoinPointId(), exposedJpId);
					bridge.setDirectionality(edge.getDirectionality());
					bridgeEdges.add(bridge);
					group.getMemberEdgeIds().add(bridge.getId());

					relinks.add(new EdgeRelink(edge.getId(), edge.getSourceJoinPointId(), exposedJpId, true));
				}
			}
			if (!sourceInside && targetInside) {
				UUID exposedJpId = exposeJoinPoint(graph, group, edge.getTargetJoinPointId());
				if (exposedJpId != null) {
					Edge bridge = new Edge(edge.getTypeId(), exposedJpId, edge.getTargetJoinPointId());
					bridge.setDirectionality(edge.getDirectionality());
					bridgeEdges.add(bridge);
					group.getMemberEdgeIds().add(bridge.getId());

					relinks.add(new EdgeRelink(edge.getId(), edge.getTargetJoinPointId(), exposedJpId, false));
				}
			}
		}

		computeCollapsedBounds(graph, group, selectedNodeIds);

		return new GroupResult(group, bridgeEdges, relinks);
	}

	private static UUID exposeJoinPoint(Graph graph, Group group, UUID internalJpId) {
		for (JoinPointMapping existing : group.getJoinPointMappings()) {
			if (existing.getInternalId().equals(internalJpId))
				return existing.getExposedId();
		}

		JoinPoint internalJp = findJoinPoint(graph, internalJpId);
		if (internalJp == null)
			return null;

		JoinPoint exposed = new JoinPoint(internalJp.getName(), internalJp.getPosition(),
				internalJp.getDirection().toBorderDirection(), -1);
		group.getExposedJoinPoints().add(exposed);
		group.getJoinPointMappings().add(new JoinPointMapping(exposed.getId(), internalJpId));
		return exposed.getId();
	}

	public static void exposeJoinPoint(Graph graph, Group group, UUID internalJpId, JoinPoint exposed) {
		group.getExposedJoinPoints().add(exposed);
		group.getJoinPointMappings().add(new JoinPointMapping(exposed.getId(), internalJpId));
	}

	public static void unexposeJoinPoint(Graph graph, Group group, UUID exposedJpId) {
		group.getExposedJoinPoints().removeIf(jp -> jp.getId().equals(exposedJpId));
		group.getJoinPointMappings().removeIf(m -> m.getExposedId().equals(exposedJpId));

		graph.getEdges().removeIf(
				e -> e.getSourceJoinPointId().equals(exposedJpId) || e.getTargetJoinPointId().equals(exposedJpId));
	}

	public static JoinPoint findJoinPoint(Graph graph, UUID jpId) {
		for (Node node : graph.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints()) {
				if (jp.getId().equals(jpId))
					return jp;
			}
		}
		for (Group group : graph.getGroups()) {
			for (JoinPoint jp : group.getExposedJoinPoints()) {
				if (jp.getId().equals(jpId))
					return jp;
			}
		}
		return null;
	}

	private static final double GROUP_PADDING = 20;
	private static final double GROUP_HEADER = 20;

	private static void computeCollapsedBounds(Graph graph, Group group, Set<UUID> nodeIds) {
		double[] bounds = computeMemberBounds(graph, nodeIds);
		if (bounds == null)
			return;

		group.setX(bounds[0] - GROUP_PADDING);
		group.setY(bounds[1] - GROUP_PADDING - GROUP_HEADER);
		group.setWidth(bounds[2] - bounds[0] + GROUP_PADDING * 2);
		group.setHeight(bounds[3] - bounds[1] + GROUP_PADDING * 2 + GROUP_HEADER);

		positionExposedJoinPoints(graph, group);
	}

	static double[] computeMemberBounds(Graph graph, Set<UUID> nodeIds) {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

		for (Node node : graph.getNodes()) {
			if (!nodeIds.contains(node.getId()))
				continue;
			minX = Math.min(minX, node.getX());
			minY = Math.min(minY, node.getY());
			maxX = Math.max(maxX, node.getX() + node.getWidth());
			maxY = Math.max(maxY, node.getY() + node.getHeight());
		}

		if (minX >= Double.MAX_VALUE)
			return null;
		return new double[] { minX, minY, maxX, maxY };
	}

	static double[] computeFullBounds(Graph graph, Group group) {
		double[] nodeBounds = computeMemberBounds(graph, group.getMemberNodeIds());
		double minX = nodeBounds != null ? nodeBounds[0] : Double.MAX_VALUE;
		double minY = nodeBounds != null ? nodeBounds[1] : Double.MAX_VALUE;
		double maxX = nodeBounds != null ? nodeBounds[2] : Double.MIN_VALUE;
		double maxY = nodeBounds != null ? nodeBounds[3] : Double.MIN_VALUE;

		for (UUID childId : group.getMemberGroupIds()) {
			Group child = findGroupById(graph, childId);
			if (child != null) {
				minX = Math.min(minX, child.getX());
				minY = Math.min(minY, child.getY());
				maxX = Math.max(maxX, child.getX() + child.getWidth());
				maxY = Math.max(maxY, child.getY() + child.getHeight());
			}
		}

		if (minX >= Double.MAX_VALUE)
			return null;
		return new double[] { minX, minY, maxX, maxY };
	}

	public static double[] computeMinBounds(Graph graph, Group group) {
		double[] memberBounds = computeFullBounds(graph, group);
		if (memberBounds == null)
			return new double[] { group.getX(), group.getY(), 100, 80 };
		double minW = memberBounds[2] - memberBounds[0] + GROUP_PADDING * 2;
		double minH = memberBounds[3] - memberBounds[1] + GROUP_PADDING * 2 + GROUP_HEADER;
		double minX = memberBounds[0] - GROUP_PADDING;
		double minY = memberBounds[1] - GROUP_PADDING - GROUP_HEADER;
		return new double[] { minX, minY, minW, minH };
	}

	public static void positionExposedJoinPoints(Graph graph, Group group) {
		for (JoinPoint ejp : group.getExposedJoinPoints())
			positionSingleExposedJoinPoint(graph, group, ejp);
		separateOverlappingJoinPoints(group);
	}

	public static void positionSingleExposedJoinPoint(Graph graph, Group group, JoinPoint ejp) {
		double gx = group.getX(), gy = group.getY();
		double gw = group.getWidth(), gh = group.getHeight();

		JoinPointMapping mapping = group.getJoinPointMappings().stream()
				.filter(m -> m.getExposedId().equals(ejp.getId())).findFirst().orElse(null);

		double cx, cy;
		if (mapping != null) {
			double[] internalPos = findInternalPosition(graph, mapping.getInternalId());
			cx = internalPos != null ? internalPos[0] : gx + gw / 2;
			cy = internalPos != null ? internalPos[1] : gy + gh / 2;
		} else {
			cx = gx + gw / 2;
			cy = gy + gh / 2;
		}

		double dTop = Math.abs(cy - gy);
		double dBottom = Math.abs(cy - (gy + gh));
		double dLeft = Math.abs(cx - gx);
		double dRight = Math.abs(cx - (gx + gw));
		double min = Math.min(Math.min(dTop, dBottom), Math.min(dLeft, dRight));

		ejp.setPosition(JoinPointPosition.CUSTOM);

		if (min == dLeft) {
			ejp.setCustomX(0);
			ejp.setCustomY(clampFraction((cy - gy) / gh));
		} else if (min == dRight) {
			ejp.setCustomX(1);
			ejp.setCustomY(clampFraction((cy - gy) / gh));
		} else if (min == dTop) {
			ejp.setCustomX(clampFraction((cx - gx) / gw));
			ejp.setCustomY(0);
		} else {
			ejp.setCustomX(clampFraction((cx - gx) / gw));
			ejp.setCustomY(1);
		}
	}

	private static double clampFraction(double v) {
		return Math.max(0.05, Math.min(0.95, v));
	}

	private static void separateOverlappingJoinPoints(Group group) {
		java.util.List<JoinPoint> jps = group.getExposedJoinPoints();
		double minGap = 0.08;

		for (int i = 0; i < jps.size(); i++) {
			for (int j = i + 1; j < jps.size(); j++) {
				JoinPoint a = jps.get(i), b = jps.get(j);
				boolean sameEdge = isSameEdge(a, b);
				if (!sameEdge)
					continue;
				boolean horizontal = (a.getCustomY() == 0 || a.getCustomY() == 1);
				if (horizontal) {
					if (Math.abs(a.getCustomX() - b.getCustomX()) < minGap) {
						b.setCustomX(clampFraction(a.getCustomX() + minGap));
					}
				} else {
					if (Math.abs(a.getCustomY() - b.getCustomY()) < minGap) {
						b.setCustomY(clampFraction(a.getCustomY() + minGap));
					}
				}
			}
		}
	}

	private static boolean isSameEdge(JoinPoint a, JoinPoint b) {
		if (a.getCustomX() == 0 && b.getCustomX() == 0)
			return true;
		if (a.getCustomX() == 1 && b.getCustomX() == 1)
			return true;
		if (a.getCustomY() == 0 && b.getCustomY() == 0)
			return true;
		return a.getCustomY() == 1 && b.getCustomY() == 1;
	}

	private static double[] findInternalPosition(Graph graph, UUID internalJpId) {
		for (Node node : graph.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints()) {
				if (jp.getId().equals(internalJpId)) {
					double[] rel = cz.bliksoft.javautils.fx.controls.graph.render.JoinPointRenderer.computePosition(
							jp.getPosition(), jp.getCustomX(), jp.getCustomY(), node.getWidth(), node.getHeight());
					return new double[] { node.getX() + rel[0], node.getY() + rel[1] };
				}
			}
		}
		return null;
	}

	public static Group findGroupContaining(Graph graph, UUID nodeId) {
		for (Group group : graph.getGroups()) {
			if (group.getMemberNodeIds().contains(nodeId))
				return group;
		}
		return null;
	}

	public static int countBordersCrossed(Graph graph, UUID nodeIdA, UUID nodeIdB) {
		java.util.List<UUID> groupsA = findAllContainingGroups(graph, nodeIdA);
		java.util.List<UUID> groupsB = findAllContainingGroups(graph, nodeIdB);

		int shared = 0;
		for (UUID gid : groupsA) {
			if (groupsB.contains(gid))
				shared++;
		}
		return (groupsA.size() - shared) + (groupsB.size() - shared);
	}

	private static java.util.List<UUID> findAllContainingGroups(Graph graph, UUID nodeId) {
		java.util.List<UUID> result = new java.util.ArrayList<>();
		Group direct = findGroupContaining(graph, nodeId);
		if (direct == null)
			return result;
		result.add(direct.getId());
		addAncestorGroups(graph, direct.getId(), result);
		return result;
	}

	private static void addAncestorGroups(Graph graph, UUID groupId, java.util.List<UUID> result) {
		for (Group group : graph.getGroups()) {
			if (group.getMemberGroupIds().contains(groupId)) {
				result.add(group.getId());
				addAncestorGroups(graph, group.getId(), result);
				return;
			}
		}
	}

	public static Group findGroupById(Graph graph, UUID groupId) {
		return graph.getGroups().stream().filter(g -> g.getId().equals(groupId)).findFirst().orElse(null);
	}

	public static Group findExpandedGroupAtPoint(Graph graph, double x, double y) {
		for (Group group : graph.getGroups()) {
			if (group.isCollapsed())
				continue;
			if (x >= group.getX() && x <= group.getX() + group.getWidth() && y >= group.getY()
					&& y <= group.getY() + group.getHeight())
				return group;
		}
		return null;
	}

	public static void addNodeToGroup(Group group, Node node) {
		group.getMemberNodeIds().add(node.getId());
	}

	public static void expandAncestorBounds(Graph graph, Group startGroup) {
		Group current = startGroup;
		while (current != null) {
			double[] minBounds = computeMinBounds(graph, current);
			if (minBounds == null)
				break;
			boolean changed = false;
			if (current.getX() > minBounds[0]) {
				current.setWidth(current.getWidth() + (current.getX() - minBounds[0]));
				current.setX(minBounds[0]);
				changed = true;
			}
			if (current.getY() > minBounds[1]) {
				current.setHeight(current.getHeight() + (current.getY() - minBounds[1]));
				current.setY(minBounds[1]);
				changed = true;
			}
			double minRight = minBounds[0] + minBounds[2];
			if (current.getX() + current.getWidth() < minRight) {
				current.setWidth(minRight - current.getX());
				changed = true;
			}
			double minBottom = minBounds[1] + minBounds[3];
			if (current.getY() + current.getHeight() < minBottom) {
				current.setHeight(minBottom - current.getY());
				changed = true;
			}
			if (!changed)
				break;
			current = findParentGroup(graph, current.getId());
		}
	}

	public static void expandAllAncestors(Graph graph) {
		for (Group group : graph.getGroups()) {
			if (!group.getMemberGroupIds().isEmpty() || !group.getMemberNodeIds().isEmpty())
				expandAncestorBounds(graph, group);
		}
	}

	private static Group findParentGroup(Graph graph, UUID groupId) {
		for (Group g : graph.getGroups()) {
			if (g.getMemberGroupIds().contains(groupId))
				return g;
		}
		return null;
	}

	public static JoinPoint getOrCreateExposedJoinPoint(Graph graph, Group group, UUID internalJpId) {
		JoinPoint exposed = null;

		for (JoinPointMapping m : group.getJoinPointMappings()) {
			if (m.getInternalId().equals(internalJpId)) {
				for (JoinPoint jp : group.getExposedJoinPoints()) {
					if (jp.getId().equals(m.getExposedId())) {
						exposed = jp;
						break;
					}
				}
			}
		}

		JoinPoint internalJp = findJoinPoint(graph, internalJpId);
		if (internalJp == null)
			return exposed;

		if (exposed == null) {
			exposed = new JoinPoint(internalJp.getName(), internalJp.getPosition(),
					internalJp.getDirection().toBorderDirection(), -1);
			group.getExposedJoinPoints().add(exposed);
			group.getJoinPointMappings().add(new JoinPointMapping(exposed.getId(), internalJpId));
			positionSingleExposedJoinPoint(graph, group, exposed);
		}

		ensureBridgeEdge(graph, group, internalJpId, exposed.getId(), internalJp.getDirection());
		return exposed;
	}

	private static void ensureBridgeEdge(Graph graph, Group group, UUID internalJpId, UUID exposedJpId,
			Direction internalDirection) {
		for (Edge edge : graph.getEdges()) {
			if ((edge.getSourceJoinPointId().equals(internalJpId) && edge.getTargetJoinPointId().equals(exposedJpId))
					|| (edge.getSourceJoinPointId().equals(exposedJpId)
							&& edge.getTargetJoinPointId().equals(internalJpId)))
				return;
		}

		Edge bridge = new Edge("default", internalDirection.isOutgoing() ? internalJpId : exposedJpId,
				internalDirection.isOutgoing() ? exposedJpId : internalJpId);
		graph.getEdges().add(bridge);
		group.getMemberEdgeIds().add(bridge.getId());
	}

	public static void removeExposedJoinPoint(Graph graph, Group group, UUID exposedJpId) {
		group.getExposedJoinPoints().removeIf(jp -> jp.getId().equals(exposedJpId));
		group.getJoinPointMappings().removeIf(m -> m.getExposedId().equals(exposedJpId));
		graph.getEdges().removeIf(
				e -> e.getSourceJoinPointId().equals(exposedJpId) || e.getTargetJoinPointId().equals(exposedJpId));
		group.getMemberEdgeIds().removeIf(id -> {
			for (Edge e : graph.getEdges()) {
				if (e.getId().equals(id))
					return false;
			}
			return true;
		});
	}

	public static boolean hasConnections(Graph graph, UUID jpId) {
		for (Edge e : graph.getEdges()) {
			if (e.getSourceJoinPointId().equals(jpId) || e.getTargetJoinPointId().equals(jpId))
				return true;
		}
		return false;
	}
}
