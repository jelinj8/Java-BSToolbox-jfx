package cz.bliksoft.javautils.fx.controls.codebooks;

import java.util.function.Consumer;

import javafx.scene.layout.Region;
import javafx.stage.Window;

public interface ICodebookProvider<T> {

	T identify(String selectorText, boolean refineIfNotUnique);
	
	String toDisplayString(T value);

	default String toEditString(T value) {
	    return toDisplayString(value);
	}
	
	Selector<T> createSelector(Consumer<T> onConfirm);

	sealed interface Selector<T> permits PopupSelector, DialogSelector { }

    /**
     * Popup selector: CodebookField will show this in a PopupControl.
     * If it also implements IFilterableSelector, CodebookField will forward TextField text changes as filter input.
     */
    non-sealed interface PopupSelector<T> extends Selector<T> {
        Region content();

        /** Optional filtering hook; return null if not supported */
        default IFilterableSelector filterable() { return null; }
    }

    /**
     * Dialog selector: provider shows its own dialog/window.
     */
    non-sealed interface DialogSelector<T> extends Selector<T> {
        void show(Window owner, String initialFilterText);
    }
	
}
