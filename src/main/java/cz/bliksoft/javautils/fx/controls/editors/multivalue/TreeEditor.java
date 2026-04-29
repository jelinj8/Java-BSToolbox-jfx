package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Reusable tree editor with variable, typed node structure.
 *
 * <p>
 * Node types are defined via {@link ITreeNodeType}. Each type controls what
 * child types are allowed, how nodes are displayed, and whether inline rename
 * is supported. The add button adapts to the selected node's allowed child
 * types (shown as a {@link SplitMenuButton} when multiple child types are
 * available).
 *
 * <p>
 * When the selected node's type provides an inline editor that
 * {@link IValueEditorProvider#supportsDialog() supports a dialog}, a "…" button
 * is shown in the toolbar.
 *
 * @param <N> the application node type managed by the tree
 */
public class TreeEditor<N> extends VBox {

	private final StringProperty title = new SimpleStringProperty();
	private final ReadOnlyObjectWrapper<N> selectedItem = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<List<N>> selectedPath = new ReadOnlyObjectWrapper<>(List.of());

	private final Function<N, ITreeNodeType<N>> typeResolver;
	private final TreeView<N> treeView = new TreeView<>();
	private final Map<N, TreeItem<N>> itemMap = new IdentityHashMap<>();

	private final SplitMenuButton addBtn = new SplitMenuButton();
	private final Button delBtn = new Button("\u2013");
	private final Button dialogBtn = new Button("\u2026");

	public TreeEditor(Function<N, ITreeNodeType<N>> typeResolver) {
		this.typeResolver = typeResolver;
		setSpacing(4);

		Label titleLabel = new Label();
		titleLabel.textProperty().bind(title);
		titleLabel.managedProperty().bind(title.isNotEmpty());
		titleLabel.visibleProperty().bind(title.isNotEmpty());

		treeView.setEditable(true);
		treeView.setCellFactory(tv -> new TreeValueCell<>(typeResolver));
		VBox.setVgrow(treeView, Priority.ALWAYS);
		treeView.setPrefHeight(150);

		addBtn.setVisible(false);
		addBtn.setManaged(false);

		delBtn.disableProperty().bind(treeView.getSelectionModel().selectedItemProperty().isNull());

		dialogBtn.setVisible(false);
		dialogBtn.setManaged(false);
		dialogBtn.setFocusTraversable(false);
		dialogBtn.setOnAction(e -> openDialogForSelected());

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox toolbar = new HBox(4, titleLabel, spacer, addBtn, delBtn, dialogBtn);
		toolbar.setAlignment(Pos.CENTER_LEFT);

		getChildren().addAll(toolbar, treeView);

		treeView.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
			if (sel == null || sel.getValue() == null) {
				selectedItem.set(null);
				selectedPath.set(List.of());
			} else {
				selectedItem.set(sel.getValue());
				List<N> path = new ArrayList<>();
				for (TreeItem<N> ti = sel; ti != null; ti = ti.getParent())
					if (ti.getValue() != null)
						path.add(0, ti.getValue());
				selectedPath.set(Collections.unmodifiableList(path));
			}
			updateToolbarButtons(sel);
		});

		treeView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ENTER) {
				e.consume();
				TreeItem<N> sel = treeView.getSelectionModel().getSelectedItem();
				if (sel != null && sel.getValue() != null) {
					ITreeNodeType<N> type = typeResolver.apply(sel.getValue());
					if (type != null && type.inlineEditor() != null)
						treeView.edit(sel);
				}
			}
		});

		delBtn.setOnAction(e -> {
			TreeItem<N> sel = treeView.getSelectionModel().getSelectedItem();
			if (sel == null || sel.getParent() == null)
				return;
			N node = sel.getValue();
			N parent = sel.getParent().getValue();
			if (parent != null) {
				ITreeNodeType<N> parentType = typeResolver.apply(parent);
				if (parentType != null)
					parentType.removeChild(parent, node);
			}
			sel.getParent().getChildren().remove(sel);
			itemMap.remove(node);
		});
	}

	// ---- Public API ----

	public void setRoot(N root) {
		itemMap.clear();
		treeView.setRoot(buildTreeItem(root));
	}

	public StringProperty titleProperty() {
		return title;
	}

	public String getTitle() {
		return title.get();
	}

	public void setTitle(String t) {
		title.set(t);
	}

	public ReadOnlyObjectProperty<N> selectedItemProperty() {
		return selectedItem.getReadOnlyProperty();
	}

	public N getSelectedItem() {
		return selectedItem.get();
	}

	public ReadOnlyObjectProperty<List<N>> selectedPathProperty() {
		return selectedPath.getReadOnlyProperty();
	}

	public List<N> getSelectedPath() {
		return selectedPath.get();
	}

	public void setSelectedItem(N item) {
		TreeItem<N> ti = itemMap.get(item);
		if (ti != null)
			treeView.getSelectionModel().select(ti);
	}

	// ---- Toolbar management ----

	private void updateToolbarButtons(TreeItem<N> sel) {
		updateAddButton(sel);
		updateDialogButton(sel);
	}

	private void updateAddButton(TreeItem<N> sel) {
		if (sel == null || sel.getValue() == null) {
			addBtn.setVisible(false);
			addBtn.setManaged(false);
			return;
		}
		ITreeNodeType<N> type = typeResolver.apply(sel.getValue());
		List<? extends ITreeNodeType<N>> childTypes = type != null ? type.childTypes(sel.getValue()) : List.of();

		if (childTypes.isEmpty()) {
			addBtn.setVisible(false);
			addBtn.setManaged(false);
			return;
		}

		addBtn.setVisible(true);
		addBtn.setManaged(true);
		addBtn.getItems().clear();

		ITreeNodeType<N> firstType = childTypes.get(0);
		TreeItem<N> selFinal = sel;
		addBtn.setOnAction(e -> addChild(selFinal, firstType));
		addBtn.setText(childTypes.size() == 1 ? "+" : "+ " + firstType.getTypeName());

		for (ITreeNodeType<N> ct : childTypes.subList(1, childTypes.size())) {
			MenuItem item = new MenuItem(ct.getTypeName());
			item.setOnAction(e -> addChild(selFinal, ct));
			addBtn.getItems().add(item);
		}
	}

	private void updateDialogButton(TreeItem<N> sel) {
		boolean show = false;
		if (sel != null && sel.getValue() != null) {
			ITreeNodeType<N> type = typeResolver.apply(sel.getValue());
			IValueEditorProvider<N> ed = type != null ? type.inlineEditor() : null;
			show = ed != null && ed.supportsDialog();
		}
		dialogBtn.setVisible(show);
		dialogBtn.setManaged(show);
	}

	// ---- Add / dialog helpers ----

	private void addChild(TreeItem<N> parentItem, ITreeNodeType<N> childType) {
		N parent = parentItem.getValue();
		N child = childType.create();
		ITreeNodeType<N> parentType = typeResolver.apply(parent);
		if (parentType != null)
			parentType.addChild(parent, child);
		TreeItem<N> childItem = buildTreeItem(child); // recurses into pre-populated children
		parentItem.getChildren().add(childItem);
		parentItem.setExpanded(true);
		treeView.getSelectionModel().select(childItem);
		treeView.scrollTo(treeView.getRow(childItem));
	}

	private void openDialogForSelected() {
		TreeItem<N> sel = treeView.getSelectionModel().getSelectedItem();
		if (sel == null || sel.getValue() == null)
			return;
		N node = sel.getValue();
		ITreeNodeType<N> type = typeResolver.apply(node);
		if (type == null)
			return;
		IValueEditorProvider<N> ed = type.inlineEditor();
		if (ed == null || !ed.supportsDialog())
			return;

		ObjectProperty<N> prop = new SimpleObjectProperty<>(node);
		Window owner = getScene() != null ? getScene().getWindow() : null;
		ed.showDialog(owner, prop);
		type.onEditCommitted(prop.get());
		treeView.refresh();
	}

	// ---- Tree building ----

	private TreeItem<N> buildTreeItem(N node) {
		ITreeNodeType<N> type = typeResolver.apply(node);
		TreeItem<N> item = new TreeItem<>(node);
		if (type != null) {
			Node icon = type.createIcon(node);
			if (icon != null)
				item.setGraphic(icon);
			for (N child : type.getChildren(node))
				item.getChildren().add(buildTreeItem(child));
		}
		itemMap.put(node, item);
		return item;
	}
}
