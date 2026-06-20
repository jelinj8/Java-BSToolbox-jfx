package cz.bliksoft.javautils.fx.controls.graph.render;

import java.util.LinkedHashMap;
import java.util.Map;

public class EdgeRendererRegistry {

	private static final EdgeRendererRegistry INSTANCE = new EdgeRendererRegistry();

	private final Map<String, IEdgeRenderer> renderers = new LinkedHashMap<>();
	private IEdgeRenderer fallback;

	public static EdgeRendererRegistry getInstance() {
		return INSTANCE;
	}

	public void register(String typeId, IEdgeRenderer renderer) {
		renderers.put(typeId, renderer);
	}

	public IEdgeRenderer get(String typeId) {
		IEdgeRenderer r = renderers.get(typeId);
		return r != null ? r : fallback;
	}

	public void setFallback(IEdgeRenderer fallback) {
		this.fallback = fallback;
	}

	public void clear() {
		renderers.clear();
		fallback = null;
	}
}
