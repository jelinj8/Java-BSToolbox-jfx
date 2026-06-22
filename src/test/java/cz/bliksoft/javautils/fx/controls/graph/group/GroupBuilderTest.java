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
		assertEquals(2, group.getNodes().size());
		assertNotNull(group.findNode(n1.getId()));
		assertNotNull(group.findNode(n2.getId()));
		assertTrue(group.getEdges().stream().anyMatch(e -> e.getId().equals(edge.getId())));
		assertTrue(group.getExposedJoinPoints().isEmpty());
		assertTrue(result.getBridgeEdges().isEmpty());
		assertTrue(result.getRelinks().isEmpty());
		assertTrue(graph.getNodes().isEmpty());
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

		addEdge(graph, jp1out, jp2in);
		addEdge(graph, jp2out, jp3in);

		GroupBuilder.GroupResult result = GroupBuilder.createFromSelection(graph, Set.of(n2.getId()), "Middle");
		Group group = result.getGroup();

		assertEquals(2, group.getExposedJoinPoints().size());
		assertEquals(2, result.getBridgeEdges().size());
		assertEquals(2, result.getRelinks().size());
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

		GroupBuilder.createFromSelection(graph, Set.of(n1.getId()), "G1");

		Group n1Parent = GroupBuilder.findGroupContaining(graph, n1.getId());
		assertNotNull(n1Parent);
		assertNotEquals(graph.getId(), n1Parent.getId());

		Group n2Parent = GroupBuilder.findGroupContaining(graph, n2.getId());
		assertNotNull(n2Parent);
		assertEquals(graph.getId(), n2Parent.getId());
	}

	@Test
	void findGroupById() {
		Graph graph = new Graph("test");
		Node n1 = createNode(graph, 0, 0);
		Group group = GroupBuilder.createFromSelection(graph, Set.of(n1.getId()), "G1").getGroup();

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

		GroupBuilder.createFromSelection(graph, Set.of(n1.getId(), n2.getId()), "G");
		assertEquals(1, graph.getGroups().size());

		graph.getGroups().clear();
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

		GroupBuilder.createFromSelection(graph, Set.of(n1.getId(), n2.getId()), "Persisted");

		String xml = cz.bliksoft.dataflow.xml.GraphSerializer.marshal(graph);
		Graph loaded = cz.bliksoft.dataflow.xml.GraphSerializer.unmarshal(xml);

		assertEquals(1, loaded.getGroups().size());
		Group loadedGroup = loaded.getGroups().get(0);
		assertEquals("Persisted", loadedGroup.getName());
		assertEquals(2, loadedGroup.getNodes().size());
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
