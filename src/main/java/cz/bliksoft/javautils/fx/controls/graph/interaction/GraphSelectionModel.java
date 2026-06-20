package cz.bliksoft.javautils.fx.controls.graph.interaction;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class GraphSelectionModel {

	private final ObservableSet<UUID> selection = FXCollections.observableSet(new LinkedHashSet<>());
	private final ReadOnlyObjectWrapper<Set<UUID>> selectedProperty = new ReadOnlyObjectWrapper<>(
			Collections.unmodifiableSet(selection));

	public void select(UUID id) {
		selection.clear();
		selection.add(id);
	}

	public void addToSelection(UUID id) {
		selection.add(id);
	}

	public void toggle(UUID id) {
		if (selection.contains(id))
			selection.remove(id);
		else
			selection.add(id);
	}

	public void selectAll(Set<UUID> ids) {
		selection.clear();
		selection.addAll(ids);
	}

	public void clear() {
		selection.clear();
	}

	public boolean isSelected(UUID id) {
		return selection.contains(id);
	}

	public boolean isEmpty() {
		return selection.isEmpty();
	}

	public int size() {
		return selection.size();
	}

	public Set<UUID> getSelection() {
		return Collections.unmodifiableSet(selection);
	}

	public ObservableSet<UUID> observableSelection() {
		return selection;
	}

	public ReadOnlyObjectProperty<Set<UUID>> selectedProperty() {
		return selectedProperty.getReadOnlyProperty();
	}
}
