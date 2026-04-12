package cz.bliksoft.javautils.fx.controls.codebooks.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.fx.controls.codebooks.BasicCodebookProvider;
import cz.bliksoft.javautils.fx.controls.codebooks.ICodebookProvider;
import cz.bliksoft.javautils.fx.controls.codebooks.IFilterableSelector;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TableCodebookPopupProvider<T> extends BasicCodebookProvider<T> {

	private final List<Supplier<TableColumn<T, ?>>> columnFactories = new ArrayList<>();

	public TableCodebookPopupProvider(List<T> items) {
		super(items);
	}

	public TableCodebookPopupProvider(Property<List<T>> itemsProperty) {
		super(itemsProperty);
	}

	public TableCodebookPopupProvider(Supplier<List<T>> dataSource) {
		super(dataSource);
	}

	/** Add a fully configured column (factory called fresh on each popup open). */
	public void addColumn(Supplier<TableColumn<T, ?>> factory) {
		columnFactories.add(factory);
	}

	/** Convenience: add a plain string column. */
	public void addColumn(String header, Function<T, String> valueExtractor) {
		columnFactories.add(() -> {
			TableColumn<T, String> col = new TableColumn<>(header);
			col.setCellValueFactory(cd -> new ReadOnlyStringWrapper(valueExtractor.apply(cd.getValue())));
			return col;
		});
	}

	@Override
	public T identify(String selectorText, boolean refineIfNotUnique) {
		if (selectorText == null || selectorText.isBlank())
			return null;
		List<T> matches = dataSource.get().stream()
				.filter(item -> filter.test(item, selectorText))
				.collect(Collectors.toList());
		return matches.size() == 1 ? matches.get(0) : null;
	}

	@Override
	public Selector<T> createSelector(Consumer<T> onConfirm) {
		return new PopupTableSelector<>(onConfirm, this, columnFactories);
	}

	private static final class PopupTableSelector<T> extends VBox
			implements ICodebookProvider.PopupSelector<T>, IFilterableSelector {

		private final FilteredList<T> filtered;
		private final TableView<T> table;
		private final BasicCodebookProvider<T> base;

		PopupTableSelector(Consumer<T> onConfirm, BasicCodebookProvider<T> base,
				List<Supplier<TableColumn<T, ?>>> columnFactories) {
			this.base = base;

			filtered = new FilteredList<>(
					FXCollections.observableArrayList(base.dataSource.get()),
					base.additionalFilter == null ? s -> true : base.additionalFilter);

			table = new TableView<>(filtered);
			table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

			for (Supplier<TableColumn<T, ?>> factory : columnFactories)
				table.getColumns().add(factory.get());

			Runnable confirmSelected = () -> {
				T sel = table.getSelectionModel().getSelectedItem();
				if (sel != null)
					onConfirm.accept(sel);
			};

			table.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2)
					confirmSelected.run();
			});

			table.setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ENTER) {
					confirmSelected.run();
					e.consume();
				}
			});

			if (!filtered.isEmpty())
				table.getSelectionModel().selectFirst();

			getChildren().add(table);
			setPrefSize(360, 220);
			table.setPrefSize(360, 220);
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
			String t = filterText == null ? "" : filterText.trim();

			filtered.setPredicate(s -> (t.isEmpty() || base.filter.test(s, t))
					&& (base.additionalFilter == null || base.additionalFilter.test(s)));

			if (!filtered.isEmpty()) {
				if (table.getSelectionModel().getSelectedItem() == null)
					table.getSelectionModel().selectFirst();
			} else {
				table.getSelectionModel().clearSelection();
			}
		}
	}
}
