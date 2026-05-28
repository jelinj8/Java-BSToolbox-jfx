package cz.bliksoft.javautils.app.ui.interfaces;

import cz.bliksoft.javautils.fx.binding.ObjectStatus;
import javafx.beans.value.ObservableValue;

/**
 * Implemented by model objects that carry an observable {@link ObjectStatus}.
 *
 * <p>
 * Multi-value editor cells ({@code TreeValueCell}, {@code ListValueCell}, etc.)
 * check for this interface on each item and automatically apply a CSS class of
 * the form {@code object-status-<name>} (e.g. {@code object-status-modified})
 * to the cell, removing the previous class on every status change.
 */
public interface IObjectStatusProvider {

	/**
	 * Returns the observable status value. The value may be {@code null} when no
	 * status is available (no CSS class is applied in that case).
	 *
	 * @return the status observable; never {@code null} itself
	 */
	ObservableValue<ObjectStatus> objectStatusProperty();
}
