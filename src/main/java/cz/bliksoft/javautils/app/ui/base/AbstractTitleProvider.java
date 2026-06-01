package cz.bliksoft.javautils.app.ui.base;

import cz.bliksoft.javautils.app.ui.interfaces.ITitleProvider;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * Convenience base for {@link ITitleProvider} implementations. All methods
 * return {@code null} by default; subclasses override only what they need.
 *
 * <p>
 * {@link #getTitle()} delegates to {@link #titleProperty()} so subclasses only
 * need to override {@code titleProperty()} to serve both static and binding
 * consumers.
 */
public abstract class AbstractTitleProvider implements ITitleProvider {

	@Override
	public String getTitle() {
		Property<String> p = titleProperty();
		return p != null ? p.getValue() : null;
	}

	@Override
	public Property<String> titleProperty() {
		return null;
	}

	@Override
	public ObservableValue<Node> graphicProperty() {
		return null;
	}
}
