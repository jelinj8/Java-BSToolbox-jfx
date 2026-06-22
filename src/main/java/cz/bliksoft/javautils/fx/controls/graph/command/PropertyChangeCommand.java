package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;

public class PropertyChangeCommand implements IGraphCommand {

	private final Group graph;
	private final UUID elementId;
	private final String propertyKey;
	private final Object oldValue;
	private final Object newValue;

	public PropertyChangeCommand(Group graph, UUID elementId, String propertyKey, Object oldValue, Object newValue) {
		this.graph = graph;
		this.elementId = elementId;
		this.propertyKey = propertyKey;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	@Override
	public void execute() {
		setProperty(newValue);
	}

	@Override
	public void undo() {
		setProperty(oldValue);
	}

	@Override
	public void redo() {
		setProperty(newValue);
	}

	@Override
	public String getDescription() {
		return "Change property '" + propertyKey + "'";
	}

	private void setProperty(Object value) {
		Map<String, Object> props = findProperties();
		if (props == null)
			return;
		if (value == null)
			props.remove(propertyKey);
		else
			props.put(propertyKey, value);
	}

	private Map<String, Object> findProperties() {
		for (Node n : graph.getNodes()) {
			if (n.getId().equals(elementId)) {
				if (n.getProperties() == null)
					n.setProperties(new LinkedHashMap<>());
				return n.getProperties();
			}
		}
		for (Edge e : graph.getEdges()) {
			if (e.getId().equals(elementId)) {
				if (e.getProperties() == null)
					e.setProperties(new LinkedHashMap<>());
				return e.getProperties();
			}
		}
		for (Group g : graph.getGroups()) {
			if (g.getId().equals(elementId)) {
				if (g.getProperties() == null)
					g.setProperties(new LinkedHashMap<>());
				return g.getProperties();
			}
		}
		return null;
	}
}
