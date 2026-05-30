package cz.bliksoft.javautils.app.ui.interfaces;

import javafx.beans.property.Property;

/**
 * Implemented by objects that carry an icon spec string as an observable
 * property.
 *
 * <p>
 * The spec is interpreted by
 * {@link cz.bliksoft.javautils.fx.tools.ImageUtils#getIconNode}: a file name,
 * an inline SVG path prefixed with {@code [P]:}, an overlay chain
 * ({@code a#b}), etc.
 *
 * <p>
 * The return type is {@link Property}{@code <String>} rather than
 * {@link javafx.beans.property.ReadOnlyStringProperty} so that both read-only
 * action properties ({@code SimpleStringProperty}) and writable model
 * properties ({@code StringProperty}) satisfy the interface without a wrapper.
 * Callers that only need to observe treat it as
 * {@link javafx.beans.value.ObservableValue}.
 */
public interface IIconSpecPropertyProvider {

	/**
	 * Returns the observable icon spec property. The value may be {@code null} or
	 * blank when no icon is available.
	 *
	 * @return the icon spec property; never {@code null} itself
	 */
	Property<String> iconSpecProperty();

	/**
	 * Returns the menu-specific icon spec property, or {@code null} to fall back to
	 * {@link #iconSpecProperty()}.
	 */
	default Property<String> menuIconSpecProperty() {
		return null;
	}
}
