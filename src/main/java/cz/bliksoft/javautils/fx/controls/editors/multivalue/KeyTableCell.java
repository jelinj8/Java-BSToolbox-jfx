package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import cz.bliksoft.javautils.fx.controls.codebooks.CodebookField;
import cz.bliksoft.javautils.fx.controls.codebooks.providers.ListCodebookPopupProvider;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Display/edit-mode table cell for the key column.
 * Shows the key as text in display mode. Double-click or ENTER starts inline edit.
 * On commit, focus moves to the value column of the same row.
 */
final class KeyTableCell<V> extends TableCell<KVEntry<V>, String> {

    private final ObjectProperty<Map<String, Class<?>>> registryProperty;
    private final TableColumn<KVEntry<V>, ?> valueColumn;

    private KVEntry<V> currentEntry = null;
    private String originalKey;

    KeyTableCell(ObjectProperty<Map<String, Class<?>>> registryProperty,
                 TableColumn<KVEntry<V>, ?> valueColumn) {
        this.registryProperty = registryProperty;
        this.valueColumn = valueColumn;

        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isEmpty()) startEdit();
        });
    }

    @Override
    public void startEdit() {
        if (isEmpty() || getTableRow() == null || getTableRow().getItem() == null) return;
        originalKey = getItem() != null ? getItem() : "";
        super.startEdit();

        Map<String, Class<?>> registry = registryProperty.get();
        if (registry != null) {
            CodebookField<String> field = new CodebookField<>(
                    new ListCodebookPopupProvider<>(List.copyOf(registry.keySet())));
            field.setMaxWidth(Double.MAX_VALUE);
            if (!originalKey.isBlank()) field.setValue(originalKey);
            field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) { e.consume(); cancelEdit(); }
                else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.TAB) {
                    e.consume();
                    doCommit(field.getValue());
                }
            });
            field.valueProperty().addListener((obs, o, n) -> {
                if (isEditing()) doCommit(n);
            });
            setText(null);
            setGraphic(field);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            Platform.runLater(field::requestFocus);
        } else {
            TextField tf = new TextField(originalKey);
            tf.setMaxWidth(Double.MAX_VALUE);
            tf.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) { e.consume(); cancelEdit(); }
                else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.TAB) {
                    e.consume();
                    doCommit(tf.getText());
                }
            });
            setText(null);
            setGraphic(tf);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            Platform.runLater(tf::requestFocus);
        }
    }

    private void doCommit(String value) {
        commitEdit(value != null ? value : "");
    }

    @Override
    public void commitEdit(String newKey) {
        // End edit state before updating model so the extractor-triggered UPDATE
        // event does not cause TableViewSkin to cancel the already-finished edit.
        super.commitEdit(newKey);
        String k = newKey != null ? newKey : "";
        if (currentEntry != null && !Objects.equals(currentEntry.key.get(), k))
            currentEntry.key.set(k);
        showDisplayState(newKey);
        // Move focus to the value cell of the same row.
        int row = getTableRow() != null ? getTableRow().getIndex() : -1;
        Platform.runLater(() -> {
            TableView<KVEntry<V>> tv = getTableView();
            if (tv != null && row >= 0) tv.edit(row, valueColumn);
        });
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        showDisplayState(getItem());
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (isEditing()) return;

        currentEntry = (!empty && getTableRow() != null) ? getTableRow().getItem() : null;

        if (empty || currentEntry == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        showDisplayState(item);
    }

    private void showDisplayState(String key) {
        setText(key != null ? key : "");
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }
}
