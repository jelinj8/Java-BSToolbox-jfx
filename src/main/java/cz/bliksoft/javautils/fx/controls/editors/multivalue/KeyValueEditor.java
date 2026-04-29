package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.IdentityHashMap;
import java.util.Map;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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

	private final ObservableMap<String, V> values = FXCollections.observableHashMap();

	private final ObservableList<KVEntry<V>> entries = FXCollections
			.observableArrayList(entry -> new Observable[] { entry.key, entry.value });

	private final Map<KVEntry<V>, ChangeListener<?>[]> entryListeners = new IdentityHashMap<>();

	private TableView<KVEntry<V>> table;

	public KeyValueEditor() {
		this(null);
	}

	@SuppressWarnings("unchecked")
	public KeyValueEditor(IValueEditorProvider<V> defaultProvider) {
		if (defaultProvider != null)
			defaultValueProvider.set(defaultProvider);

		setSpacing(4);

		Label titleLabel = new Label();
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
		valCol.setCellFactory(col -> new ValueTableCell<>(propertyRegistry, defaultValueProvider));

		TableColumn<KVEntry<V>, String> keyCol = new TableColumn<>();
		keyCol.setEditable(true);
		keyCol.setCellValueFactory(r -> r.getValue().key);
		keyCol.setCellFactory(col -> new KeyTableCell<>(propertyRegistry, valCol));

		table.getColumns().addAll(keyCol, valCol);

		Button addBtn = new Button("+");
		Button delBtn = new Button("\u2013");
		delBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

		addBtn.setOnAction(e -> {
			KVEntry<V> entry = new KVEntry<>("", null);
			entries.add(entry);
			table.getSelectionModel().select(entry);
			table.scrollTo(entry);
			Platform.runLater(() -> table.edit(entries.indexOf(entry), keyCol));
		});

		table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (table.getEditingCell() != null)
				return;
			if (e.getCode() == KeyCode.ENTER) {
				e.consume();
				KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
				if (sel != null) {
					int idx = entries.indexOf(sel);
					String key = sel.key.get();
					// Existing rows go straight to value; new rows (blank key) start at key.
					if (key != null && !key.isBlank())
						table.edit(idx, valCol);
					else
						table.edit(idx, keyCol);
				}
			} else if (e.getCode() == KeyCode.INSERT) {
				e.consume();
				addBtn.fire();
			} else if (e.getCode() == KeyCode.DELETE) {
				e.consume();
				delBtn.fire();
			}
		});
		delBtn.setOnAction(e -> {
			KVEntry<V> sel = table.getSelectionModel().getSelectedItem();
			if (sel != null)
				entries.remove(sel);
		});

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox toolbar = new HBox(4, titleLabel, spacer, addBtn, delBtn);
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

	public ObjectProperty<Map<String, Class<?>>> propertyRegistryProperty() {
		return propertyRegistry;
	}

	public ObjectProperty<IValueEditorProvider<V>> defaultValueProviderProperty() {
		return defaultValueProvider;
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
