package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

final class ListEntry<V> {

	final ObjectProperty<V> value = new SimpleObjectProperty<>();

	ListEntry(V v) {
		value.set(v);
	}
}
