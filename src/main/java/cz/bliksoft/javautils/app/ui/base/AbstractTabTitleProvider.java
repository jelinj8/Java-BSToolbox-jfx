package cz.bliksoft.javautils.app.ui.base;

import cz.bliksoft.javautils.app.ui.interfaces.ITabTitleProvider;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * Convenience base for {@link ITabTitleProvider} implementations. Both methods
 * return {@code null} by default; subclasses override only what they need.
 */
public abstract class AbstractTabTitleProvider implements ITabTitleProvider {

	@Override
	public Property<String> tabTitleProperty() {
		return null;
	}

	@Override
	public ObservableValue<Node> tabGraphicProperty() {
		return null;
	}
}
