package cz.bliksoft.javautils.fx.controls.codebooks.providers;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.fx.controls.codebooks.BasicCodebookProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.scene.image.Image;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ListCodebookDialogProvider<T> extends BasicCodebookProvider<T> {

	public ListCodebookDialogProvider(List<T> items) {
		super(items);
	}

	public ListCodebookDialogProvider(Property<List<T>> itemsProperty) {
		super(itemsProperty);
	}

	public ListCodebookDialogProvider(Supplier<List<T>> dataSource) {
		super(dataSource);
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
		stage.setTitle(BSAppJFXMessages.getString("Codebook.button.title"));
		String iconSpec = IconspecUtils.getMenuIconspec("codebook/dialog/list");
		if (iconSpec != null) {
			Image dialogIcon = ImageUtils.getImage(iconSpec, false);
			if (dialogIcon != null)
				stage.getIcons().setAll(dialogIcon);
		}

		TextField filterField = new TextField();
		filterField.setPromptText(BSAppJFXMessages.getString("Codebook.button.filter.prompt"));
		filterField.setText(initialFilterText == null ? "" : initialFilterText);

		Predicate<T> initialPred = additionalFilter != null ? additionalFilter : s -> true;
		FilteredList<T> filtered = new FilteredList<>(FXCollections.observableArrayList(dataSource.get()), initialPred);

		ListView<T> list = new ListView<>(filtered);
		list.setPrefHeight(260);

		setCellFactory(list);

		// Apply filter (contains, case-insensitive)
		Runnable applyFilter = () -> {
			String t = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();

			filtered.setPredicate(s -> ((t.isEmpty() || filter.test(s, t))
					&& (additionalFilter == null || additionalFilter.test(s))));

			if (!filtered.isEmpty()) {
				if (list.getSelectionModel().getSelectedItem() == null) {
					list.getSelectionModel().selectFirst();
				}
			} else {
				list.getSelectionModel().clearSelection();
			}
		};

		filterField.textProperty().addListener((obs, o, n) -> applyFilter.run());
		applyFilter.run();

		Button ok = new Button(BSAppJFXMessages.getString("button.ok"));
		Button cancel = new Button(BSAppJFXMessages.getString("button.cancel"));
		ok.setDefaultButton(true);
		cancel.setCancelButton(true);

		ok.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());

		Runnable confirmAndClose = () -> {
			T sel = list.getSelectionModel().getSelectedItem();
			if (sel != null) {
				onConfirm.accept(sel); // ✅ confirm only
				stage.close();
			}
		};

		ok.setOnAction(e -> confirmAndClose.run());
		cancel.setOnAction(e -> stage.close());

		// ENTER confirms when list focused; ESC cancels
		list.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				confirmAndClose.run();
				e.consume();
			}
		});

		// Double-click confirms
		list.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				confirmAndClose.run();
			}
		});

		// UX: DOWN from filter moves into list
		filterField.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.DOWN) {
				if (!filtered.isEmpty()) {
					if (list.getSelectionModel().getSelectedItem() == null) {
						list.getSelectionModel().selectFirst();
					}
					list.requestFocus();
				}
				e.consume();
			} else if (e.getCode() == KeyCode.ENTER) {
				// If user presses ENTER in filter, move focus to list (don’t confirm
				// immediately)
				if (!filtered.isEmpty()) {
					if (list.getSelectionModel().getSelectedItem() == null) {
						list.getSelectionModel().selectFirst();
					}
					list.requestFocus();
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
		root.setCenter(list);
		root.setBottom(buttons);
		BorderPane.setMargin(buttons, new Insets(10, 0, 0, 0));

		Scene scene = new Scene(root, 420, 360);

		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				stage.close();
				e.consume();
			}
		});

		stage.setScene(scene);
		stage.show();

		// initial focus
		filterField.requestFocus();
		filterField.positionCaret(filterField.getText().length());
	}
}
