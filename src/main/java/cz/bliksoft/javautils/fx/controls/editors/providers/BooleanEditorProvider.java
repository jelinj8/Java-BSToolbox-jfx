package cz.bliksoft.javautils.fx.controls.editors.providers;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

public class BooleanEditorProvider implements IValueEditorProvider<Boolean> {

    @Override
    public Node createEditor(ObjectProperty<Boolean> prop) {
        CheckBox cb = new CheckBox();
        Boolean init = prop.get();
        cb.setSelected(init != null && init);
        cb.selectedProperty().bindBidirectional(prop);
        return cb;
    }

    @Override
    public String toDisplayString(Boolean value) {
        return value != null ? value.toString() : "";
    }

    @Override
    public Boolean fromString(String s) {
        if (s == null)
            return null;
        return Boolean.parseBoolean(s.trim());
    }
}
