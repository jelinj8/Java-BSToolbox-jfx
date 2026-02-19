package cz.bliksoft.javautils.app.ui.utils.state.binders;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;

import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateBinder;

public final class SplitPaneBinder implements FxStateBinder {

	@Override
	public boolean supports(Node n) {
		return n instanceof SplitPane;
	}

	@Override
	public void save(Node n, String pfx) {
		SplitPane sp = (SplitPane) n;
		BSApp.getLocalProperties().put(pfx + ".div", join(sp.getDividerPositions()));
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
			List<Double> out = new ArrayList<>();
			for (String p : parts) {
				String t = p.trim();
				if (!t.isEmpty())
					out.add(Double.parseDouble(t));
			}
			double[] r = new double[out.size()];
			for (int i = 0; i < out.size(); i++)
				r[i] = out.get(i);
			return r;
		} catch (Exception e) {
			return null;
		}
	}
}
