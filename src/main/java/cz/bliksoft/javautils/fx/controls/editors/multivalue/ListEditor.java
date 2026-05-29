package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import cz.bliksoft.javautils.app.ui.actions.ShortcutFileLoader;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.ValueEditorFactory;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Reusable single-column list editor.
 *
 * <p>
 * Each item is displayed as text and edited inline on double-click or ENTER.
 * The editor type is supplied via {@link IValueEditorProvider}. The
 * {@link #getItems() items} list is a live {@link ObservableList} that updates
 * as the user edits.
 *
 * @param <V> the type of items in the list
 */
public class ListEditor<V> extends VBox {

	private final StringProperty title = new SimpleStringProperty();
	private final ObjectProperty<IValueEditorProvider<V>> valueProvider = new SimpleObjectProperty<>();

	private final ObservableList<V> items = FXCollections.observableArrayList();

	// No extractor — extractor-triggered UPDATE events cause TableViewSkin to
	// cancel
	// the active edit. Instead, items is kept in sync via direct per-entry value
	// listeners.
	private final ObservableList<ListEntry<V>> entries = FXCollections.observableArrayList();
	private final Map<ListEntry<V>, ChangeListener<V>> entryValueListeners = new IdentityHashMap<>();

	private final ReadOnlyObjectWrapper<V> selectedItem = new ReadOnlyObjectWrapper<>();
	private TableView<ListEntry<V>> table;

	private Supplier<V> addItemSupplier = null;
	private Runnable editAction = null;
	private Runnable previewAction = null;

	private final KeyCombination kcAdd = loadEditorKey("multivalue-editors/add", KeyCode.INSERT);
	private final KeyCombination kcRemove = loadEditorKey("multivalue-editors/remove", KeyCode.DELETE);
	private final KeyCombination kcPreview = loadEditorKey("multivalue-editors/preview", KeyCode.F3);
	private final Button editBtn = new Button(null, ImageUtils.getIconView("9/EDIT.png", 9));
	private final Button previewBtn = new Button(null, ImageUtils.getIconView("9/INFO.png", 9));

	private HBox toolbar;
	private Node leadingToolbarNode;

	@SuppressWarnings("unchecked")
	public ListEditor() {
		this((IValueEditorProvider<V>) ValueEditorFactory.stringProvider());
	}

	public ListEditor(IValueEditorProvider<V> provider) {
		valueProvider.set(provider);
		setSpacing(4);

		Label titleLabel = new Label();
		titleLabel.getStyleClass().add("ui-title");
		titleLabel.textProperty().bind(title);
		titleLabel.managedProperty().bind(title.isNotEmpty());
		titleLabel.visibleProperty().bind(title.isNotEmpty());

		table = new TableView<>(entries);
		table.setEditable(true);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPrefHeight(100);
		table.setPlaceholder(new Label(""));
		VBox.setVgrow(table, Priority.ALWAYS);
		hideTableHeader(table);

		TableColumn<ListEntry<V>, V> valCol = new TableColumn<>("");
		valCol.setEditable(true);
		valCol.setCellValueFactory(r -> r.getValue().value);
		valCol.setCellFactory(col -> new ListValueCell<>(valueProvider.get()));
		table.getColumns().add(valCol);

		valueProvider.addListener((obs, o, n) -> table.refresh());

		table.getSelectionModel().selectedItemProperty()
				.addListener((obs, o, n) -> selectedItem.set(n != null ? n.value.get() : null));

		Button addBtn = new Button(null, ImageUtils.getIconView("9/ADD.png", 9));
		addBtn.setFocusTraversable(false);
		addBtn.setTooltip(new Tooltip("Přidat"));
		Button delBtn = new Button(null, ImageUtils.getIconView("9/REMOVE.png", 9));
		delBtn.setFocusTraversable(false);
		delBtn.setTooltip(new Tooltip("Odebrat"));
		delBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

		editBtn.setFocusTraversable(false);
		editBtn.setTooltip(new Tooltip("Upravit"));
		editBtn.setVisible(false);
		editBtn.setManaged(false);
		editBtn.setOnAction(e -> {
			if (editAction != null)
				editAction.run();
		});
		editBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

		previewBtn.setFocusTraversable(false);
		previewBtn.setTooltip(new Tooltip("Náhled"));
		previewBtn.setVisible(false);
		previewBtn.setManaged(false);
		previewBtn.setOnAction(e -> firePreview());
		previewBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

		table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ENTER) {
				if (table.getEditingCell() == null) {
					// Not editing — consume and start edit on selected row
					e.consume();
					ListEntry<V> sel = table.getSelectionModel().getSelectedItem();
					if (sel != null)
						table.edit(entries.indexOf(sel), valCol);
				}
				// Editing — let the event through to the editor's own filter (commitEdit)
			} else if (table.getEditingCell() == null) {
				if (kcAdd.match(e)) {
					e.consume();
					addBtn.fire();
				} else if (kcRemove.match(e)) {
					e.consume();
					delBtn.fire();
				} else if (kcPreview.match(e) && previewAction != null
						&& table.getSelectionModel().getSelectedItem() != null) {
					e.consume();
					firePreview();
				}
			}
		});

		addBtn.setOnAction(e -> {
			if (addItemSupplier != null) {
				V item = addItemSupplier.get();
				if (item == null)
					return;
				ListEntry<V> entry = new ListEntry<>(item);
				entries.add(entry);
				table.getSelectionModel().select(entry);
				table.scrollTo(entry);
				table.requestFocus();
			} else {
				ListEntry<V> entry = new ListEntry<>(null);
				entries.add(entry);
				table.getSelectionModel().select(entry);
				table.scrollTo(entry);
				table.requestFocus();
				javafx.application.Platform.runLater(() -> table.edit(entries.indexOf(entry), valCol));
			}
		});
		delBtn.setOnAction(e -> {
			ListEntry<V> sel = table.getSelectionModel().getSelectedItem();
			if (sel != null)
				entries.remove(sel);
			table.requestFocus();
		});

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolbar = new HBox(4, titleLabel, spacer, addBtn, delBtn, editBtn, previewBtn);
		toolbar.setAlignment(Pos.CENTER_LEFT);

		getChildren().addAll(toolbar, table);

		entries.addListener((ListChangeListener<ListEntry<V>>) change -> {
			while (change.next()) {
				if (change.wasPermutated()) {
					for (int i = change.getFrom(); i < change.getTo(); i++)
						items.set(i, entries.get(i).value.get());
				} else {
					int from = change.getFrom();
					for (ListEntry<V> e : change.getRemoved()) {
						ChangeListener<V> l = entryValueListeners.remove(e);
						if (l != null)
							e.value.removeListener(l);
						items.remove(from);
					}
					int i = 0;
					for (ListEntry<V> e : change.getAddedSubList()) {
						items.add(from + i, e.value.get());
						ListEntry<V> cap = e;
						ChangeListener<V> l = (obs, o, n) -> {
							int j = entries.indexOf(cap);
							if (j >= 0)
								items.set(j, n);
						};
						entryValueListeners.put(e, l);
						e.value.addListener(l);
						i++;
					}
				}
			}
		});
	}

	// ---- Public API ----

	/**
	 * When set, the add button calls this supplier instead of inserting a null
	 * entry. Return {@code null} from the supplier to cancel the add (nothing is
	 * inserted).
	 */
	public void setAddItemSupplier(Supplier<V> supplier) {
		addItemSupplier = supplier;
	}

	public void setEditAction(Runnable action) {
		editAction = action;
		editBtn.setVisible(action != null);
		editBtn.setManaged(action != null);
	}

	public void setPreviewAction(Runnable action) {
		previewAction = action;
		previewBtn.setVisible(action != null);
		previewBtn.setManaged(action != null);
	}

	private void firePreview() {
		if (previewAction != null)
			previewAction.run();
	}

	private static KeyCombination loadEditorKey(String key, KeyCode fallback) {
		KeyCombination kc = ShortcutFileLoader.loadFromKeyBindings(key);
		return kc != null ? kc : new KeyCodeCombination(fallback);
	}

	/** Forces all visible cells to re-render with their current values. */
	public void refresh() {
		table.refresh();
	}

	public void loadFrom(Collection<? extends V> source) {
		entries.clear();
		if (source != null)
			source.forEach(v -> entries.add(new ListEntry<>(v)));
	}

	public ObservableList<V> getItems() {
		return items;
	}

	public ReadOnlyObjectProperty<V> selectedItemProperty() {
		return selectedItem.getReadOnlyProperty();
	}

	public V getSelectedItem() {
		return selectedItem.get();
	}

	public void setSelectedItem(V item) {
		entries.stream().filter(e -> Objects.equals(e.value.get(), item)).findFirst()
				.ifPresent(e -> table.getSelectionModel().select(e));
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

	public void addItem(V item) {
		if (item == null)
			return;
		ListEntry<V> entry = new ListEntry<>(item);
		entries.add(entry);
		table.getSelectionModel().select(entry);
		table.scrollTo(entry);
		table.requestFocus();
	}

	public void setLeadingToolbarNode(Node node) {
		if (leadingToolbarNode != null)
			toolbar.getChildren().remove(leadingToolbarNode);
		leadingToolbarNode = node;
		if (node != null)
			toolbar.getChildren().add(2, node);
	}

	public ObjectProperty<IValueEditorProvider<V>> valueProviderProperty() {
		return valueProvider;
	}

	private static void hideTableHeader(TableView<?> table) {
		table.skinProperty().addListener((obs, o, n) -> {
			if (n != null) {
				javafx.scene.Node header = table.lookup("TableHeaderRow");
				if (header instanceof Region r) {
					r.setVisible(false);
					r.setManaged(false);
					r.setMinHeight(0);
					r.setPrefHeight(0);
					r.setMaxHeight(0);
				}
			}
		});
	}
}
