package cz.bliksoft.javautils.fx.controls.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.bliksoft.dataflow.engine.GraphInstance;
import cz.bliksoft.dataflow.engine.NodeState;
import cz.bliksoft.dataflow.manager.GraphLifecycleStatus;
import cz.bliksoft.dataflow.manager.ManagedGraph;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Node;

class AttachModelTest {

	private static Graph graphWithNode() {
		Graph g = new Graph("flow");
		g.getNodes().add(new Node("flowchart-start", 0, 0, 10, 10));
		return g;
	}

	@Test
	void graphLabelShowsIdAndStatus() {
		ManagedGraph mg = new ManagedGraph("g1");
		mg.setStatus(GraphLifecycleStatus.RUNNING);
		assertEquals("g1 [RUNNING]", AttachModel.graphLabel(mg));
	}

	@Test
	void instancesListRunsAndResolvedFallback() {
		Graph running = graphWithNode();
		GraphInstance runningInstance = new GraphInstance(running);

		Graph done = graphWithNode();
		GraphInstance doneInstance = new GraphInstance(done);
		for (Node n : done.getNodes())
			doneInstance.setNodeState(n.getId(), NodeState.COMPLETED);

		ManagedGraph mg = new ManagedGraph("g1");
		mg.setResolvedGraph(graphWithNode());
		mg.addInstance(runningInstance);
		mg.addInstance(doneInstance);

		List<AttachModel.InstanceItem> items = AttachModel.instances(mg);

		// two runs (in add order) + one resolved fallback
		assertEquals(3, items.size());
		assertTrue(items.get(0).label().contains("running"), items.get(0).label());
		assertTrue(items.get(1).label().contains("completed"), items.get(1).label());
		assertNull(items.get(2).instance(), "last item is the resolved-graph fallback");
	}

	@Test
	void instancesEmptyWhenNoRunsAndNoResolved() {
		ManagedGraph mg = new ManagedGraph("g1");
		assertEquals(0, AttachModel.instances(mg).size());
	}
}
