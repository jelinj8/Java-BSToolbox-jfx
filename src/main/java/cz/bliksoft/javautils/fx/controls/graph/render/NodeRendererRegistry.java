package cz.bliksoft.javautils.fx.controls.graph.render;

import java.util.LinkedHashMap;
import java.util.Map;

public class NodeRendererRegistry {

	private static final NodeRendererRegistry INSTANCE = new NodeRendererRegistry();

	private final Map<String, INodeRenderer> renderers = new LinkedHashMap<>();
	private INodeRenderer fallback;

	public static NodeRendererRegistry getInstance() {
		return INSTANCE;
	}

	public void register(String typeId, INodeRenderer renderer) {
		renderers.put(typeId, renderer);
	}

	public INodeRenderer get(String typeId) {
		INodeRenderer r = renderers.get(typeId);
		return r != null ? r : fallback;
	}

	public void setFallback(INodeRenderer fallback) {
		this.fallback = fallback;
	}

	public void clear() {
		renderers.clear();
		fallback = null;
	}
}
