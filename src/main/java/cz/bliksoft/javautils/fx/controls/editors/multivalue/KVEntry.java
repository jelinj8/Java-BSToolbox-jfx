package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

final class KVEntry<V> {

    final StringProperty key = new SimpleStringProperty();
    final ObjectProperty<V> value = new SimpleObjectProperty<>();

    KVEntry(String k, V v) {
        key.set(k);
        value.set(v);
    }
}
