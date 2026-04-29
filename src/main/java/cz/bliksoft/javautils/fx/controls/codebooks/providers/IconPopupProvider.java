package cz.bliksoft.javautils.fx.controls.codebooks.providers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import cz.bliksoft.javautils.fx.controls.codebooks.ICodebookProvider;
import cz.bliksoft.javautils.fx.controls.codebooks.IFilterableSelector;
import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class IconPopupProvider implements ICodebookProvider<String> {

	private static final String FILE_PREFIX = "[F]:";
	private static final double ICON_SIZE = 16.0;

	private final List<File> rootFolders = new ArrayList<>();

	public IconPopupProvider() {
	}

	public IconPopupProvider(File rootFolder) {
		rootFolders.add(rootFolder);
	}

	public void addFolder(File folder) {
		rootFolders.add(folder);
	}

	@Override
	public String identify(String selectorText, boolean refineIfNotUnique) {
		if (selectorText == null || selectorText.isBlank())
			return null;
		if (selectorText.startsWith(FILE_PREFIX)) {
			File f = new File(selectorText.substring(FILE_PREFIX.length()));
			return (f.exists() && f.isFile() && isIconFile(f)) ? selectorText : null;
		}
		return null;
	}

	@Override
	public String toDisplayString(String value) {
		if (value == null)
			return "";
		if (value.startsWith(FILE_PREFIX))
			return new File(value.substring(FILE_PREFIX.length())).getName();
		return value;
	}

	@Override
	public String toEditString(String value) {
		return toDisplayString(value);
	}

	@Override
	public Selector<String> createSelector(Consumer<String> onConfirm) {
		return new PopupIconSelector(onConfirm, new ArrayList<>(rootFolders));
	}

	private static boolean isIconFile(File f) {
		String name = f.getName().toLowerCase();
		return name.endsWith(".png") || name.endsWith(".svg");
	}

	private static final class PopupIconSelector extends VBox
			implements ICodebookProvider.PopupSelector<String>, IFilterableSelector {

		private final TreeView<File> tree;
		private final List<File> rootFolders;
		private final Consumer<String> onConfirm;

		PopupIconSelector(Consumer<String> onConfirm, List<File> rootFolders) {
			this.onConfirm = onConfirm;
			this.rootFolders = rootFolders;

			tree = new TreeView<>();
			tree.setShowRoot(false);
			tree.setFocusTraversable(true);

			tree.setCellFactory(tv -> new TreeCell<>() {
				@Override
				protected void updateItem(File item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setGraphic(null);
					} else if (item.isDirectory()) {
						setText(item.getName());
						setGraphic(null);
					} else {
						setText(item.getName());
						ImageView iv = ImageUtils.getIconView(FILE_PREFIX + item.getPath(), ICON_SIZE);
						setGraphic(iv);
					}
				}
			});

			tree.setOnMouseClicked(e -> {
				if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
					confirmSelectedFile();
			});

			tree.setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ENTER) {
					if (confirmSelectedFile())
						e.consume();
				}
			});

			getChildren().add(tree);
			setPrefSize(300, 300);
			tree.setPrefSize(300, 300);

			rebuildTree("");
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
			rebuildTree(filterText == null ? "" : filterText.trim());
		}

		/** Returns true if a file was confirmed. */
		private boolean confirmSelectedFile() {
			TreeItem<File> sel = tree.getSelectionModel().getSelectedItem();
			if (sel != null && sel.getValue() != null && sel.getValue().isFile()) {
				onConfirm.accept(FILE_PREFIX + sel.getValue().getPath());
				return true;
			}
			return false;
		}

		private void rebuildTree(String rawFilter) {
			String lower = rawFilter.toLowerCase();

			TreeItem<File> syntheticRoot = new TreeItem<>();
			for (File folder : rootFolders)
				addFiles(syntheticRoot, folder, lower, 0);

			tree.setRoot(syntheticRoot);
			syntheticRoot.setExpanded(true);

			if (!lower.isEmpty())
				expandAll(syntheticRoot);

			TreeItem<File> firstFile = findFirstFile(syntheticRoot);
			if (firstFile != null)
				tree.getSelectionModel().select(firstFile);
			else
				tree.getSelectionModel().clearSelection();
		}

		/**
		 * Recursively adds {@code file} into {@code target}. At level 0 the folder
		 * itself is not shown — its children are merged directly into target, matching
		 * the original IconCBSelector behaviour.
		 */
		private static void addFiles(TreeItem<File> target, File file, String filterLower, int level) {
			if (!file.exists())
				return;

			if (file.isDirectory()) {
				TreeItem<File> node = new TreeItem<>(file);

				File[] dirs = file.listFiles(File::isDirectory);
				if (dirs != null) {
					Arrays.sort(dirs);
					for (File d : dirs)
						addFiles(node, d, filterLower, level + 1);
				}

				File[] files = file.listFiles(f -> isIconFile(f)
						&& (filterLower.isEmpty() || f.getName().toLowerCase().contains(filterLower)));
				if (files != null) {
					Arrays.sort(files);
					for (File f : files)
						node.getChildren().add(new TreeItem<>(f));
				}

				if (!node.getChildren().isEmpty()) {
					if (level == 0) {
						// flatten root folder — add its children directly
						target.getChildren().addAll(node.getChildren());
					} else {
						target.getChildren().add(node);
					}
				}
			} else if (isIconFile(file)) {
				if (filterLower.isEmpty() || file.getName().toLowerCase().contains(filterLower))
					target.getChildren().add(new TreeItem<>(file));
			}
		}

		/**
		 * Returns the first {@link TreeItem} whose value is a file (not a directory).
		 */
		private static TreeItem<File> findFirstFile(TreeItem<File> node) {
			if (node == null)
				return null;
			for (TreeItem<File> child : node.getChildren()) {
				File f = child.getValue();
				if (f != null && f.isFile())
					return child;
				TreeItem<File> found = findFirstFile(child);
				if (found != null)
					return found;
			}
			return null;
		}

		private static void expandAll(TreeItem<File> item) {
			if (item == null)
				return;
			item.setExpanded(true);
			for (TreeItem<File> ch : item.getChildren())
				expandAll(ch);
		}
	}
}
