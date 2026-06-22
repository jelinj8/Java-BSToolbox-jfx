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

	public static GroupResult createFromSelection(Group parent, Set<UUID> selectedIds, String name) {
		Group group = new Group(name);

		Set<UUID> memberJpIds = new LinkedHashSet<>();
		List<Node> selectedNodes = new ArrayList<>();
		for (Node node : parent.getNodes()) {
			if (selectedIds.contains(node.getId())) {
				selectedNodes.add(node);
				for (JoinPoint jp : node.getJoinPoints())
					memberJpIds.add(jp.getId());
			}
		}

		List<Group> selectedGroups = new ArrayList<>();
		for (Group child : parent.getGroups()) {
			if (selectedIds.contains(child.getId())) {
				selectedGroups.add(child);
				for (JoinPoint jp : child.getExposedJoinPoints())
					memberJpIds.add(jp.getId());
			}
		}

		for (Node node : selectedNodes) {
			parent.getNodes().remove(node);
			group.getNodes().add(node);
		}
		for (Group child : selectedGroups) {
			parent.getGroups().remove(child);
			group.getGroups().add(child);
		}

		List<Edge> internalEdges = new ArrayList<>();
		for (Edge edge : parent.getEdges()) {
			if (memberJpIds.contains(edge.getSourceJoinPointId()) && memberJpIds.contains(edge.getTargetJoinPointId()))
				internalEdges.add(edge);
		}
		for (Edge edge : internalEdges) {
			parent.getEdges().remove(edge);
			group.getEdges().add(edge);
		}

		List<Edge> bridgeEdges = new ArrayList<>();
		List<EdgeRelink> relinks = new ArrayList<>();

		for (Edge edge : new ArrayList<>(parent.getEdges())) {
			boolean sourceInside = memberJpIds.contains(edge.getSourceJoinPointId());
			boolean targetInside = memberJpIds.contains(edge.getTargetJoinPointId());

			if (sourceInside && !targetInside) {
				UUID exposedJpId = exposeJoinPoint(parent, group, edge.getSourceJoinPointId());
				if (exposedJpId != null) {
					Edge bridge = new Edge(edge.getTypeId(), edge.getSourceJoinPointId(), exposedJpId);
					bridge.setDirectionality(edge.getDirectionality());
					bridgeEdges.add(bridge);
					group.getEdges().add(bridge);
					relinks.add(new EdgeRelink(edge.getId(), edge.getSourceJoinPointId(), exposedJpId, true));
				}
			}
			if (!sourceInside && targetInside) {
				UUID exposedJpId = exposeJoinPoint(parent, group, edge.getTargetJoinPointId());
				if (exposedJpId != null) {
					Edge bridge = new Edge(edge.getTypeId(), exposedJpId, edge.getTargetJoinPointId());
					bridge.setDirectionality(edge.getDirectionality());
					bridgeEdges.add(bridge);
					group.getEdges().add(bridge);
					relinks.add(new EdgeRelink(edge.getId(), edge.getTargetJoinPointId(), exposedJpId, false));
				}
			}
		}

		for (EdgeRelink r : relinks) {
			for (Edge edge : parent.getEdges()) {
				if (edge.getId().equals(r.getEdgeId())) {
					if (r.isSource())
						edge.setSourceJoinPointId(r.getNewJoinPointId());
					else
						edge.setTargetJoinPointId(r.getNewJoinPointId());
					break;
				}
			}
		}

		computeCollapsedBounds(parent, group, selectedIds);
		parent.getGroups().add(group);

		return new GroupResult(group, bridgeEdges, relinks);
	}

	private static UUID exposeJoinPoint(Group parent, Group group, UUID internalJpId) {
		UUID existing = findExposedJpForInternal(group, internalJpId);
		if (existing != null)
			return existing;

		JoinPoint internalJp = findJoinPointInGroup(group, internalJpId);
		if (internalJp == null)
			internalJp = findJoinPointInGroup(parent, internalJpId);
		if (internalJp == null)
			return null;

		JoinPoint exposed = new JoinPoint(internalJp.getName(), internalJp.getPosition(),
				internalJp.getDirection().toBorderDirection(), -1);
		group.getExposedJoinPoints().add(exposed);
		return exposed.getId();
	}

	public static void exposeJoinPoint(Group parent, Group group, UUID internalJpId, JoinPoint exposed) {
		group.getExposedJoinPoints().add(exposed);
		positionSingleExposedJoinPoint(group, exposed);
		ensureBridgeEdge(group, internalJpId, exposed.getId(), exposed.getDirection());
	}

	public static UUID findExposedJpForInternal(Group group, UUID internalJpId) {
		Set<UUID> exposedIds = new java.util.HashSet<>();
		for (JoinPoint jp : group.getExposedJoinPoints())
			exposedIds.add(jp.getId());
		for (Edge edge : group.getEdges()) {
			if (edge.getSourceJoinPointId().equals(internalJpId) && exposedIds.contains(edge.getTargetJoinPointId()))
				return edge.getTargetJoinPointId();
			if (edge.getTargetJoinPointId().equals(internalJpId) && exposedIds.contains(edge.getSourceJoinPointId()))
				return edge.getSourceJoinPointId();
		}
		return null;
	}

	public static UUID findInternalJpForExposed(Group group, UUID exposedJpId) {
		for (Edge edge : group.getEdges()) {
			if (edge.getSourceJoinPointId().equals(exposedJpId))
				return edge.getTargetJoinPointId();
			if (edge.getTargetJoinPointId().equals(exposedJpId))
				return edge.getSourceJoinPointId();
		}
		return null;
	}

	public static JoinPoint findJoinPointInGroup(Group group, UUID jpId) {
		for (Node node : group.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints()) {
				if (jp.getId().equals(jpId))
					return jp;
			}
		}
		for (Group child : group.getGroups()) {
			for (JoinPoint jp : child.getExposedJoinPoints()) {
				if (jp.getId().equals(jpId))
					return jp;
			}
		}
		for (JoinPoint jp : group.getExposedJoinPoints()) {
			if (jp.getId().equals(jpId))
				return jp;
		}
		return null;
	}

	public static JoinPoint findJoinPoint(Group root, UUID jpId) {
		JoinPoint found = findJoinPointInGroup(root, jpId);
		if (found != null)
			return found;
		for (Group child : root.getGroups()) {
			found = findJoinPoint(child, jpId);
			if (found != null)
				return found;
		}
		return null;
	}

	private static final double GROUP_PADDING = 20;
	private static final double GROUP_HEADER = 20;

	private static void computeCollapsedBounds(Group parent, Group group, Set<UUID> nodeIds) {
		double[] bounds = computeMemberBounds(group);
		if (bounds == null)
			return;

		group.setX(bounds[0] - GROUP_PADDING);
		group.setY(bounds[1] - GROUP_PADDING - GROUP_HEADER);
		group.setWidth(bounds[2] - bounds[0] + GROUP_PADDING * 2);
		group.setHeight(bounds[3] - bounds[1] + GROUP_PADDING * 2 + GROUP_HEADER);

		positionExposedJoinPoints(group);
	}

	public static double[] computeMemberBounds(Group group) {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

		for (Node node : group.getNodes()) {
			minX = Math.min(minX, node.getX());
			minY = Math.min(minY, node.getY());
			maxX = Math.max(maxX, node.getX() + node.getWidth());
			maxY = Math.max(maxY, node.getY() + node.getHeight());
		}

		for (Group child : group.getGroups()) {
			minX = Math.min(minX, child.getX());
			minY = Math.min(minY, child.getY());
			maxX = Math.max(maxX, child.getX() + child.getWidth());
			maxY = Math.max(maxY, child.getY() + child.getHeight());
		}

		if (minX >= Double.MAX_VALUE)
			return null;
		return new double[] { minX, minY, maxX, maxY };
	}

	public static double[] computeMinBounds(Group group) {
		double[] memberBounds = computeMemberBounds(group);
		if (memberBounds == null)
			return new double[] { group.getX(), group.getY(), 100, 80 };
		double minW = memberBounds[2] - memberBounds[0] + GROUP_PADDING * 2;
		double minH = memberBounds[3] - memberBounds[1] + GROUP_PADDING * 2 + GROUP_HEADER;
		double minX = memberBounds[0] - GROUP_PADDING;
		double minY = memberBounds[1] - GROUP_PADDING - GROUP_HEADER;
		return new double[] { minX, minY, minW, minH };
	}

	public static void positionExposedJoinPoints(Group group) {
		for (JoinPoint ejp : group.getExposedJoinPoints())
			positionSingleExposedJoinPoint(group, ejp);
		separateOverlappingJoinPoints(group);
	}

	public static void positionSingleExposedJoinPoint(Group group, JoinPoint ejp) {
		double gx = group.getX(), gy = group.getY();
		double gw = group.getWidth(), gh = group.getHeight();

		UUID internalJpId = findInternalJpForExposed(group, ejp.getId());

		double cx, cy;
		if (internalJpId != null) {
			double[] internalPos = findInternalPosition(group, internalJpId);
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
				if (!isSameEdge(a, b))
					continue;
				boolean horizontal = (a.getCustomY() == 0 || a.getCustomY() == 1);
				if (horizontal) {
					if (Math.abs(a.getCustomX() - b.getCustomX()) < minGap)
						b.setCustomX(clampFraction(a.getCustomX() + minGap));
				} else {
					if (Math.abs(a.getCustomY() - b.getCustomY()) < minGap)
						b.setCustomY(clampFraction(a.getCustomY() + minGap));
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

	private static double[] findInternalPosition(Group group, UUID internalJpId) {
		for (Node node : group.getAllNodesRecursive()) {
			for (JoinPoint jp : node.getJoinPoints()) {
				if (jp.getId().equals(internalJpId)) {
					double[] rel = cz.bliksoft.javautils.fx.controls.graph.render.JoinPointRenderer.computePosition(
							jp.getPosition(), jp.getCustomX(), jp.getCustomY(), node.getWidth(), node.getHeight());
					return new double[] { node.getX() + rel[0], node.getY() + rel[1] };
				}
			}
		}
		for (Group child : group.getAllGroupsRecursive()) {
			for (JoinPoint jp : child.getExposedJoinPoints()) {
				if (jp.getId().equals(internalJpId)) {
					double w = Math.max(child.getWidth(), 80);
					double h = Math.max(child.getHeight(), 50);
					double[] rel = cz.bliksoft.javautils.fx.controls.graph.render.JoinPointRenderer
							.computePosition(jp.getPosition(), jp.getCustomX(), jp.getCustomY(), w, h);
					return new double[] { child.getX() + rel[0], child.getY() + rel[1] };
				}
			}
		}
		return null;
	}

	public static Group findGroupContaining(Group root, UUID nodeId) {
		return root.findParentOf(nodeId);
	}

	public static Group findGroupById(Group root, UUID groupId) {
		if (root.getId().equals(groupId))
			return root;
		return root.findGroup(groupId);
	}

	public static Group findExpandedGroupAtPoint(Group root, double x, double y) {
		Group innermost = null;
		double smallestArea = Double.MAX_VALUE;
		for (Group group : root.getAllGroupsRecursive()) {
			if (group.isCollapsed())
				continue;
			if (x >= group.getX() && x <= group.getX() + group.getWidth() && y >= group.getY()
					&& y <= group.getY() + group.getHeight()) {
				double area = group.getWidth() * group.getHeight();
				if (area < smallestArea) {
					smallestArea = area;
					innermost = group;
				}
			}
		}
		return innermost;
	}

	public static void addNodeToGroup(Group group, Node node) {
		if (!group.getNodes().contains(node))
			group.getNodes().add(node);
	}

	public static void expandAncestorBounds(Group root, Group startGroup) {
		Group current = startGroup;
		while (current != null) {
			double[] minBounds = computeMinBounds(current);
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
			current = root.findParentOf(current.getId());
		}
	}

	public static void expandAllAncestors(Group root) {
		for (Group group : root.getAllGroupsRecursive()) {
			if (!group.getNodes().isEmpty() || !group.getGroups().isEmpty())
				expandAncestorBounds(root, group);
		}
	}

	public static int countBordersCrossed(Group root, UUID nodeIdA, UUID nodeIdB) {
		Group parentA = root.findParentOf(nodeIdA);
		Group parentB = root.findParentOf(nodeIdB);
		if (parentA == null || parentB == null)
			return 0;
		if (parentA.getId().equals(parentB.getId()))
			return 0;
		return 1;
	}

	public static JoinPoint getOrCreateExposedJoinPoint(Group parent, Group group, UUID internalJpId) {
		UUID existingExposedId = findExposedJpForInternal(group, internalJpId);
		if (existingExposedId != null) {
			for (JoinPoint jp : group.getExposedJoinPoints()) {
				if (jp.getId().equals(existingExposedId))
					return jp;
			}
		}

		JoinPoint internalJp = findJoinPoint(group, internalJpId);
		if (internalJp == null)
			internalJp = findJoinPointInGroup(parent, internalJpId);
		if (internalJp == null)
			return null;

		JoinPoint exposed = new JoinPoint(internalJp.getName(), internalJp.getPosition(),
				internalJp.getDirection().toBorderDirection(), -1);
		group.getExposedJoinPoints().add(exposed);
		positionSingleExposedJoinPoint(group, exposed);

		ensureBridgeEdge(group, internalJpId, exposed.getId(), internalJp.getDirection());
		return exposed;
	}

	private static void ensureBridgeEdge(Group group, UUID internalJpId, UUID exposedJpId,
			Direction internalDirection) {
		for (Edge edge : group.getEdges()) {
			if ((edge.getSourceJoinPointId().equals(internalJpId) && edge.getTargetJoinPointId().equals(exposedJpId))
					|| (edge.getSourceJoinPointId().equals(exposedJpId)
							&& edge.getTargetJoinPointId().equals(internalJpId)))
				return;
		}
		Edge bridge = new Edge("default", internalDirection.isOutgoing() ? internalJpId : exposedJpId,
				internalDirection.isOutgoing() ? exposedJpId : internalJpId);
		group.getEdges().add(bridge);
	}

	public static void removeExposedJoinPoint(Group parent, Group group, UUID exposedJpId) {
		group.getExposedJoinPoints().removeIf(jp -> jp.getId().equals(exposedJpId));
		group.getEdges().removeIf(
				e -> e.getSourceJoinPointId().equals(exposedJpId) || e.getTargetJoinPointId().equals(exposedJpId));
		parent.getEdges().removeIf(
				e -> e.getSourceJoinPointId().equals(exposedJpId) || e.getTargetJoinPointId().equals(exposedJpId));
	}

	public static boolean hasConnections(Group root, UUID jpId) {
		for (Edge e : root.getAllEdgesRecursive()) {
			if (e.getSourceJoinPointId().equals(jpId) || e.getTargetJoinPointId().equals(jpId))
				return true;
		}
		return false;
	}
}
