package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.app.ui.actions.IconBinder;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.ShortcutFileLoader;
import cz.bliksoft.javautils.app.ui.interfaces.IIconSpecPropertyProvider;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.ValueEditorFactory;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Node;
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
import javafx.stage.Window;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;

/**
 * Reusable key/value table editor with optional typed property registry.
 *
 * <p>
 * When a {@link #propertyRegistryProperty() propertyRegistry} is set, the key
 * column restricts selection to the defined property names using a codebook
 * popup. The value column editor is resolved per key type via
 * {@link cz.bliksoft.javautils.fx.controls.editors.ValueEditorFactory}.
 *
 * <p>
 * The {@link #getValues() values} map is a live {@link ObservableMap} that
 * updates as the user edits, suitable for binding validation state.
 *
 * @param <V> the type of values in the map
 */
public class KeyValueEditor<V> extends VBox {

	private final StringProperty title = new SimpleStringProperty();
	private final ObjectProperty<Map<String, Class<?>>> propertyRegistry = new SimpleObjectProperty<>();
	private final ObjectProperty<IValueEditorProvider<V>> defaultValueProvider = new SimpleObjectProperty<>();
	private final Map<Class<?>, IValueEditorProvider<V>> typeProviders = new HashMap<>();
	private final SimpleBooleanProperty keysRestrictedToRegistry = new SimpleBooleanProperty(true);
	private final SimpleBooleanProperty inlineEditing = new SimpleBooleanProperty(true);

	private final ObservableMap<String, V> values = FXCollections.observableHashMap();

	private final ObservableList<KVEntry<V>> entries = FXCollections
			.observableArrayList(entry -> new Observable[] { entry.key, entry.value });

	private final Map<KVEntry<V>, ChangeListener<?>[]> entryListeners = new IdentityHashMap<>();

	private final ReadOnlyObjectWrapper<V> selectedValue = new ReadOnlyObjectWrapper<>();

	private Runnable editAction = null;
	private Runnable previewAction = null;
	private IUIAction itemAction = null;

	private final KeyCombination kcAdd = loadEditorKey("multivalue-editors/add", KeyCode.INSERT);
	private final KeyCombination kcRemove = loadEditorKey("multivalue-editors/remove", KeyCode.DELETE);
	private final KeyCombination kcPreview = loadEditorKey("multivalue-editors/preview", KeyCode.F3);

	private final Button addBtn = new Button(null, ImageUtils.getIconView(IconspecUtils.getIconspec("editor/add"))); //$NON-NLS-1$
	private final Button delBtn = new Button(null, ImageUtils.getIconView(IconspecUtils.getIconspec("editor/remove"))); //$NON-NLS-1$
	private final Button editBtn = new Button(null, ImageUtils.getIconView(IconspecUtils.getIconspec("editor/edit"))); //$NON-NLS-1$
	private final Button previewBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("editor/preview"))); //$NON-NLS-1$
	private final Button itemActionBtn = new Button();

	private TableView<KVEntry<V>> table;
	private HBox toolbar;
	private Node leadingToolbarNode;
	private Runnable addAction;
	private Runnable removeAction;
	private final SimpleBooleanProperty keysEditable = new SimpleBooleanProperty(true);

	public KeyValueEditor() {
		this(null);
	}

	@SuppressWarnings("unchecked")
	public KeyValueEditor(IValueEditorProvider<V> defaultProvider) {
		if (defaultProvider != null)
			defaultValueProvider.set(defaultProvider);

		setSpacing(4);

		Label titleLabel = new Label();
		titleLabel.getStyleClass().add("ui-title");
		titleLabel.textProperty().bind(title);
		titleLabel.managedProperty().bind(title.isNotEmpty());
		titleLabel.visibleProperty().bind(title.isNotEmpty());

		table = new TableView<>(entries);
		table.setEditable(true);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPrefHeight(120);
		VBox.setVgrow(table, Priority.ALWAYS);
		hideTableHeader(table);

		// valCol must be declared first so the keyCol cell-factory lambda can capture
		// it.
		TableColumn<KVEntry<V>, V> valCol = new TableColumn<>();
		valCol.setEditable(true);
		valCol.setCellValueFactory(r -> r.getValue().value);
		valCol.setCellFactory(
				col -> new ValueTableCell<>(propertyRegistry, defaultValueProvider, typeProviders, inlineEditing));

		TableColumn<KVEntry<V>, String> keyCol = new TableColumn<>();
		keyCol.setEditable(true);
		keyCol.setCellValueFactory(r -> r.getValue().key);
		keyCol.setCellFactory(
				col -> new KeyTableCell<>(propertyRegistry, keysRestrictedToRegistry, valCol, keysEditable));

		table.getColumns().addAll(keyCol, valCol);

		table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			selectedValue.set(n != null ? n.value.get() : null);
			updateEditButton(n);
		});

		addBtn.setFocusTraversable(false);
		addBtn.setTooltip(new Tooltip(BSAppMessages.getString("editor.button.add")));
		delBtn.setFocusTraversable(false);
		delBtn.setTooltip(new Tooltip(BSAppMessages.getString("editor.button.remove")));
		delBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

		editBtn.setFocusTraversable(false);
		editBtn.setTooltip(new Tooltip(BSAppMessages.getString("editor.button.edit")));
		editBtn.setVisible(false);
		editBtn.setManaged(false);
		editBtn.setOnAction(e -> fireEditAction());
		editBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

		previewBtn.setFocusTraversable(false);
		previewBtn.setTooltip(new Tooltip(BSAppMessages.getString("editor.button.preview")));
		previewBtn.setVisible(false);
		previewBtn.setManaged(false);
		previewBtn.setOnAction(e -> firePreview());
		previewBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

		itemActionBtn.setFocusTraversable(false);
		itemActionBtn.setVisible(false);
		itemActionBtn.setManaged(false);

		addAction = () -> {
			KVEntry<V> entry = new KVEntry<>("", null);
			entries.add(entry);
			table.getSelectionModel().select(entry);
			table.scrollTo(entry);
			table.requestFocus();
			Platform.runLater(() -> table.edit(entries.indexOf(entry), keyCol));
		};
		addBtn.setOnAction(e -> {
			if (addAction != null)
				addAction.run();
		});

		table.setOnMouseClicked(e -> {
			if (e.getClickCount() != 2 || table.getSelectionModel().getSelectedItem() == null)
				return;
			if (itemAction != null && (itemAction.enabledProperty() == null || itemAction.enabledProperty().get())) {
				itemAction.execute();
			} else if (!inlineEditing.get()) {
				fireEditAction();
			} else {
				KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
				IValueEditorProvider<V> provider = resolveProvider(sel.key.get());
				if (provider != null && provider.dialogOnly())
					openDialogForSelected(provider);
			}
		});

		table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (table.getEditingCell() != null)
				return;
			if (e.getCode() == KeyCode.ENTER) {
				e.consume();
				KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
				if (sel == null)
					return;
				if (inlineEditing.get()) {
					String key = sel.key.get();
					IValueEditorProvider<V> provider = resolveProvider(key);
					if (provider != null && provider.dialogOnly()) {
						openDialogForSelected(provider);
					} else {
						int idx = entries.indexOf(sel);
						if (key != null && !key.isBlank())
							table.edit(idx, valCol);
						else
							table.edit(idx, keyCol);
					}
				} else {
					fireEditAction();
				}
			} else if (kcAdd.match(e)) {
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
		});
		removeAction = () -> {
			KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
			if (sel != null)
				entries.remove(sel);
			table.requestFocus();
		};
		delBtn.setOnAction(e -> {
			if (removeAction != null)
				removeAction.run();
		});

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolbar = new HBox(4, titleLabel, spacer, addBtn, delBtn, editBtn, itemActionBtn, previewBtn);
		toolbar.setAlignment(Pos.CENTER_LEFT);

		getChildren().addAll(toolbar, table);

		entries.addListener((ListChangeListener<KVEntry<V>>) change -> {
			while (change.next()) {
				if (change.wasPermutated() || change.wasUpdated())
					continue;
				change.getRemoved().forEach(this::detachEntryListeners);
				change.getAddedSubList().forEach(this::attachEntryListeners);
			}
		});
	}

	// ---- Public API ----

	public void loadFrom(Map<String, V> source) {
		entries.clear();
		values.clear();
		if (source != null)
			source.forEach((k, v) -> entries.add(new KVEntry<>(k, v)));
	}

	public ObservableMap<String, V> getValues() {
		return values;
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

	/**
	 * Sets the action run by the add button, replacing the default (insert a blank
	 * row and start editing its key). Pass {@code null} to hide the button
	 * entirely, e.g. when the set of keys is fixed.
	 */
	public void setAddAction(Runnable action) {
		addAction = action;
		addBtn.setVisible(action != null);
		addBtn.setManaged(action != null);
	}

	/**
	 * Sets the action run by the remove button, replacing the default (remove the
	 * selected row). Pass {@code null} to hide the button entirely, e.g. when the
	 * set of keys is fixed.
	 */
	public void setRemoveAction(Runnable action) {
		removeAction = action;
		delBtn.setVisible(action != null);
		delBtn.setManaged(action != null);
	}

	/**
	 * Sets the action run by the toolbar edit button. Pass {@code null} to hide it.
	 * When {@link #inlineEditingProperty() inlineEditing} is {@code false}, this
	 * action is also invoked on ENTER if the selected row's provider does not
	 * support a dialog.
	 */
	public void setEditAction(Runnable action) {
		editAction = action;
		editBtn.setVisible(action != null);
		editBtn.setManaged(action != null);
	}

	/**
	 * Binds an {@link IUIAction} as the primary item action, triggered by
	 * double-clicking a row or pressing the item-action button that appears in the
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
		var notSelected = table.getSelectionModel().selectedItemProperty().isNull();
		itemActionBtn.disableProperty()
				.bind(action.enabledProperty() != null ? notSelected.or(Bindings.not(action.enabledProperty()))
						: notSelected);
		itemActionBtn.setVisible(true);
		itemActionBtn.setManaged(true);
	}

	/**
	 * Controls whether the key column can be edited. Set to {@code false} when the
	 * set of keys is fixed and only the values may be edited.
	 */
	public void setKeysEditable(boolean editable) {
		keysEditable.set(editable);
	}

	public void setPreviewAction(Runnable action) {
		previewAction = action;
		previewBtn.setVisible(action != null);
		previewBtn.setManaged(action != null);
	}

	/**
	 * Controls whether values can be edited inline in cells. When {@code false},
	 * per-cell edit buttons are hidden and ENTER/double-click delegates to the
	 * provider's dialog (if supported) or the configured
	 * {@link #setEditAction(Runnable) editAction}. Default is {@code true}.
	 */
	public SimpleBooleanProperty inlineEditingProperty() {
		return inlineEditing;
	}

	public void setInlineEditing(boolean value) {
		inlineEditing.set(value);
	}

	public boolean isInlineEditing() {
		return inlineEditing.get();
	}

	public ReadOnlyObjectProperty<V> selectedValueProperty() {
		return selectedValue.getReadOnlyProperty();
	}

	public V getSelectedValue() {
		return selectedValue.get();
	}

	public String getSelectedKey() {
		KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
		return sel != null ? sel.key.get() : null;
	}

	/**
	 * Updates the value of the currently selected entry without changing the
	 * selection.
	 */
	public void updateSelectedValue(V newValue) {
		KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
		if (sel != null)
			sel.value.set(newValue);
	}

	/** Forces all visible cells to re-render with their current values. */
	public void refresh() {
		table.refresh();
	}

	public void setPlaceholderText(String text) {
		table.setPlaceholder(new Label(text));
	}

	public void setLeadingToolbarNode(Node node) {
		if (leadingToolbarNode != null)
			toolbar.getChildren().remove(leadingToolbarNode);
		leadingToolbarNode = node;
		if (node != null)
			toolbar.getChildren().add(2, node);
	}

	public ObjectProperty<Map<String, Class<?>>> propertyRegistryProperty() {
		return propertyRegistry;
	}

	/**
	 * Controls how a non-null {@link #propertyRegistryProperty() propertyRegistry}
	 * is used by the key column. When {@code true} (default), keys are restricted
	 * to the registry's names via a codebook popup. When {@code false}, the
	 * registry's names are offered as popup suggestions only — any typed key is
	 * accepted.
	 */
	public SimpleBooleanProperty keysRestrictedToRegistryProperty() {
		return keysRestrictedToRegistry;
	}

	public void setKeysRestrictedToRegistry(boolean value) {
		keysRestrictedToRegistry.set(value);
	}

	public boolean isKeysRestrictedToRegistry() {
		return keysRestrictedToRegistry.get();
	}

	public ObjectProperty<IValueEditorProvider<V>> defaultValueProviderProperty() {
		return defaultValueProvider;
	}

	public Map<Class<?>, IValueEditorProvider<V>> getTypeProviders() {
		return typeProviders;
	}

	// ---- Internal helpers ----

	private void fireEditAction() {
		if (editAction != null) {
			editAction.run();
			return;
		}
		KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
		if (sel != null) {
			IValueEditorProvider<V> provider = resolveProvider(sel.key.get());
			if (provider != null && provider.supportsDialog())
				openDialogForSelected(provider);
		}
	}

	private void updateEditButton(KVEntry<V> sel) {
		if (editAction != null)
			return;
		boolean show = false;
		if (sel != null) {
			IValueEditorProvider<V> provider = resolveProvider(sel.key.get());
			show = provider != null && provider.supportsDialog();
		}
		editBtn.setVisible(show);
		editBtn.setManaged(show);
	}

	private void firePreview() {
		if (previewAction != null)
			previewAction.run();
	}

	private static KeyCombination loadEditorKey(String key, KeyCode fallback) {
		KeyCombination kc = ShortcutFileLoader.loadFromKeyBindings(key);
		return kc != null ? kc : new KeyCodeCombination(fallback);
	}

	private void openDialogForSelected(IValueEditorProvider<V> provider) {
		KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
		if (sel == null)
			return;
		Window owner = getScene() != null ? getScene().getWindow() : null;
		ObjectProperty<V> prop = new SimpleObjectProperty<>(sel.value.get());
		provider.showDialog(owner, prop);
		sel.value.set(prop.get());
	}

	@SuppressWarnings("unchecked")
	private IValueEditorProvider<V> resolveProvider(String key) {
		Map<String, Class<?>> registry = propertyRegistry.get();
		if (registry != null && key != null && !key.isBlank()) {
			Class<?> type = registry.get(key);
			if (type != null) {
				IValueEditorProvider<V> override = typeProviders.get(type);
				if (override != null)
					return override;
				return (IValueEditorProvider<V>) ValueEditorFactory.forStringType(type);
			}
		}
		IValueEditorProvider<V> def = defaultValueProvider.get();
		if (def != null)
			return def;
		return (IValueEditorProvider<V>) ValueEditorFactory.stringProvider();
	}

	// ---- ObservableMap sync ----

	private void attachEntryListeners(KVEntry<V> entry) {
		ChangeListener<String> keyListener = (obs, oldKey, newKey) -> {
			if (oldKey != null && !oldKey.isBlank())
				values.remove(oldKey);
			if (newKey != null && !newKey.isBlank())
				values.put(newKey, entry.value.get());
		};
		ChangeListener<V> valueListener = (obs, o, n) -> {
			String key = entry.key.get();
			if (key != null && !key.isBlank())
				values.put(key, n);
		};
		entry.key.addListener(keyListener);
		entry.value.addListener(valueListener);
		entryListeners.put(entry, new ChangeListener<?>[] { keyListener, valueListener });

		String key = entry.key.get();
		if (key != null && !key.isBlank())
			values.put(key, entry.value.get());
	}

	private static void hideTableHeader(TableView<?> table) {
		table.skinProperty().addListener((obs, o, n) -> {
			if (n != null) {
				javafx.scene.Node header = table.lookup("TableHeaderRow");
				if (header instanceof javafx.scene.layout.Region r) {
					r.setVisible(false);
					r.setManaged(false);
					r.setMinHeight(0);
					r.setPrefHeight(0);
					r.setMaxHeight(0);
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void detachEntryListeners(KVEntry<V> entry) {
		ChangeListener<?>[] listeners = entryListeners.remove(entry);
		if (listeners != null) {
			entry.key.removeListener((ChangeListener<String>) listeners[0]);
			entry.value.removeListener((ChangeListener<V>) listeners[1]);
		}
		String key = entry.key.get();
		if (key != null && !key.isBlank())
			values.remove(key);
	}
}
