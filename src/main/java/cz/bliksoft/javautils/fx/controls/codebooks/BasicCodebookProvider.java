package cz.bliksoft.javautils.fx.controls.codebooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.fx.controls.renderers.IconTextListCell;
import javafx.beans.property.Property;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;

/**
 * Base implementation of {@link ICodebookProvider} that holds a flat or
 * hierarchical list of items and provides configurable filtering, display
 * string conversion, and optional cell factory customization.
 *
 * <p>
 * Construct via one of the public constructors, then configure the public
 * fields (or their setters) before attaching to a {@code CodebookField}.
 *
 * @param <T> the type of codebook item
 */
public abstract class BasicCodebookProvider<T> implements ICodebookProvider<T> {

	private List<T> DATA = null;

	/**
	 * Returns the internal data list used as the flat item source.
	 *
	 * @return the data list, or {@code null} if a dynamic {@code dataSource} is
	 *         used instead
	 */
	protected List<T> getData() {
		return DATA;
	}

	/** Root item for hierarchical providers; {@code null} for flat providers. */
	protected T rootItem = null;

	/**
	 * Returns the root item of the hierarchy, or {@code null} for flat providers.
	 *
	 * @return the root item
	 */
	protected T getRoot() {
		return rootItem;
	}

	/** Whether the root item itself is included in the flat data list. */
	protected boolean showRoot;

	/**
	 * Creates a hierarchical provider.
	 *
	 * @param rootItem         the root node of the hierarchy
	 * @param childrenProvider function that returns the children of a node
	 * @param showRoot         {@code true} to include the root itself in the data
	 *                         list
	 */
	public BasicCodebookProvider(T rootItem, Function<T, List<T>> childrenProvider, boolean showRoot) {
		this.rootItem = rootItem;
		this.showRoot = showRoot;
		if (showRoot) {
			DATA = new ArrayList<>(1);
			DATA.add(rootItem);
		} else {
			DATA = childrenProvider.apply(rootItem);
		}
		this.childrenProvider = childrenProvider;
		dataSource = this::getData;
	}

	/**
	 * Creates a flat provider backed by a fixed list.
	 *
	 * @param items the list of items to expose
	 */
	public BasicCodebookProvider(List<T> items) {
		DATA = items;
		dataSource = this::getData;
	}

	/**
	 * Creates a flat provider backed by a live property whose value may change.
	 *
	 * @param itemsProperty property holding the current item list
	 */
	public BasicCodebookProvider(Property<List<T>> itemsProperty) {
		dataSource = itemsProperty::getValue;
	}

	/** Supplier that returns the current list of items; set by all constructors. */
	public Supplier<List<T>> dataSource = null;

	/**
	 * Creates a provider whose item list is evaluated lazily on each call.
	 *
	 * @param dataSource supplier of the item list
	 */
	public BasicCodebookProvider(Supplier<List<T>> dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Function returning the children of a node; non-null only for hierarchical
	 * providers.
	 */
	public Function<T, List<T>> childrenProvider = null;

	/**
	 * Sets the children provider for hierarchical item navigation.
	 *
	 * @param childrenProvider function that returns the children of a given node
	 */
	public void setChildrenProvider(Function<T, List<T>> childrenProvider) {
		this.childrenProvider = childrenProvider;
	}

	/**
	 * Predicate used to filter items by a text token; defaults to case-insensitive
	 * {@code contains}.
	 */
	public BiPredicate<T, String> filter = (T value, String s) -> {
		return value.toString().toLowerCase().contains(s.toLowerCase());
	};

	/**
	 * Replaces the default filter predicate.
	 *
	 * @param filter predicate {@code (item, filterText) -> matches}
	 */
	public void setFilterPredicate(BiPredicate<T, String> filter) {
		this.filter = filter;
	}

	/**
	 * Function that converts an item to its display string; defaults to
	 * {@link Object#toString()}.
	 */
	public Function<T, String> toDisplayString = v -> String.valueOf(v);

	/**
	 * Sets the display string function used in dropdowns and dialogs.
	 *
	 * @param toDisplayString function converting an item to its display label
	 */
	public void setToDisplayString(Function<T, String> toDisplayString) {
		this.toDisplayString = toDisplayString;
	}

	@Override
	public String toDisplayString(T value) {
		return toDisplayString.apply(value);
	}

	/**
	 * Function that converts an item to the string placed in the edit field;
	 * defaults to {@link Object#toString()}.
	 */
	public Function<T, String> toEditString = v -> String.valueOf(v);

	/**
	 * Sets the edit-string function used to populate the text field after
	 * selection.
	 *
	 * @param toEditString function converting an item to its edit text
	 */
	public void setToEditString(Function<T, String> toEditString) {
		this.toEditString = toEditString;
	}

	@Override
	public String toEditString(T value) {
		return toEditString.apply(value);
	}

	/**
	 * Optional function supplying the cell text label (overrides
	 * {@link #toDisplayString} in list cells).
	 */
	public Function<T, String> textProvider = null;

	/**
	 * Sets the cell text provider used inside list/table cell renderers.
	 *
	 * @param textProvider function returning the cell text for an item
	 */
	public void setTextProvider(Function<T, String> textProvider) {
		this.textProvider = textProvider;
	}

	/** Optional function supplying CSS style classes for individual cells. */
	public Function<T, Set<String>> classProvider = null;

	/**
	 * Sets the CSS class provider for per-item cell styling.
	 *
	 * @param classProvider function returning a set of CSS class names for an item
	 */
	public void setClassProvider(Function<T, Set<String>> classProvider) {
		this.classProvider = classProvider;
	}

	/** Optional function supplying an icon image for individual cells. */
	public Function<T, Image> iconProvider = null;

	/**
	 * Sets the icon provider used by list cell renderers.
	 *
	 * @param iconProvider function returning the icon {@link Image} for an item
	 */
	public void setIconProvider(Function<T, Image> iconProvider) {
		this.iconProvider = iconProvider;
	}

	/** Optional function supplying an overlay icon path for individual cells. */
	public Function<T, String> overlayPathProvider = null;

	/**
	 * Sets the overlay icon path provider used by list cell renderers.
	 *
	 * @param overlayPathProvider function returning the overlay icon path for an
	 *                            item
	 */
	public void setOverlayPathProvider(Function<T, String> overlayPathProvider) {
		this.overlayPathProvider = overlayPathProvider;
	}

	/**
	 * Optional additional filter applied on top of the text-based {@link #filter}.
	 */
	public Predicate<T> additionalFilter = null;

	/**
	 * Sets an extra filter predicate applied in addition to the text filter. Use
	 * this to restrict the visible set to items that meet a business rule.
	 *
	 * @param additionalFilter predicate that returns {@code true} for visible items
	 */
	public void setAdditionalFilter(Predicate<T> additionalFilter) {
		this.additionalFilter = additionalFilter;
	}

	@Override
	public T identify(String selectorText, boolean refineIfNotUnique) {
		if (selectorText == null)
			return null;
		String t = selectorText.trim();
		if (t.isEmpty())
			return null;

		List<T> filtered = getData().stream().filter(v -> filter.test(v, selectorText)).collect(Collectors.toList());

		if (filtered.size() == 1)
			return filtered.getFirst();

		return null;
	}

	/**
	 * Installs a custom cell factory on a {@link ListView} if any of the visual
	 * providers ({@link #textProvider}, {@link #iconProvider},
	 * {@link #classProvider}) are set.
	 *
	 * @param list the list view to configure
	 */
	public void setCellFactory(ListView<T> list) {
		if (textProvider != null || classProvider != null || iconProvider != null) {
			list.setCellFactory(
					lv -> new IconTextListCell<T>(textProvider, iconProvider, classProvider, overlayPathProvider));
		}
	}
}
