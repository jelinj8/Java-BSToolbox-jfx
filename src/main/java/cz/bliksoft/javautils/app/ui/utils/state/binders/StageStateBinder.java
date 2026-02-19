package cz.bliksoft.javautils.app.ui.utils.state.binders;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

import cz.bliksoft.javautils.app.BSApp;

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

		BSApp.getLocalProperties().putBool(pfx + ".max", stage.isMaximized());
		BSApp.getLocalProperties().putBool(pfx + ".fs", stage.isFullScreen());

		// Ulož "normal bounds" pouze pokud okno není max/fullscreen
		if (!stage.isMaximized() && !stage.isFullScreen()) {
			double x = stage.getX();
			double y = stage.getY();
			double w = stage.getWidth();
			double h = stage.getHeight();

			if (Double.isFinite(x) && Double.isFinite(y)) {
				BSApp.getLocalProperties().putDouble(pfx + ".x", x);
				BSApp.getLocalProperties().putDouble(pfx + ".y", y);
			}
			if (Double.isFinite(w) && Double.isFinite(h) && w > 200 && h > 150) {
				BSApp.getLocalProperties().putDouble(pfx + ".w", w);
				BSApp.getLocalProperties().putDouble(pfx + ".h", h);
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
			Double w = BSApp.getLocalProperties().getDouble(pfx + ".w");
			Double h = BSApp.getLocalProperties().getDouble(pfx + ".h");
			Double x = BSApp.getLocalProperties().getDouble(pfx + ".x");
			Double y = BSApp.getLocalProperties().getDouble(pfx + ".y");
			Boolean max = BSApp.getLocalProperties().getBool(pfx + ".max");
			Boolean fs = BSApp.getLocalProperties().getBool(pfx + ".fs");

			// Nejprve velikost/pozice (normal bounds)
			if (w != null && h != null && Double.isFinite(w) && Double.isFinite(h) && w > 200 && h > 150) {
				stage.setWidth(w);
				stage.setHeight(h);
			}
			if (x != null && y != null && Double.isFinite(x) && Double.isFinite(y)) {
				stage.setX(x);
				stage.setY(y);
			}

			clampToVisibleArea(stage);

			// Pak stavy
			if (fs != null)
				stage.setFullScreen(fs);
			if (max != null)
				stage.setMaximized(max);
		});
	}

	/**
	 * Když uživatel odpojil monitor / změnil rozlišení, může uložené okno skončit
	 * mimo obraz. Tohle ho vrátí aspoň částečně do viditelné oblasti (union všech
	 * monitorů).
	 */
	private static void clampToVisibleArea(Stage stage) {
		double x = stage.getX();
		double y = stage.getY();
		double w = stage.getWidth();
		double h = stage.getHeight();

		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(w) || !Double.isFinite(h))
			return;
		if (w <= 0 || h <= 0)
			return;

		Rectangle2D union = unionVisualBounds(Screen.getScreens());
		if (union == null)
			return;

		// Pokud je okno aspoň částečně v union, nech být
		if (intersects(union, x, y, w, h))
			return;

		// Jinak ho přesuň dovnitř union (s okrajem)
		double margin = 20;
		double nx = clamp(x, union.getMinX() + margin, union.getMaxX() - margin - w);
		double ny = clamp(y, union.getMinY() + margin, union.getMaxY() - margin - h);

		// když je okno větší než union, aspoň přilep vlevo/nahoře
		if (w > union.getWidth())
			nx = union.getMinX();
		if (h > union.getHeight())
			ny = union.getMinY();

		stage.setX(nx);
		stage.setY(ny);
	}

	private static Rectangle2D unionVisualBounds(List<Screen> screens) {
		if (screens == null || screens.isEmpty())
			return null;

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

		for (Screen s : screens) {
			Rectangle2D b = s.getVisualBounds();
			minX = Math.min(minX, b.getMinX());
			minY = Math.min(minY, b.getMinY());
			maxX = Math.max(maxX, b.getMaxX());
			maxY = Math.max(maxY, b.getMaxY());
		}
		return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
	}

	private static boolean intersects(Rectangle2D r, double x, double y, double w, double h) {
		double rx2 = r.getMinX() + r.getWidth();
		double ry2 = r.getMinY() + r.getHeight();
		double x2 = x + w;
		double y2 = y + h;
		return x < rx2 && x2 > r.getMinX() && y < ry2 && y2 > r.getMinY();
	}

	private static double clamp(double v, double min, double max) {
		if (max < min)
			return min; // když je okno větší než prostor
		return Math.max(min, Math.min(max, v));
	}
}
