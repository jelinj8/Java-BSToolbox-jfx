package cz.bliksoft.javautils.fx.controls.graph.command;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import cz.bliksoft.dataflow.model.Direction;
import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.JoinPointPosition;
import cz.bliksoft.dataflow.model.Node;

class CreateCommandsTest {

	@Test
	void createEdgeCommandAddAndUndo() {
		Graph graph = new Graph("test");
		Node n1 = new Node("a", 0, 0, 100, 50);
		JoinPoint jp1 = new JoinPoint("out", JoinPointPosition.RIGHT, Direction.OUT);
		n1.getJoinPoints().add(jp1);
		Node n2 = new Node("b", 200, 0, 100, 50);
		JoinPoint jp2 = new JoinPoint("in", JoinPointPosition.LEFT, Direction.IN);
		n2.getJoinPoints().add(jp2);
		graph.getNodes().add(n1);
		graph.getNodes().add(n2);

		Edge edge = new Edge("default", jp1.getId(), jp2.getId());
		CreateEdgeCommand cmd = new CreateEdgeCommand(graph, edge);

		assertEquals(0, graph.getEdges().size());

		cmd.execute();
		assertEquals(1, graph.getEdges().size());
		assertEquals(edge.getId(), graph.getEdges().get(0).getId());

		cmd.undo();
		assertEquals(0, graph.getEdges().size());

		cmd.redo();
		assertEquals(1, graph.getEdges().size());
	}

	@Test
	void createNodeCommandAddAndUndo() {
		Graph graph = new Graph("test");
		Node node = new Node("process", 50, 50, 120, 60);
		CreateNodeCommand cmd = new CreateNodeCommand(graph, node);

		assertEquals(0, graph.getNodes().size());

		cmd.execute();
		assertEquals(1, graph.getNodes().size());
		assertEquals(node.getId(), graph.getNodes().get(0).getId());

		cmd.undo();
		assertEquals(0, graph.getNodes().size());

		cmd.redo();
		assertEquals(1, graph.getNodes().size());
	}

	@Test
	void deleteRemovesConnectedEdges() {
		Graph graph = new Graph("test");
		Node n1 = new Node("a", 0, 0, 100, 50);
		JoinPoint jp1 = new JoinPoint("out", JoinPointPosition.RIGHT, Direction.OUT);
		n1.getJoinPoints().add(jp1);
		Node n2 = new Node("b", 200, 0, 100, 50);
		JoinPoint jp2 = new JoinPoint("in", JoinPointPosition.LEFT, Direction.IN);
		n2.getJoinPoints().add(jp2);
		graph.getNodes().add(n1);
		graph.getNodes().add(n2);

		Edge edge = new Edge("default", jp1.getId(), jp2.getId());
		graph.getEdges().add(edge);

		DeleteElementsCommand cmd = new DeleteElementsCommand(graph, java.util.Set.of(n1.getId()));
		cmd.execute();

		assertEquals(1, graph.getNodes().size());
		assertEquals(n2.getId(), graph.getNodes().get(0).getId());
		assertEquals(0, graph.getEdges().size());

		cmd.undo();
		assertEquals(2, graph.getNodes().size());
		assertEquals(1, graph.getEdges().size());
	}
}
