package cz.bliksoft.javautils.app.ui.widgets;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;

/**
 * A widget is a self-contained display component that lives inside a
 * {@link WidgetContainer}. Implement this interface to provide the visual
 * content; implement {@link IWidgetFactory} to make the widget discoverable and
 * instantiable by the container.
 */
public interface IWidget {

	/**
	 * Returns the widget's display title (shown in containers that have a title
	 * bar).
	 */
	String getTitle();

	/** Returns the JavaFX node that represents this widget's visual content. */
	Node getComponent();

	/**
	 * Returns an optional context menu that will be shown when the user
	 * right-clicks the widget. Return {@code null} if the widget has no custom
	 * menu. When the container is {@link WidgetContainer#isConfigurable()
	 * configurable}, the container's own management items (Replace/Remove) are
	 * shown instead; the widget's menu is currently unused in that case.
	 */
	default ContextMenu getContextMenu() {
		return null;
	}

	/**
	 * Called by the container when the widget is removed. Release any resources
	 * (timers, listeners, etc.) here.
	 */
	default void cleanup() {
	}
}
