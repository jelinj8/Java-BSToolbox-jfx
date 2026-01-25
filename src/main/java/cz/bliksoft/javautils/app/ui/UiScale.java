package cz.bliksoft.javautils.app.ui;

import javafx.stage.Screen;

public final class UiScale {
	private UiScale() {
	}

	public static double outputScale() {
		// Prefer output scale if available (newer JavaFX), fallback to DPI/96
		try {
			// JavaFX 19+ has outputScaleX/outputScaleY on Screen
			double s = Screen.getPrimary().getOutputScaleX();
			if (s > 0)
				return s;
		} catch (Throwable ignored) {
		}

		// Fallback
		return Screen.getPrimary().getDpi() / 96.0;
	}

	/** Quantize to common asset buckets to stabilize caching and file naming. */
	public static String bucketedScaleString() {
		double s = outputScale();
		double b = bucket(s);
		// avoid "2.0"
		if (Math.abs(b - Math.rint(b)) < 1e-9)
			return Integer.toString((int) Math.rint(b));
		return Double.toString(b);
	}

	private static double bucket(double s) {
		// Choose buckets you actually have assets for
		// (add/remove as needed)
		double[] buckets = { 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0 };
		double best = buckets[0];
		double bestDist = Math.abs(s - best);
		for (double b : buckets) {
			double d = Math.abs(s - b);
			if (d < bestDist) {
				bestDist = d;
				best = b;
			}
		}
		return best;
	}
}
