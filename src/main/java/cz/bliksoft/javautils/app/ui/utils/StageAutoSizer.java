package cz.bliksoft.javautils.app.ui.utils;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public final class StageAutoSizer {

	private StageAutoSizer() {
	}

	private static Runnable autosizer = null;

	public static void autoSize() {
		autosizer.run();
	}

	/**
	 * Updates stage minWidth/minHeight so the scene content is not clipped. Call
	 * once after you set the scene. Works best after stage.show().
	 *
	 * Returns a Runnable you can call to force recalculation manually.
	 */
	public static Runnable install(Stage stage) {
		Scene scene = stage.getScene();
		if (scene == null)
			throw new IllegalStateException("Stage has no Scene yet.");

		Parent root = scene.getRoot();
		if (!(root instanceof Region region)) {
			// Fallback: for non-Region roots, use layoutBounds, but min/pref sizing may be
			// unreliable
			return installForNonRegion(stage);
		}

		final Object lock = new Object();
		final boolean[] pending = { false };

		Runnable recalc = () -> {
			if (scene.getWidth() <= 0 || scene.getHeight() <= 0)
				return;

			// required inner size
			double needW = region.minWidth(-1);
			double needH = region.minHeight(-1);

			// include decorations using measured delta (no guessing titlebar pixels)
			double deltaW = stage.getWidth() - scene.getWidth();
			double deltaH = stage.getHeight() - scene.getHeight();

			stage.setMinWidth(needW + deltaW);
			stage.setMinHeight(needH + deltaH);
		};

		Runnable recalcLaterThrottled = () -> {
			synchronized (lock) {
				if (pending[0])
					return;
				pending[0] = true;
			}
			Platform.runLater(() -> {
				synchronized (lock) {
					pending[0] = false;
				}
				recalc.run();
			});
		};

		// After the stage is shown, sizes become valid
		stage.showingProperty().addListener((obs, was, isNow) -> {
			if (isNow)
				recalcLaterThrottled.run();
		});

		// Recalc when layout changes (dynamic content)
		ChangeListener<Bounds> boundsListener = (obs, o, n) -> recalcLaterThrottled.run();
		region.layoutBoundsProperty().addListener(boundsListener);

		// Also when scene size changes (user resizing) – keeps min consistent if your
		// min changes later
		scene.widthProperty().addListener((obs, o, n) -> recalcLaterThrottled.run());
		scene.heightProperty().addListener((obs, o, n) -> recalcLaterThrottled.run());

		// Do an initial attempt (works if already shown; otherwise showing listener
		// handles it)
		recalcLaterThrottled.run();

		autosizer = recalcLaterThrottled;
		return recalcLaterThrottled;
	}

	private static Runnable installForNonRegion(Stage stage) {
		Scene scene = stage.getScene();
		Parent root = scene.getRoot();

		final Object lock = new Object();
		final boolean[] pending = { false };

		Runnable recalc = () -> {
			if (scene.getWidth() <= 0 || scene.getHeight() <= 0)
				return;

			Bounds b = root.getLayoutBounds();
			double needW = b.getWidth();
			double needH = b.getHeight();

			double deltaW = stage.getWidth() - scene.getWidth();
			double deltaH = stage.getHeight() - scene.getHeight();

			stage.setMinWidth(needW + deltaW);
			stage.setMinHeight(needH + deltaH);
		};

		Runnable recalcLaterThrottled = () -> {
			synchronized (lock) {
				if (pending[0])
					return;
				pending[0] = true;
			}
			Platform.runLater(() -> {
				synchronized (lock) {
					pending[0] = false;
				}
				recalc.run();
			});
		};

		stage.showingProperty().addListener((obs, was, isNow) -> {
			if (isNow)
				recalcLaterThrottled.run();
		});
		root.layoutBoundsProperty().addListener((obs, o, n) -> recalcLaterThrottled.run());
		recalcLaterThrottled.run();

		return recalcLaterThrottled;
	}
}
