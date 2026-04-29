package cz.bliksoft.javautils.fx.controls.codebooks.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.fx.controls.codebooks.BasicCodebookProvider;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class TreeCodebookDialogProvider<T> extends BasicCodebookProvider<T> {

	private final List<T> roots;
	private String dialogTitle = BSAppMessages.getString("Codebook.button.title");

	/**
	 * Single hidden root — its children become the top-level visible nodes.
	 * {@code identify()} searches the entire subtree.
	 */
	public TreeCodebookDialogProvider(T root, Function<T, List<T>> cp) {
		super(() -> collectAll(cp.apply(root), cp));
		this.childrenProvider = cp;
		this.roots = cp.apply(root);
	}

	/** Multiple explicit roots shown at the top level. */
	public TreeCodebookDialogProvider(List<T> roots, Function<T, List<T>> cp) {
		super(() -> collectAll(roots, cp));
		this.childrenProvider = cp;
		this.roots = roots;
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

		TreeView<T> tree = new TreeView<>();
		tree.setShowRoot(false);
		tree.setFocusTraversable(true);

		tree.setCellFactory(tv -> new TreeCell<>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(toDisplayString.apply(item));
				}
			}
		});

		Button ok = new Button(BSAppMessages.getString("button.ok"));
		Button cancel = new Button(BSAppMessages.getString("button.cancel"));
		ok.setDefaultButton(true);
		cancel.setCancelButton(true);

		BooleanBinding okDisabled = Bindings.createBooleanBinding(() -> {
			TreeItem<T> sel = tree.getSelectionModel().getSelectedItem();
			return sel == null || sel.getValue() == null;
		}, tree.getSelectionModel().selectedItemProperty());
		ok.disableProperty().bind(okDisabled);

		Runnable confirmAndClose = () -> {
			TreeItem<T> selItem = tree.getSelectionModel().getSelectedItem();
			if (selItem != null && selItem.getValue() != null) {
				onConfirm.accept(selItem.getValue());
				stage.close();
			}
		};

		ok.setOnAction(e -> confirmAndClose.run());
		cancel.setOnAction(e -> stage.close());

		tree.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ENTER) {
				confirmAndClose.run();
				e.consume();
			}
		});

		tree.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2)
				confirmAndClose.run();
		});

		filterField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.ENTER) {
				tree.requestFocus();
				e.consume();
			}
		});

		Runnable applyFilter = () -> {
			String t = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase(Locale.ROOT);

			TreeItem<T> syntheticRoot = new TreeItem<>();
			for (T r : roots) {
				TreeItem<T> child = buildFilteredTree(r, t);
				if (child != null)
					syntheticRoot.getChildren().add(child);
			}

			tree.setRoot(syntheticRoot);
			syntheticRoot.setExpanded(true);

			if (!t.isEmpty())
				expandAll(syntheticRoot);

			TreeItem<T> first = firstSelectableChild(syntheticRoot);
			if (first != null) {
				tree.getSelectionModel().select(first);
			} else {
				tree.getSelectionModel().clearSelection();
			}
		};

		filterField.textProperty().addListener((obs, o, n) -> applyFilter.run());
		applyFilter.run();

		HBox buttons = new HBox(10, ok, cancel);
		buttons.setAlignment(Pos.CENTER_RIGHT);

		BorderPane rootPane = new BorderPane();
		rootPane.setPadding(new Insets(12));
		rootPane.setTop(filterField);
		BorderPane.setMargin(filterField, new Insets(0, 0, 10, 0));
		rootPane.setCenter(tree);
		rootPane.setBottom(buttons);
		BorderPane.setMargin(buttons, new Insets(10, 0, 0, 0));

		Scene scene = new Scene(rootPane, 520, 420);

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

	private TreeItem<T> buildFilteredTree(T node, String filterLower) {
		if (node == null)
			return null;

		boolean filtering = filterLower != null && !filterLower.isEmpty();
		boolean selfMatches = !filtering || filter.test(node, filterLower);

		TreeItem<T> out = new TreeItem<>(node);

		List<T> children = childrenProvider.apply(node);
		if (children != null) {
			for (T child : children) {
				TreeItem<T> childItem = buildFilteredTree(child, filterLower);
				if (childItem != null)
					out.getChildren().add(childItem);
			}
		}

		if (!filtering)
			return out;
		if (selfMatches || !out.getChildren().isEmpty())
			return out;
		return null;
	}

	private static void expandAll(TreeItem<?> item) {
		if (item == null)
			return;
		item.setExpanded(true);
		for (TreeItem<?> ch : item.getChildren())
			expandAll(ch);
	}

	private static <E> TreeItem<E> firstSelectableChild(TreeItem<E> root) {
		if (root == null || root.getChildren().isEmpty())
			return null;
		return root.getChildren().get(0);
	}

	private static <E> List<E> collectAll(List<E> roots, Function<E, List<E>> cp) {
		List<E> result = new ArrayList<>();
		if (roots != null) {
			for (E r : roots)
				collectAllRec(r, cp, result);
		}
		return result;
	}

	private static <E> void collectAllRec(E node, Function<E, List<E>> cp, List<E> out) {
		if (node == null)
			return;
		out.add(node);
		List<E> children = cp.apply(node);
		if (children != null) {
			for (E ch : children)
				collectAllRec(ch, cp, out);
		}
	}
}
