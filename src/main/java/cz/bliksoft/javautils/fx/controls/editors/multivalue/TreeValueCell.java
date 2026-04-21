package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.function.Function;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Display/edit-mode tree cell for {@link TreeEditor}.
 *
 * <p>Shows display text (and optional icon) from the node's {@link ITreeNodeType}.
 * Inline editing is available when {@code type.inlineEditor() != null}: double-click
 * or ENTER starts edit; ENTER commits, ESC cancels.
 *
 * <p>The editor node is obtained fresh on each {@code startEdit()} call via
 * {@link IValueEditorProvider#createEditor}, so it may be a simple text field or
 * any composite panel that binds to the node's observable sub-properties.
 */
final class TreeValueCell<N> extends TreeCell<N> {

    private final Function<N, ITreeNodeType<N>> typeResolver;

    private final ObjectProperty<N> editProxy = new SimpleObjectProperty<>();
    private N originalItem;

    TreeValueCell(Function<N, ITreeNodeType<N>> typeResolver) {
        this.typeResolver = typeResolver;

        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isEmpty() && getItem() != null) {
                ITreeNodeType<N> type = typeResolver.apply(getItem());
                if (type != null && type.inlineEditor() != null)
                    startEdit();
            }
        });
    }

    @Override
    public void startEdit() {
        N item = getItem();
        if (item == null) return;
        ITreeNodeType<N> type = typeResolver.apply(item);
        IValueEditorProvider<N> provider = type != null ? type.inlineEditor() : null;
        if (provider == null) return;

        super.startEdit();

        originalItem = item;
        editProxy.set(item);

        Node editNode = provider.createEditor(editProxy);
        editNode.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                applyCurrentEdit();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                cancelEdit();
            }
        });

        setText(null);
        setGraphic(editNode);
        editNode.requestFocus();
    }

    @Override
    public void cancelEdit() {
        editProxy.set(originalItem);
        super.cancelEdit();
        restoreDisplayState();
    }

    @Override
    protected void updateItem(N item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        if (!isEditing())
            restoreDisplayState();
    }

    private void applyCurrentEdit() {
        N item = getItem();
        if (item != null) {
            ITreeNodeType<N> type = typeResolver.apply(item);
            if (type != null)
                type.onEditCommitted(editProxy.get());
        }
        cancelEdit();
    }

    private void restoreDisplayState() {
        N item = getItem();
        if (item == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        ITreeNodeType<N> type = typeResolver.apply(item);
        setText(type != null ? type.getDisplayText(item) : item.toString());
        setGraphic(type != null ? type.createIcon(item) : null);
    }
}
