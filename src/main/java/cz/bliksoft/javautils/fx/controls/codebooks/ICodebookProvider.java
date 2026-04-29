package cz.bliksoft.javautils.fx.controls.codebooks;

import java.util.function.Consumer;

import javafx.scene.layout.Region;
import javafx.stage.Window;

public interface ICodebookProvider<T> {

	/**
	 * Attempts to resolve a single item from the given text.
	 *
	 * @param selectorText      the text typed by the user; may be {@code null}
	 * @param refineIfNotUnique if {@code true}, the provider may show a dialog when
	 *                          the match is not unique
	 *
	 * @return the matched item, or {@code null} if not found or ambiguous
	 */
	T identify(String selectorText, boolean refineIfNotUnique);

	/**
	 * Converts an item to the string displayed in dropdowns and dialogs.
	 *
	 * @param value the item to convert
	 *
	 * @return the display string; never {@code null}
	 */
	String toDisplayString(T value);

	/**
	 * Converts an item to the string placed in the text field after selection.
	 * Defaults to {@link #toDisplayString(Object)}.
	 *
	 * @param value the item to convert
	 *
	 * @return the edit string; never {@code null}
	 */
	default String toEditString(T value) {
		return toDisplayString(value);
	}

	/**
	 * Creates a {@link Selector} that will call {@code onConfirm} when the user
	 * picks an item.
	 *
	 * @param onConfirm callback invoked with the selected item
	 *
	 * @return a popup or dialog selector
	 */
	Selector<T> createSelector(Consumer<T> onConfirm);

	sealed interface Selector<T> permits PopupSelector, DialogSelector {
	}

	/**
	 * Popup selector: CodebookField will show this in a PopupControl. If it also
	 * implements IFilterableSelector, CodebookField will forward TextField text
	 * changes as filter input.
	 *
	 * @param <T> the type of item being selected
	 */
	non-sealed interface PopupSelector<T> extends Selector<T> {
		Region content();

		/** Optional filtering hook; return null if not supported */
		default IFilterableSelector filterable() {
			return null;
		}
	}

	/**
	 * Dialog selector: provider shows its own dialog/window.
	 *
	 * @param <T> the type of item being selected
	 */
	non-sealed interface DialogSelector<T> extends Selector<T> {
		void show(Window owner, String initialFilterText);
	}

}
