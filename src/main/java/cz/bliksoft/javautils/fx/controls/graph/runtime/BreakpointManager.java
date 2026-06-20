package cz.bliksoft.javautils.fx.controls.graph.runtime;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class BreakpointManager {

	private final Set<UUID> breakpoints = new LinkedHashSet<>();

	public void toggleBreakpoint(UUID nodeId) {
		if (!breakpoints.remove(nodeId))
			breakpoints.add(nodeId);
	}

	public boolean hasBreakpoint(UUID nodeId) {
		return breakpoints.contains(nodeId);
	}

	public Set<UUID> getBreakpoints() {
		return Collections.unmodifiableSet(breakpoints);
	}

	public void clear() {
		breakpoints.clear();
	}
}
