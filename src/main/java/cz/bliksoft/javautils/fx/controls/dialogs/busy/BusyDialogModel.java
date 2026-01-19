package cz.bliksoft.javautils.fx.controls.dialogs.busy;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

public final class BusyDialogModel {
	private final StringProperty title = new SimpleStringProperty(this, "title", "Working…");
	private final StringProperty detail = new SimpleStringProperty(this, "detail", "");
	private final BooleanProperty showing = new SimpleBooleanProperty(this, "showing", false);

	private final BooleanProperty indeterminate = new SimpleBooleanProperty(this, "indeterminate", true);
	private final LongProperty max = new SimpleLongProperty(this, "max", 100);
	private final LongProperty value = new SimpleLongProperty(this, "value", 0);

	private final ObservableList<javafx.scene.image.Image> frames = FXCollections.observableArrayList();

	private Task<?> boundTask;

	public StringProperty titleProperty() {
		return title;
	}

	public StringProperty detailProperty() {
		return detail;
	}

	public BooleanProperty showingProperty() {
		return showing;
	}

	public BooleanProperty indeterminateProperty() {
		return indeterminate;
	}

	public LongProperty maxProperty() {
		return max;
	}

	public LongProperty valueProperty() {
		return value;
	}

	public ObservableList<javafx.scene.image.Image> getFrames() {
		return frames;
	}

	/**
	 * Bind UI fields to a JavaFX Task (title/message/progress). Safe to call on FX
	 * thread.
	 */
	public void bindToTask(Task<?> task) {
		runFx(() -> {
			unbindTask();
			this.boundTask = task;

			// Task title/message update via updateTitle/updateMessage
			title.bind(task.titleProperty());
			detail.bind(task.messageProperty());

			// Task progress: progress in [0..1] or -1 for indeterminate
			// map it to value/max for your overlay's "x / y" display
			indeterminate.bind(task.progressProperty().lessThan(0));

			// Keep max fixed at 100 and scale value, so "x / y" remains meaningful.
			// If you'd rather show percent only, you can drop max/value altogether.
			max.set(100);
			value.bind(Bindings.createLongBinding(() -> {
				double p = task.getProgress();
				if (p < 0)
					return 0L;
				double clamped = Math.max(0, Math.min(1, p));
				return Math.round(clamped * 100.0);
			}, task.progressProperty()));
		});
	}

	/**
	 * Show overlay while task is RUNNING, hide when it finishes
	 * (SUCCEEDED/FAILED/CANCELLED).
	 */
	public void showWhileRunning(Task<?> task) {
		runFx(() -> {
			bindToTask(task);

			// initial state
			showing.set(task.getState() == Worker.State.RUNNING);

			task.stateProperty().addListener((obs, oldS, newS) -> {
				if (newS == Worker.State.RUNNING) {
					showing.set(true);
				} else if (newS == Worker.State.SUCCEEDED || newS == Worker.State.FAILED
						|| newS == Worker.State.CANCELLED) {
					showing.set(false);
					// optional: unbind automatically when done
					unbindTask();
				}
			});
		});
	}

	/** Unbind from any previously-bound task to avoid memory leaks. */
	public void unbindTask() {
		runFx(() -> {
			if (boundTask != null) {
				if (title.isBound())
					title.unbind();
				if (detail.isBound())
					detail.unbind();
				if (indeterminate.isBound())
					indeterminate.unbind();
				if (value.isBound())
					value.unbind();
				// max isn't bound in this approach
				boundTask = null;
			}
		});
	}

	// Convenience setters (still useful when you don't have a Task)
	public void setShowingFx(boolean v) {
		runFx(() -> showing.set(v));
	}

	public void setTitleFx(String s) {
		runFx(() -> title.set(s));
	}

	public void setDetailFx(String s) {
		runFx(() -> detail.set(s));
	}

	public void setIndeterminateFx(boolean v) {
		runFx(() -> indeterminate.set(v));
	}

	public void setProgressFx(long value, long max) {
		runFx(() -> {
			this.value.set(value);
			this.max.set(max);
		});
	}

	private static void runFx(Runnable r) {
		if (Platform.isFxApplicationThread())
			r.run();
		else
			Platform.runLater(r);
	}
}
