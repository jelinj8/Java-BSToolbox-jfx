package cz.bliksoft.javautils.fx.validation;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;

import javafx.scene.Node;

public final class ValidationRegistry {

	// key -> weak set of nodes
	private final Map<Object, Set<WeakReference<Node>>> byKey = new HashMap<>();

	public void register(Node node, Object key) {
		Objects.requireNonNull(node, "node");
		Objects.requireNonNull(key, "key");

		byKey.computeIfAbsent(key, k -> new HashSet<>()).add(new WeakReference<>(node));
		node.getProperties().put(Validation.VALIDATION_KEY, key); // optional, for debugging
	}

	public List<Node> findTargets(Object key) {
		Set<WeakReference<Node>> refs = byKey.get(key);
		if (refs == null || refs.isEmpty())
			return List.of();

		// clean dead refs as we go
		List<Node> live = new ArrayList<>(refs.size());
		refs.removeIf(ref -> {
			Node n = ref.get();
			if (n == null)
				return true;
			live.add(n);
			return false;
		});

		// optional: remove empty buckets
		if (refs.isEmpty())
			byKey.remove(key);

		return live;
	}

	public List<Node> allRegisteredLiveNodes() {
		// used when you want to clear everything
		List<Node> out = new ArrayList<>();
		for (var e : byKey.entrySet()) {
			Set<WeakReference<Node>> refs = e.getValue();
			refs.removeIf(ref -> ref.get() == null);
			for (var ref : refs)
				out.add(ref.get());
		}
		// remove empty buckets
		byKey.entrySet().removeIf(e -> e.getValue().isEmpty());
		return out.stream().filter(Objects::nonNull).collect(Collectors.toList());
	}
}
