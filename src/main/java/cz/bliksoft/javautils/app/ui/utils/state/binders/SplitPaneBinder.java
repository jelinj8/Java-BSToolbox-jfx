package cz.bliksoft.javautils.app.ui.utils.state.binders;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateBinder;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;

public final class SplitPaneBinder implements FxStateBinder {

	@Override
	public boolean supports(Node n) {
		return n instanceof SplitPane;
	}

	@Override
	public void save(Node n, String pfx) {
		SplitPane sp = (SplitPane) n;
		double[] pos = sp.getDividerPositions();
		if (pos.length == 0)
			return;
		BSApp.getLocalProperties().put(pfx + ".div", join(pos));
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

		applyPositions(sp, pos);
	}

	private static void applyPositions(SplitPane sp, double[] pos) {
		ObservableValue<? extends Number> sizeProp = sp.getOrientation() == Orientation.HORIZONTAL ? sp.widthProperty()
				: sp.heightProperty();

		ChangeListener<Number> listener = new ChangeListener<>() {
			private int attempts = 0;

			@Override
			public void changed(ObservableValue<? extends Number> obs, Number o, Number nv) {
				if (nv.doubleValue() > 0) {
					sp.setDividerPositions(pos);
					attempts++;
					if (attempts >= 2) {
						obs.removeListener(this);
					}
				}
			}
		};
		sizeProp.addListener(listener);

		sp.setDividerPositions(pos);
		Platform.runLater(() -> sp.setDividerPositions(pos));
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
