package cz.bliksoft.javautils.fx.controls.codebooks.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.fx.controls.codebooks.BasicCodebookProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
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
	private String dialogTitle = BSAppJFXMessages.getString("Codebook.button.title");

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
		List<T> matches = dataSource.get().stream().filter(
				item -> filter.test(item, selectorText) && (additionalFilter == null || additionalFilter.test(item)))
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
		stage.initOwner(BSAppUI.getDialogOwner(owner));
		stage.setTitle(dialogTitle);
		String iconSpec = IconspecUtils.getMenuIconspec("codebook/dialog/tree");
		if (iconSpec != null) {
			Image dialogIcon = ImageUtils.getImage(iconSpec, false);
			if (dialogIcon != null)
				stage.getIcons().setAll(dialogIcon);
		}

		TextField filterField = new TextField();
		filterField.setPromptText(BSAppJFXMessages.getString("Codebook.button.filter.prompt"));
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

		Button ok = new Button(BSAppJFXMessages.getString("button.ok"));
		Button cancel = new Button(BSAppJFXMessages.getString("button.cancel"));
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

		// the first real candidate, not just the first top-level node - with a filter
		// that is usually a parent kept only for its matching descendants
		Runnable selectFirstCandidate = () -> {
			String t = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase(Locale.ROOT);
			TreeItem<T> first = firstCandidate(tree.getRoot(), t);
			if (first != null) {
				tree.getSelectionModel().select(first);
				int row = tree.getRow(first);
				if (row >= 0)
					tree.scrollTo(row);
			} else {
				tree.getSelectionModel().clearSelection();
			}
		};

		filterField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.ENTER) {
				if (tree.getSelectionModel().getSelectedItem() == null)
					selectFirstCandidate.run();
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

			selectFirstCandidate.run();
		};

		filterField.textProperty().addListener((obs, o, n) -> applyFilter.run());
		applyFilter.run();

		// Fired at most once per dialog open (never per keystroke) - this dialog only
		// ever opens when identify() didn't already resolve a unique value, including
		// the pure-browse case (blank text). Whether blank text (or any text) is
		// actually worth an async lookup is the fetcher's call, not this class's -
		// e.g. a fetcher may still have useful context (from outside this field)
		// even when filterText itself is empty.
		if (supplementalCandidatesAsync != null) {
			supplementalCandidatesAsync.fetch(this, initialFilterText == null ? "" : initialFilterText, results -> {
				for (T item : results)
					tree.getRoot().getChildren().add(new TreeItem<>(item));
			});
		}

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
		boolean selfMatches = !filtering
				|| (filter.test(node, filterLower) && (additionalFilter == null || additionalFilter.test(node)));

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

	/**
	 * First item (depth-first) that is itself a valid choice: matches the filter
	 * text and {@code additionalFilter}; the first top-level node when there is
	 * none.
	 */
	private TreeItem<T> firstCandidate(TreeItem<T> root, String filterLower) {
		TreeItem<T> found = findCandidate(root, filterLower);
		return found != null ? found : firstSelectableChild(root);
	}

	private TreeItem<T> findCandidate(TreeItem<T> item, String filterLower) {
		if (item == null)
			return null;
		for (TreeItem<T> ch : item.getChildren()) {
			if (isCandidate(ch.getValue(), filterLower))
				return ch;
			TreeItem<T> deeper = findCandidate(ch, filterLower);
			if (deeper != null)
				return deeper;
		}
		return null;
	}

	private boolean isCandidate(T value, String filterLower) {
		if (value == null)
			return false;
		boolean filtering = filterLower != null && !filterLower.isEmpty();
		return (!filtering || filter.test(value, filterLower))
				&& (additionalFilter == null || additionalFilter.test(value));
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
