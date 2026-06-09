package cz.bliksoft.javautils.app.ui.utils.state.binders;

import java.util.HashMap;
import java.util.Map;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateBinder;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;

/**
 * Persists and restores {@link SplitPane} divider positions.
 *
 * <p>
 * Items marked with {@link SplitPane#setResizableWithParent(Node, boolean)
 * setResizableWithParent(item, false)} have their pixel size saved and restored
 * as an absolute value; all other items scale proportionally to fill the
 * remaining space.
 */
public final class SplitPaneBinder implements FxStateBinder {

	@Override
	public boolean supports(Node n) {
		return n instanceof SplitPane;
	}

	@Override
	public void save(Node n, String pfx) {
		SplitPane sp = (SplitPane) n;
		BSApp.getLocalProperties().put(pfx + ".div", join(sp.getDividerPositions()));
		String px = buildPixelSizes(sp);
		if (!px.isEmpty())
			BSApp.getLocalProperties().put(pfx + ".px", px);
	}

	@Override
	public void restore(Node n, String pfx) {
		SplitPane sp = (SplitPane) n;
		String raw = BSApp.getLocalProperties().getProperty(pfx + ".div");
		if (raw == null || raw.isBlank())
			return;
		double[] pos = parseDoubles(raw);
		if (pos == null || pos.length != sp.getDividers().size())
			return;

		String pxRaw = BSApp.getLocalProperties().getProperty(pfx + ".px");
		Map<Integer, Double> pixelSizes = (pxRaw != null && !pxRaw.isBlank()) ? parsePixelSizes(pxRaw) : Map.of();

		if (pixelSizes.isEmpty()) {
			Platform.runLater(() -> sp.setDividerPositions(pos));
		} else {
			restoreWithPixelSizes(sp, pos, pixelSizes);
		}
	}

	// ---- pixel-aware restore ----

	private static void restoreWithPixelSizes(SplitPane sp, double[] savedDivPos, Map<Integer, Double> pixelSizes) {
		Platform.runLater(() -> {
			double totalSize = totalSize(sp);
			if (totalSize > 0) {
				applyAdjustedPositions(sp, savedDivPos, pixelSizes, totalSize);
			} else {
				// SplitPane not yet laid out — wait for first non-zero size
				ObservableValue<? extends Number> sizeProperty = sp.getOrientation() == Orientation.HORIZONTAL
						? sp.widthProperty()
						: sp.heightProperty();
				sizeProperty.addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> obs, Number o, Number newVal) {
						if (newVal.doubleValue() > 0) {
							obs.removeListener(this);
							applyAdjustedPositions(sp, savedDivPos, pixelSizes, newVal.doubleValue());
						}
					}
				});
			}
		});
	}

	/**
	 * Computes new divider positions such that items with a saved pixel size are
	 * restored to exactly that size while all other items scale proportionally.
	 */
	private static void applyAdjustedPositions(SplitPane sp, double[] savedDivPos, Map<Integer, Double> pixelSizes,
			double totalSize) {
		int n = sp.getItems().size();

		// Reconstruct per-item sizes from saved divider fractions
		// augPos = [0, divPos[0], divPos[1], ..., divPos[n-2], 1.0]
		double[] augPos = new double[n + 1];
		augPos[0] = 0.0;
		for (int i = 0; i < savedDivPos.length; i++)
			augPos[i + 1] = savedDivPos[i];
		augPos[n] = 1.0;

		double[] sizes = new double[n];
		double totalFixed = 0;
		double sumPropFrac = 0;
		for (int i = 0; i < n; i++) {
			Double pxSize = pixelSizes.get(i);
			if (pxSize != null) {
				sizes[i] = pxSize;
				totalFixed += pxSize;
			} else {
				sizes[i] = augPos[i + 1] - augPos[i]; // saved fraction; scaled below
				sumPropFrac += sizes[i];
			}
		}

		// Scale proportional items to fill the space not occupied by fixed items
		double remaining = totalSize - totalFixed;
		int propCount = n - pixelSizes.size();
		for (int i = 0; i < n; i++) {
			if (!pixelSizes.containsKey(i)) {
				sizes[i] = sumPropFrac > 0 ? sizes[i] * remaining / sumPropFrac : remaining / Math.max(1, propCount);
			}
		}

		// Cumulative sum → divider fractions
		double[] newDivPos = new double[n - 1];
		double cumulative = 0;
		for (int i = 0; i < n - 1; i++) {
			cumulative += sizes[i];
			newDivPos[i] = cumulative / totalSize;
		}
		sp.setDividerPositions(newDivPos);
	}

	// ---- helpers ----

	private static double totalSize(SplitPane sp) {
		return sp.getOrientation() == Orientation.HORIZONTAL ? sp.getWidth() : sp.getHeight();
	}

	/**
	 * Builds the {@code .px} string: {@code "itemIndex:pixelSize,..."} for every
	 * item with {@code resizableWithParent=false}. Returns an empty string if no
	 * such items exist.
	 */
	private static String buildPixelSizes(SplitPane sp) {
		boolean horizontal = sp.getOrientation() == Orientation.HORIZONTAL;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sp.getItems().size(); i++) {
			Node item = sp.getItems().get(i);
			if (!SplitPane.isResizableWithParent(item)) {
				if (sb.length() > 0)
					sb.append(',');
				double size = horizontal ? item.getBoundsInParent().getWidth() : item.getBoundsInParent().getHeight();
				sb.append(i).append(':').append(size);
			}
		}
		return sb.toString();
	}

	private static Map<Integer, Double> parsePixelSizes(String raw) {
		Map<Integer, Double> result = new HashMap<>();
		for (String entry : raw.split(",")) {
			String t = entry.trim();
			int colon = t.indexOf(':');
			if (colon < 0)
				continue;
			try {
				int idx = Integer.parseInt(t.substring(0, colon).trim());
				double size = Double.parseDouble(t.substring(colon + 1).trim());
				result.put(idx, size);
			} catch (NumberFormatException ignore) {
			}
		}
		return result;
	}

	private static String join(double[] a) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < a.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(a[i]);
		}
		return sb.toString();
	}

	private static double[] parseDoubles(String raw) {
		try {
			String[] parts = raw.split(",");
			double[] r = new double[parts.length];
			for (int i = 0; i < parts.length; i++)
				r[i] = Double.parseDouble(parts[i].trim());
			return r;
		} catch (Exception e) {
			return null;
		}
	}
}
