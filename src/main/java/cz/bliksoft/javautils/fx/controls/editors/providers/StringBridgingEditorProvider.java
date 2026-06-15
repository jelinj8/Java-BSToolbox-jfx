package cz.bliksoft.javautils.fx.controls.editors.providers;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Bridges a typed {@link IValueEditorProvider} (e.g. for {@link Integer} or
 * {@link Boolean}) to {@link IValueEditorProvider}&lt;{@link String}&gt;, for
 * values that are actually stored as {@link String} (such as
 * {@code Map<String,String>} advanced properties) but should be edited with a
 * type-specific editor.
 *
 * <p>
 * The inline editor/dialog operates on a typed proxy property created via
 * {@link IValueEditorProvider#fromString(String)}; changes to the proxy are
 * propagated back to the outer {@link String} property via
 * {@link IValueEditorProvider#toDisplayString(Object)}.
 */
public class StringBridgingEditorProvider<T> implements IValueEditorProvider<String> {

	private final IValueEditorProvider<T> inner;

	public StringBridgingEditorProvider(IValueEditorProvider<T> inner) {
		this.inner = inner;
	}

	@Override
	public Node createEditor(ObjectProperty<String> prop) {
		ObjectProperty<T> typed = new SimpleObjectProperty<>(inner.fromString(prop.get()));
		Node node = inner.createEditor(typed);
		typed.addListener((obs, o, n) -> prop.set(inner.toDisplayString(n)));
		return node;
	}

	@Override
	public String toDisplayString(String value) {
		return value != null ? value : "";
	}

	@Override
	public String fromString(String s) {
		return s;
	}

	@Override
	public boolean supportsDialog() {
		return inner.supportsDialog();
	}

	@Override
	public void showDialog(Window owner, ObjectProperty<String> valueProperty) {
		ObjectProperty<T> typed = new SimpleObjectProperty<>(inner.fromString(valueProperty.get()));
		inner.showDialog(owner, typed);
		valueProperty.set(inner.toDisplayString(typed.get()));
	}

	@Override
	public String providerKey() {
		return "string-bridge:" + inner.providerKey();
	}
}
