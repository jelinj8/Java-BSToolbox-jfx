package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.ValueEditorFactory;
import javafx.beans.property.ObjectProperty;
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

	@SuppressWarnings("unchecked")
	public ListEditor() {
		this((IValueEditorProvider<V>) ValueEditorFactory.stringProvider());
	}

	public ListEditor(IValueEditorProvider<V> provider) {
		valueProvider.set(provider);
		setSpacing(4);

		Label titleLabel = new Label();
		titleLabel.textProperty().bind(title);
		titleLabel.managedProperty().bind(title.isNotEmpty());
		titleLabel.visibleProperty().bind(title.isNotEmpty());

		table = new TableView<>(entries);
		table.setEditable(true);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPrefHeight(100);
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

		Button addBtn = new Button("+");
		Button delBtn = new Button("\u2013");
		delBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

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
				if (e.getCode() == KeyCode.INSERT) {
					e.consume();
					addBtn.fire();
				} else if (e.getCode() == KeyCode.DELETE) {
					e.consume();
					delBtn.fire();
				}
			}
		});

		addBtn.setOnAction(e -> {
			ListEntry<V> entry = new ListEntry<>(null);
			entries.add(entry);
			table.getSelectionModel().select(entry);
			table.scrollTo(entry);
			javafx.application.Platform.runLater(() -> table.edit(entries.indexOf(entry), valCol));
		});
		delBtn.setOnAction(e -> {
			ListEntry<V> sel = table.getSelectionModel().getSelectedItem();
			if (sel != null)
				entries.remove(sel);
		});

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox toolbar = new HBox(4, titleLabel, spacer, addBtn, delBtn);
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
