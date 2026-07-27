package cz.bliksoft.javautils.app.ui.utils.state.binders;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Objects;

import cz.bliksoft.javautils.app.BSAppJFX;
import cz.bliksoft.javautils.app.ui.BSAppUI;

public final class StageStateBinder {

	private StageStateBinder() {
	}

	/**
	 * Uloží stav okna (on-demand). Klíče: ${pfx}.x .y .w .h .max .fs
	 *
	 * Doporučený prefix: windowKey + ".@stage"
	 */
	public static void save(Stage stage, String pfx) {
		Objects.requireNonNull(stage, "stage");
		Objects.requireNonNull(pfx, "pfx");

		BSAppJFX.getLocalProperties().putBool(pfx + ".max", stage.isMaximized());
		BSAppJFX.getLocalProperties().putBool(pfx + ".fs", stage.isFullScreen());

		// Ulož "normal bounds" pouze pokud okno není max/fullscreen
		if (!stage.isMaximized() && !stage.isFullScreen()) {
			double x = stage.getX();
			double y = stage.getY();
			double w = stage.getWidth();
			double h = stage.getHeight();

			if (Double.isFinite(x) && Double.isFinite(y)) {
				BSAppJFX.getLocalProperties().putDouble(pfx + ".x", x);
				BSAppJFX.getLocalProperties().putDouble(pfx + ".y", y);
			}
			if (Double.isFinite(w) && Double.isFinite(h) && w > 200 && h > 150) {
				BSAppJFX.getLocalProperties().putDouble(pfx + ".w", w);
				BSAppJFX.getLocalProperties().putDouble(pfx + ".h", h);
			}
		}
	}

	/**
	 * Obnoví stav okna (on-demand). Volat po vytvoření scény; bezpečné je zavolat
	 * před show(), protože aplikace proběhne přes Platform.runLater().
	 */
	public static void restore(Stage stage, String pfx) {
		Objects.requireNonNull(stage, "stage");
		Objects.requireNonNull(pfx, "pfx");

		Platform.runLater(() -> {
			Double w = BSAppJFX.getLocalProperties().getDouble(pfx + ".w");
			Double h = BSAppJFX.getLocalProperties().getDouble(pfx + ".h");
			Double x = BSAppJFX.getLocalProperties().getDouble(pfx + ".x");
			Double y = BSAppJFX.getLocalProperties().getDouble(pfx + ".y");
			Boolean max = BSAppJFX.getLocalProperties().getBool(pfx + ".max");
			Boolean fs = BSAppJFX.getLocalProperties().getBool(pfx + ".fs");

			// Nejprve velikost/pozice (normal bounds)
			if (w != null && h != null && Double.isFinite(w) && Double.isFinite(h) && w > 200 && h > 150) {
				stage.setWidth(w);
				stage.setHeight(h);
			}
			if (x != null && y != null && Double.isFinite(x) && Double.isFinite(y)) {
				stage.setX(x);
				stage.setY(y);
			}

			// Přesuň/zmenši okno do viditelné oblasti; maximalizovaná/fullscreen
			// okna řeší platforma sama a jejich "normal bounds" se nemají měnit
			boolean maximizing = Boolean.TRUE.equals(max) || Boolean.TRUE.equals(fs);
			if (!maximizing)
				BSAppUI.fitToScreen(stage);

			// Pak stavy
			if (fs != null)
				stage.setFullScreen(fs);
			if (max != null)
				stage.setMaximized(max);
		});
	}
}
