package cz.bliksoft.javautils.fx.controls.codebooks.providers;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cz.bliksoft.javautils.fx.controls.codebooks.BasicCodebookProvider;
import cz.bliksoft.javautils.fx.controls.codebooks.ICodebookProvider;
import cz.bliksoft.javautils.fx.controls.codebooks.IFilterableSelector;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ListCodebookPopupProvider<T> extends BasicCodebookProvider<T> {

	public ListCodebookPopupProvider(List<T> items) {
		super(items);
	}

	public ListCodebookPopupProvider(Property<List<T>> itemsProperty) {
		super(itemsProperty);
	}

	public ListCodebookPopupProvider(Supplier<List<T>> dataSource) {
		super(dataSource);
	}

	@Override
	public Selector<T> createSelector(Consumer<T> onConfirm) {
		return new PopupFilteredListSelector<T>(onConfirm, this);
	}

	/** Popup content that can be filtered externally. */
	private static final class PopupFilteredListSelector<T> extends VBox
			implements ICodebookProvider.PopupSelector<T>, IFilterableSelector {

		private final FilteredList<T> filtered;
		private final ListView<T> list;

		private final BiPredicate<T, String> filter;

		private final BasicCodebookProvider<T> base;

		PopupFilteredListSelector(Consumer<T> onConfirm, BasicCodebookProvider<T> base) {
			this.base = base;
			this.filtered = new FilteredList<>(FXCollections.observableArrayList(base.dataSource.get()),
					base.additionalFilter == null ? (s -> true) : base.additionalFilter);
			this.list = new ListView<>(filtered);

			base.setCellFactory(list);

			this.filter = base.filter;

			setSpacing(6);
			getChildren().add(list);

			setPrefSize(260, 180);
			list.setPrefSize(260, 180);

			// Confirm only on explicit action (double-click)
			list.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2) {
					T sel = list.getSelectionModel().getSelectedItem();
					if (sel != null)
						onConfirm.accept(sel);
				}
			});

			// Make sure there is always a selection if items exist
			if (!filtered.isEmpty()) {
				list.getSelectionModel().selectFirst();
			}
		}

		@Override
		public Region content() {
			return this;
		}

		@Override
		public IFilterableSelector filterable() {
			return this;
		}

		@Override
		public void setFilterText(String filterText) {
			String t = filterText == null ? "" : filterText.trim().toLowerCase();

			filtered.setPredicate(s -> ((t.isEmpty() || filter.test(s, filterText))
					&& (base.additionalFilter == null || base.additionalFilter.test(s))));

			if (!filtered.isEmpty()) {
				if (list.getSelectionModel().getSelectedItem() == null) {
					list.getSelectionModel().selectFirst();
				}
			} else {
				list.getSelectionModel().clearSelection();
			}
		}
	}
}
