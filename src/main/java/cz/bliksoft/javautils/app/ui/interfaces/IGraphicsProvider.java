package cz.bliksoft.javautils.app.ui.interfaces;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * Implemented by objects that carry a pre-built JavaFX {@link Node} graphic as
 * an observable value.
 *
 * <p>
 * Used as a fallback in the UI-action binding layer when no
 * {@link IIconSpecPropertyProvider} is available. The return type is
 * {@link ObservableValue}{@code <Node>} to accommodate both read-only
 * ({@code ReadOnlyObjectProperty}) and writable ({@code SimpleObjectProperty})
 * implementations.
 */
public interface IGraphicsProvider {

	/**
	 * Returns the observable graphic property. The value may be {@code null} when
	 * no graphic is available.
	 *
	 * @return the graphic observable; never {@code null} itself
	 */
	ObservableValue<Node> graphicsProperty();
}
