package cz.bliksoft.javautils.app.ui.builder;

import java.util.IdentityHashMap;
import java.util.Map;

import javafx.scene.control.ToggleGroup;

public final class ToggleGroupResolver {
	private final Map<String, ToggleGroup> groups = new IdentityHashMap<>();

	public ToggleGroup getGroup(String group) {
		ToggleGroup g = groups.get(group);
		if(g==null) {
			g=new ToggleGroup();
			groups.put(group, g);
		}
		return g;
	}
}
