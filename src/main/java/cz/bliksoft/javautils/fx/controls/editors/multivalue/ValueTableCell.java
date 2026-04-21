package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.Map;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.controls.editors.ValueEditorFactory;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

/**
 * Display/edit-mode table cell for the value column.
 * Shows display text in display mode. Double-click or ENTER starts inline edit.
 * Resolves the editor type from the registry based on the row's current key.
 */
final class ValueTableCell<V> extends TableCell<KVEntry<V>, V> {

    private final ObjectProperty<Map<String, Class<?>>> registryProperty;
    private final ObjectProperty<IValueEditorProvider<V>> defaultProviderProperty;

    private final ObjectProperty<V> editorProxy = new SimpleObjectProperty<>();

    private KVEntry<V> currentEntry = null;
    private IValueEditorProvider<V> currentProvider = null;

    ValueTableCell(ObjectProperty<Map<String, Class<?>>> registryProperty,
            ObjectProperty<IValueEditorProvider<V>> defaultProviderProperty) {
        this.registryProperty = registryProperty;
        this.defaultProviderProperty = defaultProviderProperty;

        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isEmpty()) startEdit();
        });
    }

    @Override
    public void startEdit() {
        if (isEmpty() || getTableRow() == null || getTableRow().getItem() == null) return;
        if (currentEntry == null) currentEntry = getTableRow().getItem();
        if (currentProvider == null) currentProvider = resolveProvider(currentEntry.key.get());

        super.startEdit();
        editorProxy.set(getItem());

        Node editorNode = currentProvider.createEditor(editorProxy);
        editorNode.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                commitEdit(editorProxy.get());
            } else if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                cancelEdit();
            } else if (e.getCode() == KeyCode.TAB && e.isShiftDown()) {
                e.consume();
                int row = getTableRow() != null ? getTableRow().getIndex() : -1;
                commitEdit(editorProxy.get());
                Platform.runLater(() -> {
                    if (getTableView() != null && row >= 0 && !getTableView().getColumns().isEmpty())
                        getTableView().edit(row, getTableView().getColumns().get(0));
                });
            }
        });

        if (currentProvider.supportsDialog()) {
            Button btn = new Button("\u2026");
            btn.setFocusTraversable(false);
            btn.setOnAction(e -> {
                Window owner = getScene() != null ? getScene().getWindow() : null;
                currentProvider.showDialog(owner, editorProxy);
            });
            HBox box = new HBox(4, editorNode, btn);
            HBox.setHgrow(editorNode, Priority.ALWAYS);
            setGraphic(box);
        } else {
            setGraphic(editorNode);
        }
        setText(null);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        Platform.runLater(editorNode::requestFocus);
    }

    @Override
    public void commitEdit(V newValue) {
        // End edit state before updating model so the extractor-triggered UPDATE
        // event does not cause TableViewSkin to cancel the already-finished edit.
        super.commitEdit(newValue);
        if (currentEntry != null) currentEntry.value.set(newValue);
        showDisplayState(newValue);
        if (getTableView() != null) getTableView().requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        showDisplayState(getItem());
        if (getTableView() != null) getTableView().requestFocus();
    }

    @Override
    protected void updateItem(V item, boolean empty) {
        super.updateItem(item, empty);
        if (isEditing()) return;

        currentEntry = (!empty && getTableRow() != null) ? getTableRow().getItem() : null;
        currentProvider = (currentEntry != null) ? resolveProvider(currentEntry.key.get()) : null;

        if (empty || currentEntry == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        showDisplayState(item);
    }

    private void showDisplayState(V v) {
        if (currentProvider == null) {
            setText("");
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
            return;
        }
        String s = currentProvider.toDisplayString(v);
        if (currentProvider.supportsDialog()) {
            Label label = new Label(s);
            IValueEditorProvider<V> provider = currentProvider;
            KVEntry<V> entry = currentEntry;
            Button btn = new Button("\u2026");
            btn.setFocusTraversable(false);
            btn.setOnAction(e -> {
                Window owner = getScene() != null ? getScene().getWindow() : null;
                ObjectProperty<V> prop = new SimpleObjectProperty<>(v);
                provider.showDialog(owner, prop);
                if (entry != null) entry.value.set(prop.get());
            });
            HBox box = new HBox(4, label, btn);
            HBox.setHgrow(label, Priority.ALWAYS);
            setText(null);
            setGraphic(box);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        } else {
            setText(s);
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
    }

    @SuppressWarnings("unchecked")
    private IValueEditorProvider<V> resolveProvider(String key) {
        Map<String, Class<?>> registry = registryProperty.get();
        if (registry != null && key != null && !key.isBlank()) {
            Class<?> type = registry.get(key);
            if (type != null)
                return (IValueEditorProvider<V>) ValueEditorFactory.forType(type);
        }
        IValueEditorProvider<V> def = defaultProviderProperty.get();
        if (def != null)
            return def;
        return (IValueEditorProvider<V>) ValueEditorFactory.stringProvider();
    }
}
