package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
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
 * Display/edit-mode table cell for {@link ListEditor}'s value column.
 *
 * <p>
 * Shows a text label in display mode. Double-click or ENTER starts inline edit.
 * When the provider supports a dialog, a "…" button is shown in both display
 * and edit mode. The editor node and graphics are created once and reused for
 * all rows the cell is recycled to.
 */
final class ListValueCell<V> extends TableCell<ListEntry<V>, V> {

	private final IValueEditorProvider<V> provider;

	private final ObjectProperty<V> editorProxy = new SimpleObjectProperty<>();

	private final Node innerEditorNode; // the actual input widget — focused on startEdit
	private final Node editCellGraphic; // innerEditorNode, or HBox(innerEditorNode, dialogBtn)
	private final Node displayCellGraphic; // null when !supportsDialog
	private final Label displayLabel; // null when !supportsDialog

	private V originalValue;
	private ListEntry<V> currentEntry = null;

	ListValueCell(IValueEditorProvider<V> provider) {
		this.provider = provider;

		innerEditorNode = provider.createEditor(editorProxy);
		innerEditorNode.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ENTER) {
				e.consume();
				commitEdit(editorProxy.get());
			} else if (e.getCode() == KeyCode.ESCAPE) {
				e.consume();
				editorProxy.set(originalValue);
				cancelEdit();
			}
		});

		if (provider.supportsDialog()) {
			Button editDialogBtn = new Button("\u2026");
			editDialogBtn.setFocusTraversable(false);
			editDialogBtn.setOnAction(e -> {
				Window owner = getScene() != null ? getScene().getWindow() : null;
				provider.showDialog(owner, editorProxy);
			});
			HBox editBox = new HBox(4, innerEditorNode, editDialogBtn);
			HBox.setHgrow(innerEditorNode, Priority.ALWAYS);
			editCellGraphic = editBox;

			displayLabel = new Label();
			Button displayDialogBtn = new Button("\u2026");
			displayDialogBtn.setFocusTraversable(false);
			displayDialogBtn.setOnAction(e -> {
				Window owner = getScene() != null ? getScene().getWindow() : null;
				provider.showDialog(owner, editorProxy);
			});
			HBox displayBox = new HBox(4, displayLabel, displayDialogBtn);
			HBox.setHgrow(displayLabel, Priority.ALWAYS);
			displayCellGraphic = displayBox;
		} else {
			editCellGraphic = innerEditorNode;
			displayLabel = null;
			displayCellGraphic = null;
		}

		setOnMouseClicked(e -> {
			if (e.getClickCount() == 2 && !isEmpty())
				startEdit();
		});
	}

	@Override
	public void startEdit() {
		if (isEmpty())
			return;
		originalValue = getItem();
		super.startEdit();
		setText(null);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		setGraphic(editCellGraphic);
		Platform.runLater(innerEditorNode::requestFocus);
	}

	@Override
	public void cancelEdit() {
		editorProxy.set(originalValue);
		super.cancelEdit();
		showDisplayState(originalValue);
		if (getTableView() != null)
			getTableView().requestFocus();
	}

	@Override
	public void commitEdit(V v) {
		super.commitEdit(v);
		showDisplayState(v);
		if (getTableView() != null)
			getTableView().requestFocus();
	}

	@Override
	protected void updateItem(V item, boolean empty) {
		super.updateItem(item, empty);

		// Don't disrupt proxy binding or graphics while the user is actively editing.
		// The extractor on `entries` fires an UPDATE event on every keypress (value
		// change),
		// which would otherwise tear down and re-establish the bidirectional binding
		// mid-edit.
		if (isEditing())
			return;

		if (currentEntry != null) {
			editorProxy.unbindBidirectional(currentEntry.value);
			currentEntry = null;
		}

		if (empty || getTableRow() == null || getTableRow().getItem() == null) {
			setText(null);
			setGraphic(null);
			return;
		}

		currentEntry = getTableRow().getItem();
		editorProxy.set(currentEntry.value.get());
		editorProxy.bindBidirectional(currentEntry.value);
		showDisplayState(item);
	}

	private void showDisplayState(V v) {
		String s = provider.toDisplayString(v);
		if (provider.supportsDialog()) {
			displayLabel.setText(s);
			setText(null);
			setGraphic(displayCellGraphic);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		} else {
			setText(s);
			setGraphic(null);
			setContentDisplay(ContentDisplay.TEXT_ONLY);
		}
	}
}
