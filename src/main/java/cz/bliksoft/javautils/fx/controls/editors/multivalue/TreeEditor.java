package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.IconBinder;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.ShortcutFileLoader;
import cz.bliksoft.javautils.app.ui.interfaces.IIconSpecPropertyProvider;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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

	private Runnable previewAction = null;
	private IUIAction itemAction = null;

	private final KeyCombination kcAdd = loadEditorKey("multivalue-editors/add", KeyCode.INSERT);
	private final KeyCombination kcRemove = loadEditorKey("multivalue-editors/remove", KeyCode.DELETE);
	private final KeyCombination kcPreview = loadEditorKey("multivalue-editors/preview", KeyCode.F3);

	private HBox toolbar;
	private Node leadingToolbarNode;

	private final Button addSimpleBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("editor/add"))); //$NON-NLS-1$
	private final SplitMenuButton addSplitBtn = new SplitMenuButton();
	private final Button delBtn = new Button(null, ImageUtils.getIconView(IconspecUtils.getIconspec("editor/remove"))); //$NON-NLS-1$
	private final Button dialogBtn = new Button(null, ImageUtils.getIconView(IconspecUtils.getIconspec("editor/edit"))); //$NON-NLS-1$
	private final Button previewBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("editor/preview"))); //$NON-NLS-1$
	private final Button itemActionBtn = new Button();

	public TreeEditor(Function<N, ITreeNodeType<N>> typeResolver) {
		this.typeResolver = typeResolver;
		setSpacing(4);

		Label titleLabel = new Label();
		titleLabel.getStyleClass().add("ui-title");
		titleLabel.textProperty().bind(title);
		titleLabel.managedProperty().bind(title.isNotEmpty());
		titleLabel.visibleProperty().bind(title.isNotEmpty());

		treeView.setEditable(true);
		treeView.setCellFactory(tv -> new TreeValueCell<>(typeResolver, this::openDialogForSelected));
		VBox.setVgrow(treeView, Priority.ALWAYS);
		treeView.setPrefHeight(150);

		addSimpleBtn.setVisible(false);
		addSimpleBtn.setManaged(false);
		addSimpleBtn.setFocusTraversable(false);
		addSimpleBtn.setTooltip(new Tooltip(BSAppJFXMessages.getString("editor.button.add")));
		addSplitBtn.setVisible(false);
		addSplitBtn.setManaged(false);
		addSplitBtn.setFocusTraversable(false);
		addSplitBtn.setGraphic(ImageUtils.getIconView(IconspecUtils.getIconspec("editor/add"))); //$NON-NLS-1$
		addSplitBtn.setTooltip(new Tooltip(BSAppJFXMessages.getString("editor.button.add")));

		delBtn.setFocusTraversable(false);
		delBtn.setTooltip(new Tooltip(BSAppJFXMessages.getString("editor.button.remove")));
		delBtn.disableProperty().bind(treeView.getSelectionModel().selectedItemProperty().isNull());

		dialogBtn.setVisible(false);
		dialogBtn.setManaged(false);
		dialogBtn.setFocusTraversable(false);
		dialogBtn.setTooltip(new Tooltip(BSAppJFXMessages.getString("editor.button.edit")));
		dialogBtn.setOnAction(e -> openDialogForSelected());

		previewBtn.setVisible(false);
		previewBtn.setManaged(false);
		previewBtn.setFocusTraversable(false);
		previewBtn.setTooltip(new Tooltip(BSAppJFXMessages.getString("editor.button.preview")));
		previewBtn.setOnAction(e -> firePreview());
		previewBtn.disableProperty().bind(treeView.getSelectionModel().selectedItemProperty().isNull());

		itemActionBtn.setFocusTraversable(false);
		itemActionBtn.setVisible(false);
		itemActionBtn.setManaged(false);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolbar = new HBox(4, titleLabel, spacer, addSimpleBtn, addSplitBtn, delBtn, dialogBtn, itemActionBtn,
				previewBtn);
		toolbar.setAlignment(Pos.CENTER_LEFT);

		getChildren().addAll(toolbar, treeView);

		treeView.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2 && itemAction != null && selectedItem.get() != null
					&& (itemAction.enabledProperty() == null || itemAction.enabledProperty().get()))
				itemAction.execute();
		});

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
				if (treeView.getEditingItem() != null)
					return; // cell is already editing; its own filter handles ENTER
				e.consume();
				TreeItem<N> sel = treeView.getSelectionModel().getSelectedItem();
				if (sel != null && sel.getValue() != null) {
					ITreeNodeType<N> type = typeResolver.apply(sel.getValue());
					if (type != null && type.inlineEditor() != null)
						treeView.edit(sel);
					else if (type != null && type.supportsDialog())
						openDialogForSelected();
				}
			} else if (treeView.getEditingItem() == null) {
				if (kcAdd.match(e)) {
					TreeItem<N> sel = treeView.getSelectionModel().getSelectedItem();
					if (sel != null && sel.getValue() != null) {
						ITreeNodeType<N> type = typeResolver.apply(sel.getValue());
						List<? extends ITreeNodeType<N>> childTypes = type != null ? type.childTypes(sel.getValue())
								: List.of();
						if (childTypes.size() == 1) {
							e.consume();
							addChild(sel, childTypes.get(0));
						}
					}
				} else if (kcRemove.match(e) && !delBtn.isDisabled()) {
					e.consume();
					delBtn.fire();
				} else if (kcPreview.match(e) && previewAction != null
						&& treeView.getSelectionModel().getSelectedItem() != null) {
					e.consume();
					firePreview();
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
			treeView.requestFocus();
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

	public void setPreviewAction(Runnable action) {
		previewAction = action;
		previewBtn.setVisible(action != null);
		previewBtn.setManaged(action != null);
	}

	/**
	 * Binds an {@link IUIAction} as the primary item action, triggered by
	 * double-clicking a node or pressing the item-action button that appears in the
	 * toolbar when this is set. Pass {@code null} to remove.
	 */
	public void setItemAction(IUIAction action) {
		itemActionBtn.disableProperty().unbind();
		itemAction = action;
		if (action == null) {
			itemActionBtn.setVisible(false);
			itemActionBtn.setManaged(false);
			return;
		}
		itemActionBtn.setOnAction(e -> action.execute());
		if (action instanceof IIconSpecPropertyProvider p)
			IconBinder.bindToolbarIcon(itemActionBtn, p, IconspecUtils.getIconspecSize("edit-button-size", 16));
		else if (action.textProperty() != null)
			itemActionBtn.textProperty().bind(action.textProperty());
		if (action.textProperty() != null) {
			Tooltip tt = new Tooltip();
			tt.textProperty().bind(action.textProperty());
			itemActionBtn.setTooltip(tt);
		}
		var notSelected = selectedItem.getReadOnlyProperty().isNull();
		itemActionBtn.disableProperty()
				.bind(action.enabledProperty() != null ? notSelected.or(Bindings.not(action.enabledProperty()))
						: notSelected);
		itemActionBtn.setVisible(true);
		itemActionBtn.setManaged(true);
	}

	private void firePreview() {
		if (previewAction != null)
			previewAction.run();
	}

	private static KeyCombination loadEditorKey(String key, KeyCode fallback) {
		KeyCombination kc = ShortcutFileLoader.loadFromKeyBindings(key);
		return kc != null ? kc : new KeyCodeCombination(fallback);
	}

	public void setLeadingToolbarNode(Node node) {
		if (leadingToolbarNode != null)
			toolbar.getChildren().remove(leadingToolbarNode);
		leadingToolbarNode = node;
		if (node != null)
			toolbar.getChildren().add(2, node);
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
		addSplitBtn.textProperty().unbind();

		if (sel == null || sel.getValue() == null) {
			addSimpleBtn.setVisible(false);
			addSimpleBtn.setManaged(false);
			addSplitBtn.setVisible(false);
			addSplitBtn.setManaged(false);
			return;
		}
		ITreeNodeType<N> type = typeResolver.apply(sel.getValue());
		List<? extends ITreeNodeType<N>> childTypes = type != null ? type.childTypes(sel.getValue()) : List.of();

		if (childTypes.isEmpty()) {
			addSimpleBtn.setVisible(false);
			addSimpleBtn.setManaged(false);
			addSplitBtn.setVisible(false);
			addSplitBtn.setManaged(false);
			return;
		}

		ITreeNodeType<N> firstType = childTypes.get(0);
		TreeItem<N> selFinal = sel;

		if (childTypes.size() == 1) {
			addSimpleBtn.setOnAction(e -> addChild(selFinal, firstType));
			addSimpleBtn.setVisible(true);
			addSimpleBtn.setManaged(true);
			addSplitBtn.setVisible(false);
			addSplitBtn.setManaged(false);
		} else {
			addSplitBtn.getItems().clear();
			addSplitBtn.setOnAction(e -> addChild(selFinal, firstType));
			var titleProp = firstType.titleProperty();
			if (titleProp != null)
				addSplitBtn.textProperty().bind(titleProp);
			else
				addSplitBtn.setText(firstType.getTitle());
			var graphicObs = firstType.graphicProperty();
			addSplitBtn.setGraphic(graphicObs != null ? graphicObs.getValue() : null);
			for (ITreeNodeType<N> ct : childTypes.subList(1, childTypes.size())) {
				var gObs = ct.graphicProperty();
				MenuItem item = new MenuItem(ct.getTitle(), gObs != null ? gObs.getValue() : null);
				item.setOnAction(e -> addChild(selFinal, ct));
				addSplitBtn.getItems().add(item);
			}
			addSplitBtn.setVisible(true);
			addSplitBtn.setManaged(true);
			addSimpleBtn.setVisible(false);
			addSimpleBtn.setManaged(false);
		}
	}

	private void updateDialogButton(TreeItem<N> sel) {
		boolean show = false;
		if (sel != null && sel.getValue() != null) {
			ITreeNodeType<N> type = typeResolver.apply(sel.getValue());
			show = type != null && type.supportsDialog();
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
		treeView.requestFocus();
	}

	void openDialogForSelected() {
		TreeItem<N> sel = treeView.getSelectionModel().getSelectedItem();
		if (sel == null || sel.getValue() == null)
			return;
		N node = sel.getValue();
		ITreeNodeType<N> type = typeResolver.apply(node);
		if (type == null || !type.supportsDialog())
			return;
		Window owner = getScene() != null ? getScene().getWindow() : null;
		type.showDialog(owner, node);
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
