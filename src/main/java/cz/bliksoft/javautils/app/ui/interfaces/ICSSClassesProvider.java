package cz.bliksoft.javautils.app.ui.interfaces;

import javafx.collections.ObservableList;

/**
 * Implemented by objects that supply a live set of CSS style-class strings to
 * be merged into the visual representation of that object.
 *
 * <p>
 * Multi-value editor cells ({@code TreeValueCell}, {@code ListValueCell}, etc.)
 * and the action-binding layer ({@code ActionBinder}) check for this interface
 * and apply the classes to the cell's or control's {@code styleClass} list,
 * tracking additions and removals via a {@code ListChangeListener}.
 *
 * <p>
 * The returned list must be a <em>stable instance</em> — the same object
 * reference for the lifetime of the implementing object. Only the list's
 * <em>contents</em> may change over time.
 */
public interface ICSSClassesProvider {

	/**
	 * Returns the live list of CSS style-class strings. Never {@code null}.
	 *
	 * @return the observable style-class list
	 */
	ObservableList<String> getCssClasses();
}
