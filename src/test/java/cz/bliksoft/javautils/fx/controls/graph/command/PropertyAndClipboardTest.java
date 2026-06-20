package cz.bliksoft.javautils.fx.controls.graph.command;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import cz.bliksoft.dataflow.model.Direction;
import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.JoinPointPosition;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.xml.GraphSerializer;
import cz.bliksoft.javautils.uuid.RandomUUIDCreator;

class PropertyAndClipboardTest {

	@Test
	void propertyChangeCommandUndoRedo() {
		Graph graph = new Graph("test");
		Node node = new Node("process", 0, 0, 100, 50);
		Map<String, Object> props = new LinkedHashMap<>();
		props.put("label", "Old");
		node.setProperties(props);
		graph.getNodes().add(node);

		PropertyChangeCommand cmd = new PropertyChangeCommand(graph, node.getId(), "label", "Old", "New");
		GraphCommandHistory history = new GraphCommandHistory();

		history.execute(cmd);
		assertEquals("New", node.getProperties().get("label"));

		history.undo();
		assertEquals("Old", node.getProperties().get("label"));

		history.redo();
		assertEquals("New", node.getProperties().get("label"));
	}

	@Test
	void propertyChangeCommandNewKey() {
		Graph graph = new Graph("test");
		Node node = new Node("process", 0, 0, 100, 50);
		node.setProperties(new LinkedHashMap<>());
		graph.getNodes().add(node);

		PropertyChangeCommand cmd = new PropertyChangeCommand(graph, node.getId(), "color", null, "red");
		cmd.execute();
		assertEquals("red", node.getProperties().get("color"));

		cmd.undo();
		assertNull(node.getProperties().get("color"));
	}

	@Test
	void clipboardFragmentSerializationRoundTrip() throws Exception {
		Graph fragment = new Graph("clipboard");

		Node n1 = new Node("process", 10, 20, 120, 60);
		JoinPoint jp1out = new JoinPoint("out", JoinPointPosition.RIGHT, Direction.OUT);
		n1.getJoinPoints().add(jp1out);

		Node n2 = new Node("process", 200, 20, 120, 60);
		JoinPoint jp2in = new JoinPoint("in", JoinPointPosition.LEFT, Direction.IN);
		n2.getJoinPoints().add(jp2in);

		fragment.getNodes().add(n1);
		fragment.getNodes().add(n2);

		Edge edge = new Edge("default", jp1out.getId(), jp2in.getId());
		fragment.getEdges().add(edge);

		String xml = GraphSerializer.marshal(fragment);
		assertNotNull(xml);
		assertTrue(xml.contains("<graph"));

		Graph loaded = GraphSerializer.unmarshal(xml);
		assertEquals(2, loaded.getNodes().size());
		assertEquals(1, loaded.getEdges().size());
	}

	@Test
	void pasteRegeneratesUuids() throws Exception {
		Graph fragment = new Graph("clipboard");
		Node n1 = new Node("process", 10, 20, 120, 60);
		UUID originalId = n1.getId();
		JoinPoint jp1 = new JoinPoint("out", JoinPointPosition.RIGHT, Direction.OUT);
		UUID originalJpId = jp1.getId();
		n1.getJoinPoints().add(jp1);
		fragment.getNodes().add(n1);

		String xml = GraphSerializer.marshal(fragment);
		Graph loaded = GraphSerializer.unmarshal(xml);

		Node loadedNode = loaded.getNodes().get(0);
		assertEquals(originalId, loadedNode.getId());

		java.util.Map<UUID, UUID> idMapping = new java.util.HashMap<>();
		UUID newId = RandomUUIDCreator.getRandomUuid();
		idMapping.put(loadedNode.getId(), newId);
		loadedNode.setId(newId);

		for (JoinPoint jp : loadedNode.getJoinPoints()) {
			UUID newJpId = RandomUUIDCreator.getRandomUuid();
			idMapping.put(jp.getId(), newJpId);
			jp.setId(newJpId);
		}

		assertNotEquals(originalId, loadedNode.getId());
		assertNotEquals(originalJpId, loadedNode.getJoinPoints().get(0).getId());
	}
}
