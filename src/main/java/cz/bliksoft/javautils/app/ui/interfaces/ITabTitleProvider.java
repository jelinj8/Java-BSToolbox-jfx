package cz.bliksoft.javautils.app.ui.interfaces;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * Implemented by tab content nodes that want to advertise their own tab header
 * title and graphic to a containing
 * {@link cz.bliksoft.javautils.app.ui.components.ContextTabbedPane}.
 *
 * <p>
 * Either method may return {@code null} to indicate "no value from this
 * provider"; the pane will then fall back to the {@code defaultTitle} /
 * no-graphic defaults. Explicit {@code CTX_TAB_TITLE} / {@code CTX_TAB_GRAPHIC}
 * context values always override the provider.
 *
 * @see cz.bliksoft.javautils.app.ui.base.AbstractTabTitleProvider
 */
public interface ITabTitleProvider {

	/**
	 * Observable tab title property. The value may be {@code null} when this
	 * provider does not supply a title.
	 *
	 * @return the title property, or {@code null} if not provided
	 */
	Property<String> tabTitleProperty();

	/**
	 * Observable tab graphic. The value may be {@code null} when this provider does
	 * not supply a graphic.
	 *
	 * @return the graphic observable, or {@code null} if not provided
	 */
	ObservableValue<Node> tabGraphicProperty();
}
