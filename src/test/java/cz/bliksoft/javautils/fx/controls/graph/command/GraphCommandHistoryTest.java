package cz.bliksoft.javautils.fx.controls.graph.command;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Node;

class GraphCommandHistoryTest {

	@Test
	void executeAndUndo() {
		GraphCommandHistory history = new GraphCommandHistory();
		AtomicInteger counter = new AtomicInteger(0);

		IGraphCommand cmd = new IGraphCommand() {
			@Override
			public void execute() {
				counter.incrementAndGet();
			}

			@Override
			public void undo() {
				counter.decrementAndGet();
			}

			@Override
			public void redo() {
				counter.incrementAndGet();
			}

			@Override
			public String getDescription() {
				return "test";
			}
		};

		assertFalse(history.canUndo());
		assertFalse(history.canRedo());

		history.execute(cmd);
		assertEquals(1, counter.get());
		assertTrue(history.canUndo());
		assertFalse(history.canRedo());

		history.undo();
		assertEquals(0, counter.get());
		assertFalse(history.canUndo());
		assertTrue(history.canRedo());

		history.redo();
		assertEquals(1, counter.get());
		assertTrue(history.canUndo());
		assertFalse(history.canRedo());
	}

	@Test
	void multipleUndoRedo() {
		GraphCommandHistory history = new GraphCommandHistory();
		AtomicInteger counter = new AtomicInteger(0);

		for (int i = 0; i < 5; i++) {
			history.execute(new IGraphCommand() {
				@Override
				public void execute() {
					counter.incrementAndGet();
				}

				@Override
				public void undo() {
					counter.decrementAndGet();
				}

				@Override
				public void redo() {
					counter.incrementAndGet();
				}

				@Override
				public String getDescription() {
					return "inc";
				}
			});
		}

		assertEquals(5, counter.get());

		history.undo();
		history.undo();
		history.undo();
		assertEquals(2, counter.get());
		assertTrue(history.canUndo());
		assertTrue(history.canRedo());

		history.redo();
		assertEquals(3, counter.get());
	}

	@Test
	void newCommandClearsFuture() {
		GraphCommandHistory history = new GraphCommandHistory();
		AtomicInteger counter = new AtomicInteger(0);

		IGraphCommand inc = simpleCommand(counter);
		history.execute(inc);
		history.execute(simpleCommand(counter));
		history.execute(simpleCommand(counter));
		assertEquals(3, counter.get());

		history.undo();
		history.undo();
		assertEquals(1, counter.get());
		assertTrue(history.canRedo());

		history.execute(simpleCommand(counter));
		assertEquals(2, counter.get());
		assertFalse(history.canRedo());
	}

	@Test
	void moveNodesCommandUpdatesModel() {
		Graph graph = new Graph("test");
		Node n1 = new Node("process", 10, 20, 100, 50);
		graph.getNodes().add(n1);

		Map<UUID, double[]> oldPos = new LinkedHashMap<>();
		oldPos.put(n1.getId(), new double[] { 10, 20 });
		Map<UUID, double[]> newPos = new LinkedHashMap<>();
		newPos.put(n1.getId(), new double[] { 100, 200 });

		MoveNodesCommand cmd = new MoveNodesCommand(graph, oldPos, newPos);
		GraphCommandHistory history = new GraphCommandHistory();

		history.execute(cmd);
		assertEquals(100, n1.getX());
		assertEquals(200, n1.getY());

		history.undo();
		assertEquals(10, n1.getX());
		assertEquals(20, n1.getY());

		history.redo();
		assertEquals(100, n1.getX());
		assertEquals(200, n1.getY());
	}

	@Test
	void compoundCommand() {
		AtomicInteger a = new AtomicInteger(0);
		AtomicInteger b = new AtomicInteger(0);

		CompoundGraphCommand compound = new CompoundGraphCommand("compound",
				java.util.List.of(simpleCommand(a), simpleCommand(b)));

		GraphCommandHistory history = new GraphCommandHistory();
		history.execute(compound);
		assertEquals(1, a.get());
		assertEquals(1, b.get());

		history.undo();
		assertEquals(0, a.get());
		assertEquals(0, b.get());
	}

	@Test
	void maxDepthEnforced() {
		GraphCommandHistory history = new GraphCommandHistory(3);
		AtomicInteger counter = new AtomicInteger(0);

		for (int i = 0; i < 5; i++)
			history.execute(simpleCommand(counter));

		assertEquals(5, counter.get());

		int undoCount = 0;
		while (history.canUndo()) {
			history.undo();
			undoCount++;
		}
		assertEquals(3, undoCount);
	}

	@Test
	void undoDescription() {
		GraphCommandHistory history = new GraphCommandHistory();
		assertNull(history.getUndoDescription());

		history.execute(simpleCommand(new AtomicInteger()));
		assertEquals("inc", history.getUndoDescription());
	}

	private IGraphCommand simpleCommand(AtomicInteger counter) {
		return new IGraphCommand() {
			@Override
			public void execute() {
				counter.incrementAndGet();
			}

			@Override
			public void undo() {
				counter.decrementAndGet();
			}

			@Override
			public void redo() {
				counter.incrementAndGet();
			}

			@Override
			public String getDescription() {
				return "inc";
			}
		};
	}
}
