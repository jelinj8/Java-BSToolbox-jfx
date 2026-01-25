package cz.bliksoft.javautils.app.ui.builder;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import javafx.scene.Node;

public final class SlotResolver {
	private final Map<Node, Map<String, Node>> explicitSlots = new IdentityHashMap<>();

	public void registerSlotOwner(Node owner, String id, Node slot) {
		explicitSlots.computeIfAbsent(owner, k -> new HashMap<>()).put(id, slot);
	}

	public Node resolveSlot(Node owner, String id) {
		// explicit first
		Map<String, Node> m = explicitSlots.get(owner);
		if (m != null && m.containsKey(id))
			return m.get(id);

		// fallback: lookup
		Node found = owner.lookup("#" + id);
		if (found != null)
			return found;

		return null;
	}
}
