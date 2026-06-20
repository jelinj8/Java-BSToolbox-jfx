package cz.bliksoft.javautils.fx.controls.graph;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateBinder;
import javafx.scene.Node;

public class GraphCanvasStateBinder implements FxStateBinder {

	@Override
	public boolean supports(Node n) {
		return n instanceof GraphCanvas;
	}

	@Override
	public void save(Node n, String prefix) {
		GraphCanvas c = (GraphCanvas) n;
		BSApp.getLocalProperties().put(prefix + ".zoom", String.valueOf(c.getZoom()));
		BSApp.getLocalProperties().put(prefix + ".scrollX", String.valueOf(c.getScrollX()));
		BSApp.getLocalProperties().put(prefix + ".scrollY", String.valueOf(c.getScrollY()));
		BSApp.getLocalProperties().put(prefix + ".gridStyle", c.getGridStyle().name());
		BSApp.getLocalProperties().put(prefix + ".snapToGrid", String.valueOf(c.isSnapToGrid()));
	}

	@Override
	public void restore(Node n, String prefix) {
		GraphCanvas c = (GraphCanvas) n;

		String zoom = BSApp.getLocalProperties().getProperty(prefix + ".zoom");
		if (zoom != null) {
			try {
				c.setZoom(Double.parseDouble(zoom));
			} catch (NumberFormatException ignore) {
			}
		}

		String sx = BSApp.getLocalProperties().getProperty(prefix + ".scrollX");
		if (sx != null) {
			try {
				c.setScrollX(Double.parseDouble(sx));
			} catch (NumberFormatException ignore) {
			}
		}

		String sy = BSApp.getLocalProperties().getProperty(prefix + ".scrollY");
		if (sy != null) {
			try {
				c.setScrollY(Double.parseDouble(sy));
			} catch (NumberFormatException ignore) {
			}
		}

		String gs = BSApp.getLocalProperties().getProperty(prefix + ".gridStyle");
		if (gs != null) {
			try {
				c.setGridStyle(GridStyle.valueOf(gs));
			} catch (IllegalArgumentException ignore) {
			}
		}

		String snap = BSApp.getLocalProperties().getProperty(prefix + ".snapToGrid");
		if (snap != null) {
			c.setSnapToGrid(Boolean.parseBoolean(snap));
		}
	}
}
