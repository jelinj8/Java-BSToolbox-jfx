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

public abstract class BasicCodebookProvider<T> implements ICodebookProvider<T> {

	private List<T> DATA = null;

	protected List<T> getData() {
		return DATA;
	}

	protected T rootItem = null;

	protected T getRoot() {
		return rootItem;
	}

	protected boolean showRoot;

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

	public BasicCodebookProvider(List<T> items) {
		DATA = items;
		dataSource = this::getData;
	}

	public BasicCodebookProvider(Property<List<T>> itemsProperty) {
		dataSource = itemsProperty::getValue;
	}

	public Supplier<List<T>> dataSource = null;

	public BasicCodebookProvider(Supplier<List<T>> dataSource) {
		this.dataSource = dataSource;
	}

	public Function<T, List<T>> childrenProvider = null;

	public void setChildrenProvider(Function<T, List<T>> childrenProvider) {
		this.childrenProvider = childrenProvider;
	}

	public BiPredicate<T, String> filter = (T value, String s) -> {
		return value.toString().toLowerCase().contains(s.toLowerCase());
	};

	public void setFilterPredicate(BiPredicate<T, String> filter) {
		this.filter = filter;
	}

	public Function<T, String> toDisplayString = v -> String.valueOf(v);

	public void setToDisplayString(Function<T, String> toDisplayString) {
		this.toDisplayString = toDisplayString;
	}

	@Override
	public String toDisplayString(T value) {
		return toDisplayString.apply(value);
	}

	public Function<T, String> toEditString = v -> String.valueOf(v);

	public void setToEditString(Function<T, String> toEditString) {
		this.toEditString = toEditString;
	}

	@Override
	public String toEditString(T value) {
		return toEditString.apply(value);
	}

	public Function<T, String> textProvider = null;

	public void setTextProvider(Function<T, String> textProvider) {
		this.textProvider = textProvider;
	}

	public Function<T, Set<String>> classProvider = null;

	public void setClassProvider(Function<T, Set<String>> classProvider) {
		this.classProvider = classProvider;
	}

	public Function<T, Image> iconProvider = null;

	public void setIconProvider(Function<T, Image> iconProvider) {
		this.iconProvider = iconProvider;
	}

	public Function<T, String> overlayPathProvider = null;

	public void setOverlayPathProvider(Function<T, String> overlayPathProvider) {
		this.overlayPathProvider = overlayPathProvider;
	}

	public Predicate<T> additionalFilter = null;

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

	public void setCellFactory(ListView<T> list) {
		if (textProvider != null || classProvider != null || iconProvider != null) {
			list.setCellFactory(
					lv -> new IconTextListCell<T>(textProvider, iconProvider, classProvider, overlayPathProvider));
		}
	}
}
