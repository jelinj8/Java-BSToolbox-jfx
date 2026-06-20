package cz.bliksoft.javautils.fx.controls.graph.group;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import cz.bliksoft.dataflow.model.Direction;
import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.JoinPointPosition;
import cz.bliksoft.dataflow.model.Node;

class GroupBuilderTest {

	@Test
	void createGroupWithInternalEdge() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Node n2 = createNode(graph, 200, 0);
		JoinPoint jp1 = addJoinPoint(n1, "out", Direction.OUT);
		JoinPoint jp2 = addJoinPoint(n2, "in", Direction.IN);
		Edge edge = addEdge(graph, jp1, jp2);

		GroupBuilder.GroupResult result = GroupBuilder.createFromSelection(graph, Set.of(n1.getId(), n2.getId()),
				"Test Group");
		Group group = result.getGroup();

		assertEquals("Test Group", group.getName());
		assertEquals(2, group.getMemberNodeIds().size());
		assertTrue(group.getMemberNodeIds().contains(n1.getId()));
		assertTrue(group.getMemberNodeIds().contains(n2.getId()));
		assertTrue(group.getMemberEdgeIds().contains(edge.getId()));
		assertTrue(group.getExposedJoinPoints().isEmpty());
		assertTrue(result.getBridgeEdges().isEmpty());
		assertTrue(result.getRelinks().isEmpty());
	}

	@Test
	void crossBoundaryEdgesRelinked() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Node n2 = createNode(graph, 200, 0);
		Node n3 = createNode(graph, 400, 0);
		JoinPoint jp1out = addJoinPoint(n1, "out", Direction.OUT);
		JoinPoint jp2in = addJoinPoint(n2, "in", Direction.IN);
		JoinPoint jp2out = addJoinPoint(n2, "out2", Direction.OUT);
		JoinPoint jp3in = addJoinPoint(n3, "in", Direction.IN);

		Edge e1 = addEdge(graph, jp1out, jp2in);
		Edge e2 = addEdge(graph, jp2out, jp3in);

		GroupBuilder.GroupResult result = GroupBuilder.createFromSelection(graph, Set.of(n2.getId()), "Middle");
		Group group = result.getGroup();

		assertEquals(2, group.getExposedJoinPoints().size());
		assertEquals(2, result.getBridgeEdges().size());
		assertEquals(2, result.getRelinks().size());

		GroupBuilder.EdgeRelink r1 = result.getRelinks().stream().filter(r -> r.getEdgeId().equals(e1.getId()))
				.findFirst().orElseThrow();
		assertFalse(r1.isSource());
		assertEquals(jp2in.getId(), r1.getOriginalJoinPointId());

		GroupBuilder.EdgeRelink r2 = result.getRelinks().stream().filter(r -> r.getEdgeId().equals(e2.getId()))
				.findFirst().orElseThrow();
		assertTrue(r2.isSource());
		assertEquals(jp2out.getId(), r2.getOriginalJoinPointId());
	}

	@Test
	void relinkAppliedAndReversed() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Node n2 = createNode(graph, 200, 0);
		JoinPoint jp1out = addJoinPoint(n1, "out", Direction.OUT);
		JoinPoint jp2in = addJoinPoint(n2, "in", Direction.IN);

		Edge edge = addEdge(graph, jp1out, jp2in);

		GroupBuilder.GroupResult result = GroupBuilder.createFromSelection(graph, Set.of(n2.getId()), "G");
		Group group = result.getGroup();

		cz.bliksoft.javautils.fx.controls.graph.command.GroupCommand cmd = new cz.bliksoft.javautils.fx.controls.graph.command.GroupCommand(
				graph, group, result.getBridgeEdges(), result.getRelinks());

		assertEquals(jp2in.getId(), edge.getTargetJoinPointId());

		cmd.execute();
		assertNotEquals(jp2in.getId(), edge.getTargetJoinPointId());
		assertEquals(1, result.getBridgeEdges().size());
		assertTrue(graph.getEdges().contains(result.getBridgeEdges().get(0)));

		cmd.undo();
		assertEquals(jp2in.getId(), edge.getTargetJoinPointId());
		assertFalse(graph.getEdges().contains(result.getBridgeEdges().get(0)));
	}

	@Test
	void collapsedBoundsComputed() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 10, 20);
		n1.setWidth(100);
		n1.setHeight(50);
		Node n2 = createNode(graph, 200, 100);
		n2.setWidth(100);
		n2.setHeight(50);

		Group group = GroupBuilder.createFromSelection(graph, Set.of(n1.getId(), n2.getId()), "Bounds").getGroup();

		assertEquals(-10, group.getX());
		assertEquals(-20, group.getY());
		assertEquals(330, group.getWidth());
		assertEquals(190, group.getHeight());
	}

	@Test
	void findGroupContaining() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Node n2 = createNode(graph, 200, 0);

		Group group = GroupBuilder.createFromSelection(graph, Set.of(n1.getId()), "G1").getGroup();
		graph.getGroups().add(group);

		assertNotNull(GroupBuilder.findGroupContaining(graph, n1.getId()));
		assertNull(GroupBuilder.findGroupContaining(graph, n2.getId()));
	}

	@Test
	void findGroupById() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Group group = GroupBuilder.createFromSelection(graph, Set.of(n1.getId()), "G1").getGroup();
		graph.getGroups().add(group);

		assertNotNull(GroupBuilder.findGroupById(graph, group.getId()));
		assertNull(GroupBuilder.findGroupById(graph, java.util.UUID.randomUUID()));
	}

	@Test
	void noDuplicateExposedJoinPoints() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Node n2 = createNode(graph, 200, 0);
		Node n3 = createNode(graph, 200, 200);
		JoinPoint jp1out = addJoinPoint(n1, "out", Direction.OUT);
		JoinPoint jp2in = addJoinPoint(n2, "in", Direction.IN);
		JoinPoint jp3in = addJoinPoint(n3, "in2", Direction.IN);

		addEdge(graph, jp1out, jp2in);
		addEdge(graph, jp1out, jp3in);

		Group group = GroupBuilder.createFromSelection(graph, Set.of(n1.getId()), "Hub").getGroup();

		assertEquals(1, group.getExposedJoinPoints().size());
	}

	@Test
	void groupAndUngroupRoundTrip() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Node n2 = createNode(graph, 200, 0);

		Group group = GroupBuilder.createFromSelection(graph, Set.of(n1.getId(), n2.getId()), "G").getGroup();
		graph.getGroups().add(group);
		assertEquals(1, graph.getGroups().size());

		graph.getGroups().remove(group);
		assertEquals(0, graph.getGroups().size());
	}

	@Test
	void groupPersistsViaXml() throws Exception {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Node n2 = createNode(graph, 200, 0);
		JoinPoint jp1 = addJoinPoint(n1, "out", Direction.OUT);
		JoinPoint jp2 = addJoinPoint(n2, "in", Direction.IN);
		addEdge(graph, jp1, jp2);

		Group group = GroupBuilder.createFromSelection(graph, Set.of(n1.getId(), n2.getId()), "Persisted").getGroup();
		graph.getGroups().add(group);

		String xml = cz.bliksoft.dataflow.xml.GraphSerializer.marshal(graph);
		Graph loaded = cz.bliksoft.dataflow.xml.GraphSerializer.unmarshal(xml);

		assertEquals(1, loaded.getGroups().size());
		Group loadedGroup = loaded.getGroups().get(0);
		assertEquals("Persisted", loadedGroup.getName());
		assertEquals(2, loadedGroup.getMemberNodeIds().size());
	}

	private Node createNode(Graph graph, double x, double y) {
		Node node = new Node("process", x, y, 120, 60);
		graph.getNodes().add(node);
		return node;
	}

	private JoinPoint addJoinPoint(Node node, String name, Direction direction) {
		JoinPoint jp = new JoinPoint(name, JoinPointPosition.RIGHT, direction);
		node.getJoinPoints().add(jp);
		return jp;
	}

	private Edge addEdge(Graph graph, JoinPoint source, JoinPoint target) {
		Edge edge = new Edge("default", source.getId(), target.getId());
		graph.getEdges().add(edge);
		return edge;
	}
}
