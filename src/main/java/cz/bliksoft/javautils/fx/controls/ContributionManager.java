package cz.bliksoft.javautils.fx.controls;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

public final class ContributionManager {

	public static final class Contribution {
		private final Node node;
		private final IntegerProperty priority = new SimpleIntegerProperty();
		private final long seq; // stable tiebreaker (insertion order)

		private Contribution(Node node, int priority, long seq) {
			this.node = node;
			this.priority.set(priority);
			this.seq = seq;
		}

		public Node node() {
			return node;
		}

		public IntegerProperty priorityProperty() {
			return priority;
		}

		public int getPriority() {
			return priority.get();
		}

		public void setPriority(int p) {
			priority.set(p);
		}

		long seq() {
			return seq;
		}
	}

	private final ObservableList<Contribution> contributions = FXCollections
			.observableArrayList(c -> new javafx.beans.Observable[] { c.priorityProperty() });

	private final SortedList<Contribution> sorted;
	private final AtomicLong seqGen = new AtomicLong();

	// Define ordering here (lower number = earlier / left; change if you prefer
	// opposite)
	private static final Comparator<Contribution> ORDER = Comparator.comparingInt(Contribution::getPriority)
			.thenComparingLong(Contribution::seq);

	public ContributionManager(Pane targetContainer) {
		sorted = new SortedList<>(contributions, ORDER);

		// Mirror sorted contributions into the container children
		sorted.addListener((ListChangeListener<Contribution>) ch -> {
			// simplest: rebuild; fine for typical toolbar/statusbar sizes
			targetContainer.getChildren().setAll(sorted.stream().map(Contribution::node).toList());
		});

		// initial fill
		targetContainer.getChildren().setAll(sorted.stream().map(Contribution::node).toList());
	}

	public Contribution add(Node node, int priority) {
		var c = new Contribution(node, priority, seqGen.incrementAndGet());
		contributions.add(c);
		return c;
	}

	public void remove(Contribution c) {
		contributions.remove(c);
	}

	public ObservableList<Contribution> contributions() {
		return contributions;
	}
}
