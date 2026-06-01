package cz.bliksoft.javautils.app.ui.interfaces;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * Implemented by objects that can supply a human-readable title and an optional
 * graphic — usable for tab headers, choice menus, labels, list cells, dialog
 * titles, and similar contexts.
 *
 * <p>
 * All three methods have default no-op implementations, so at minimum
 * <strong>at least one</strong> must be overridden to be useful:
 * <ul>
 * <li>Override {@link #getTitle()} for a static string (menu items, simple list
 * cells, …).</li>
 * <li>Override {@link #titleProperty()} for a live-bindable title (tab headers,
 * dynamic labels, …); {@link #getTitle()} should then delegate to it.</li>
 * <li>Override {@link #graphicProperty()} to supply an icon alongside the
 * title.</li>
 * </ul>
 *
 * @see cz.bliksoft.javautils.app.ui.base.AbstractTitleProvider
 */
public interface ITitleProvider {

	/** Returns the current title, or {@code null} if none is available. */
	default String getTitle() {
		return null;
	}

	/**
	 * Returns a bindable title property, or {@code null} if this provider only
	 * supports static titles via {@link #getTitle()}.
	 */
	default Property<String> titleProperty() {
		return null;
	}

	/**
	 * Returns an observable graphic node, or {@code null} if this provider does not
	 * supply a graphic.
	 */
	default ObservableValue<Node> graphicProperty() {
		return null;
	}
}
