package cz.bliksoft.javautils.fx.controls.graph.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import cz.bliksoft.dataflow.engine.GraphInstance;
import cz.bliksoft.dataflow.engine.NodeState;
import cz.bliksoft.dataflow.manager.ManagedGraph;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.xml.GraphSerializer;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

/**
 * Attaches the editor to a managed-graph run for read-only monitoring: loads a
 * clone of the run's graph into the canvas and reflects its node states (and,
 * via the panel, its variables) on a {@link NodeStateOverlay}, refreshed on a
 * timer while the run is live. The clone (a JAXB round-trip, which preserves
 * node UUIDs) ensures the editor can never mutate the manager's live graph.
 */
public class GraphMonitorController {

	private final GraphCanvas canvas;
	private final Consumer<Graph> graphLoader;
	private final Runnable inspectorRefresh;
	private final NodeStateOverlay overlay;

	private final SimpleBooleanProperty monitoring = new SimpleBooleanProperty(false);
	private final SimpleStringProperty statusText = new SimpleStringProperty("");
	private final Map<UUID, NodeState> applied = new HashMap<>();

	private Timeline timeline;
	private ManagedGraph managedGraph;
	private GraphInstance instance;
	private String managerLabel = "";

	/**
	 * @param canvas           the editor canvas to load the monitored graph into
	 * @param graphLoader      loads a graph into the editor (canvas + group editing
	 *                         pane); called on attach
	 * @param inspectorRefresh refreshes the context inspector for the monitored
	 *                         instance; called on attach and each poll
	 */
	public GraphMonitorController(GraphCanvas canvas, Consumer<Graph> graphLoader, Runnable inspectorRefresh) {
		this.canvas = canvas;
		this.graphLoader = graphLoader;
		this.inspectorRefresh = inspectorRefresh;
		this.overlay = new NodeStateOverlay(canvas);
	}

	public BooleanProperty monitoringProperty() {
		return monitoring;
	}

	public boolean isMonitoring() {
		return monitoring.get();
	}

	public StringProperty statusTextProperty() {
		return statusText;
	}

	public GraphInstance getMonitoredInstance() {
		return instance;
	}

	public void attach(AttachModel.Selection selection) {
		if (selection == null || selection.managedGraph() == null)
			return;
		detach();

		this.managedGraph = selection.managedGraph();
		this.instance = selection.instance();
		this.managerLabel = selection.managerLabel() != null ? selection.managerLabel() : "";

		Graph base = instance != null ? asGraph(instance.getGraph()) : null;
		if (base == null)
			base = managedGraph.getResolvedGraph();
		if (base == null)
			return;

		graphLoader.accept(cloneGraph(base));

		monitoring.set(true);
		updateStatusText();
		applyStates();
		if (inspectorRefresh != null)
			inspectorRefresh.run();

		if (instance != null && !instance.isCompleted())
			startPolling();
	}

	public void detach() {
		stopPolling();
		overlay.clear();
		applied.clear();
		instance = null;
		managedGraph = null;
		monitoring.set(false);
		statusText.set("");
	}

	private void startPolling() {
		timeline = new Timeline(new KeyFrame(Duration.millis(750), e -> tick()));
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
	}

	private void stopPolling() {
		if (timeline != null) {
			timeline.stop();
			timeline = null;
		}
	}

	private void tick() {
		if (instance == null) {
			stopPolling();
			return;
		}
		applyStates();
		updateStatusText();
		if (inspectorRefresh != null)
			inspectorRefresh.run();
		if (instance.isCompleted())
			stopPolling();
	}

	private void applyStates() {
		if (instance == null)
			return;
		for (var entry : instance.getNodeStates().entrySet()) {
			if (applied.get(entry.getKey()) != entry.getValue()) {
				overlay.updateNodeState(entry.getKey(), entry.getValue());
				applied.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private void updateStatusText() {
		if (managedGraph == null) {
			statusText.set("");
			return;
		}
		String prefix = managerLabel.isBlank() ? "" : managerLabel + " / ";
		statusText.set(prefix + managedGraph.getId() + " [" + managedGraph.getStatus() + "]");
	}

	private static Graph asGraph(Group group) {
		return group instanceof Graph graph ? graph : null;
	}

	private static Graph cloneGraph(Graph graph) {
		try {
			return GraphSerializer.unmarshal(GraphSerializer.marshal(graph));
		} catch (Exception e) {
			// extremely unlikely; fall back to the live graph (display only)
			return graph;
		}
	}
}
