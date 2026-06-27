package cz.bliksoft.javautils.fx.controls.graph.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.bliksoft.dataflow.engine.GraphInstance;
import cz.bliksoft.dataflow.manager.GraphExecutorManager;
import cz.bliksoft.dataflow.manager.ManagedGraph;
import cz.bliksoft.javautils.xmlfilesystem.singletons.Services;

/**
 * Read-only selection model backing the "Attach" picker: lists running
 * {@link GraphExecutorManager}s (loaded as {@code /services} entries), their
 * {@link ManagedGraph}s, and each graph's run {@link GraphInstance}s (including
 * finished ones), plus a "resolved graph" option for static inspection. Pure
 * data/labels, so it is unit-testable without JavaFX.
 */
public final class AttachModel {

	private AttachModel() {
	}

	/**
	 * A choice in the instance combo; {@code instance} is {@code null} for the
	 * resolved-only option.
	 */
	public record InstanceItem(GraphInstance instance, String label) {
	}

	/**
	 * The confirmed attach selection; {@code instance} may be {@code null}
	 * (resolved graph only).
	 */
	public record Selection(String managerLabel, ManagedGraph managedGraph, GraphInstance instance) {
	}

	public static List<GraphExecutorManager> managers() {
		return Services.getServices(GraphExecutorManager.class);
	}

	public static String managerLabel(GraphExecutorManager mgr, int index) {
		String name = mgr.getConfigRoot() != null ? mgr.getConfigRoot().getName() : null;
		if (name == null || name.isBlank())
			name = "manager#" + index;
		int graphs = mgr.listManagedGraphs().size();
		return name + " (" + graphs + " graph" + (graphs == 1 ? "" : "s") + ")";
	}

	public static List<ManagedGraph> graphs(GraphExecutorManager mgr) {
		return mgr.listManagedGraphs();
	}

	public static String graphLabel(ManagedGraph mg) {
		return mg.getId() + " [" + mg.getStatus() + "]";
	}

	/**
	 * @return one item per run instance (newest last, as tracked) plus a final
	 *         "resolved graph" item when a resolved graph is available
	 */
	public static List<InstanceItem> instances(ManagedGraph mg) {
		List<InstanceItem> items = new ArrayList<>();
		for (GraphInstance gi : mg.getInstances()) {
			String state = gi.isCompleted() ? "completed" : "running";
			items.add(new InstanceItem(gi,
					"run " + shortId(gi.getRunId()) + " — " + gi.getStartedAt() + " (" + state + ")"));
		}
		if (mg.getResolvedGraph() != null)
			items.add(new InstanceItem(null, "resolved graph (no run state)"));
		return items;
	}

	private static String shortId(UUID id) {
		String s = id.toString();
		return s.length() > 8 ? s.substring(0, 8) : s;
	}
}
