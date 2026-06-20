package cz.bliksoft.javautils.fx.controls.graph.render;

public class RenderContext {

	private final double zoom;
	private final boolean selected;

	public RenderContext(double zoom, boolean selected) {
		this.zoom = zoom;
		this.selected = selected;
	}

	public double getZoom() {
		return zoom;
	}

	public boolean isSelected() {
		return selected;
	}
}
