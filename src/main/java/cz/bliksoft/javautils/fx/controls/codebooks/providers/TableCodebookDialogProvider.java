package cz.bliksoft.javautils.fx.controls.codebooks.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.fx.controls.codebooks.BasicCodebookProvider;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class TableCodebookDialogProvider<T> extends BasicCodebookProvider<T> {

	private final List<Supplier<TableColumn<T, ?>>> columnFactories = new ArrayList<>();
	private String dialogTitle = BSAppMessages.getString("Codebook.button.title");

	public TableCodebookDialogProvider(List<T> items) {
		super(items);
	}

	public TableCodebookDialogProvider(Property<List<T>> itemsProperty) {
		super(itemsProperty);
	}

	public TableCodebookDialogProvider(Supplier<List<T>> dataSource) {
		super(dataSource);
	}

	/** Add a fully configured column (factory called fresh on each dialog open). */
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

	public void setTitle(String title) {
		this.dialogTitle = title;
	}

	@Override
	public T identify(String selectorText, boolean refineIfNotUnique) {
		if (selectorText == null || selectorText.isBlank())
			return null;
		List<T> matches = dataSource.get().stream().filter(item -> filter.test(item, selectorText))
				.collect(Collectors.toList());
		return matches.size() == 1 ? matches.get(0) : null;
	}

	@Override
	public Selector<T> createSelector(Consumer<T> onConfirm) {
		return (DialogSelector<T>) (owner, initialFilterText) -> showDialog(owner, initialFilterText, onConfirm);
	}

	private void showDialog(Window owner, String initialFilterText, Consumer<T> onConfirm) {
		Stage stage = new Stage();
		stage.initModality(Modality.WINDOW_MODAL);
		if (owner != null)
			stage.initOwner(owner);
		stage.setTitle(dialogTitle);

		TextField filterField = new TextField();
		filterField.setPromptText(BSAppMessages.getString("Codebook.button.filter.prompt"));
		filterField.setText(initialFilterText == null ? "" : initialFilterText);

		FilteredList<T> filtered = new FilteredList<>(FXCollections.observableArrayList(dataSource.get()),
				additionalFilter == null ? s -> true : additionalFilter);

		TableView<T> table = new TableView<>(filtered);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

		for (Supplier<TableColumn<T, ?>> factory : columnFactories)
			table.getColumns().add(factory.get());

		Runnable applyFilter = () -> {
			String t = filterField.getText() == null ? "" : filterField.getText().trim();
			filtered.setPredicate(
					s -> (t.isEmpty() || filter.test(s, t)) && (additionalFilter == null || additionalFilter.test(s)));

			if (!filtered.isEmpty()) {
				if (table.getSelectionModel().getSelectedItem() == null)
					table.getSelectionModel().selectFirst();
			} else {
				table.getSelectionModel().clearSelection();
			}
		};

		filterField.textProperty().addListener((obs, o, n) -> applyFilter.run());
		applyFilter.run();

		Button ok = new Button(BSAppMessages.getString("button.ok"));
		Button cancel = new Button(BSAppMessages.getString("button.cancel"));
		ok.setDefaultButton(true);
		cancel.setCancelButton(true);

		ok.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

		Runnable confirmAndClose = () -> {
			T sel = table.getSelectionModel().getSelectedItem();
			if (sel != null) {
				onConfirm.accept(sel);
				stage.close();
			}
		};

		ok.setOnAction(e -> confirmAndClose.run());
		cancel.setOnAction(e -> stage.close());

		table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ENTER) {
				confirmAndClose.run();
				e.consume();
			}
		});

		table.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2)
				confirmAndClose.run();
		});

		filterField.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.ENTER) {
				if (!filtered.isEmpty()) {
					if (table.getSelectionModel().getSelectedItem() == null)
						table.getSelectionModel().selectFirst();
					table.requestFocus();
				}
				e.consume();
			}
		});

		HBox buttons = new HBox(10, ok, cancel);
		buttons.setAlignment(Pos.CENTER_RIGHT);

		BorderPane root = new BorderPane();
		root.setPadding(new Insets(12));
		root.setTop(filterField);
		BorderPane.setMargin(filterField, new Insets(0, 0, 10, 0));
		root.setCenter(table);
		root.setBottom(buttons);
		BorderPane.setMargin(buttons, new Insets(10, 0, 0, 0));

		Scene scene = new Scene(root, 520, 380);

		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				stage.close();
				e.consume();
			}
		});

		stage.setScene(scene);
		stage.show();

		filterField.requestFocus();
		filterField.positionCaret(filterField.getText().length());
	}
}
