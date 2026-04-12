package cz.bliksoft.javautils.fx.controls.codebooks.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.fx.controls.codebooks.BasicCodebookProvider;
import cz.bliksoft.javautils.fx.controls.codebooks.ICodebookProvider;
import cz.bliksoft.javautils.fx.controls.codebooks.IFilterableSelector;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TreeCodebookPopupProvider<T> extends BasicCodebookProvider<T> {

	private final List<T> roots;

	/** Single hidden root — its children become the top-level visible nodes. */
	public TreeCodebookPopupProvider(T root, Function<T, List<T>> cp) {
		super(() -> collectAll(cp.apply(root), cp));
		this.childrenProvider = cp;
		this.roots = cp.apply(root);
	}

	/** Multiple explicit roots shown at the top level. */
	public TreeCodebookPopupProvider(List<T> roots, Function<T, List<T>> cp) {
		super(() -> collectAll(roots, cp));
		this.childrenProvider = cp;
		this.roots = roots;
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
		return new PopupTreeSelector<>(onConfirm, this, roots);
	}

	private static final class PopupTreeSelector<T> extends VBox
			implements ICodebookProvider.PopupSelector<T>, IFilterableSelector {

		private final TreeView<T> tree;
		private final List<T> roots;
		private final BasicCodebookProvider<T> base;

		PopupTreeSelector(Consumer<T> onConfirm, BasicCodebookProvider<T> base, List<T> roots) {
			this.base = base;
			this.roots = roots;

			tree = new TreeView<>();
			tree.setShowRoot(false);
			tree.setFocusTraversable(true);

			tree.setCellFactory(tv -> new TreeCell<>() {
				@Override
				protected void updateItem(T item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
					} else {
						setText(base.toDisplayString.apply(item));
					}
				}
			});

			Runnable confirmSelected = () -> {
				TreeItem<T> sel = tree.getSelectionModel().getSelectedItem();
				if (sel != null && sel.getValue() != null)
					onConfirm.accept(sel.getValue());
			};

			tree.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2)
					confirmSelected.run();
			});

			tree.setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ENTER) {
					confirmSelected.run();
					e.consume();
				}
			});

			getChildren().add(tree);
			setPrefSize(260, 220);
			tree.setPrefSize(260, 220);

			applyFilter("");
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
			applyFilter(filterText == null ? "" : filterText.trim());
		}

		private void applyFilter(String rawFilter) {
			String lower = rawFilter.toLowerCase(Locale.ROOT);
			String filterArg = lower.isEmpty() ? null : lower;

			TreeItem<T> syntheticRoot = new TreeItem<>();
			for (T r : roots) {
				TreeItem<T> child = buildFilteredTree(r, filterArg, base);
				if (child != null)
					syntheticRoot.getChildren().add(child);
			}

			tree.setRoot(syntheticRoot);
			syntheticRoot.setExpanded(true);

			if (filterArg != null)
				expandAll(syntheticRoot);

			TreeItem<T> first = firstSelectableChild(syntheticRoot);
			if (first != null) {
				tree.getSelectionModel().select(first);
			} else {
				tree.getSelectionModel().clearSelection();
			}
		}

		private static <T> TreeItem<T> buildFilteredTree(T node, String filterLower,
				BasicCodebookProvider<T> base) {
			if (node == null)
				return null;

			boolean filtering = filterLower != null;
			boolean selfMatches = !filtering || base.filter.test(node, filterLower);

			TreeItem<T> out = new TreeItem<>(node);

			List<T> children = base.childrenProvider.apply(node);
			if (children != null) {
				for (T child : children) {
					TreeItem<T> childItem = buildFilteredTree(child, filterLower, base);
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

		private static <T> TreeItem<T> firstSelectableChild(TreeItem<T> root) {
			if (root == null || root.getChildren().isEmpty())
				return null;
			return root.getChildren().get(0);
		}
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
