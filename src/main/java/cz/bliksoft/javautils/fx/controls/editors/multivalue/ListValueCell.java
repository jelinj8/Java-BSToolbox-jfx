package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import cz.bliksoft.javautils.app.ui.interfaces.ICSSClassesProvider;
import cz.bliksoft.javautils.app.ui.interfaces.IObjectStatusProvider;
import cz.bliksoft.javautils.fx.binding.ObjectStatus;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
 *
 * <p>
 * When the item implements {@link IObjectStatusProvider}, a CSS class of the
 * form {@code object-status-<name>} is applied and updated on each status
 * change. When the item implements {@link ICSSClassesProvider}, the returned
 * list's classes are merged into this cell's style-class list and kept in sync
 * via a {@code ListChangeListener}.
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

	// ---- item-listener tracking ----
	private ObservableValue<ObjectStatus> watchedStatus;
	private ChangeListener<ObjectStatus> statusListener;
	private String appliedStatusClass;
	private ObservableList<String> watchedCssList;
	private ListChangeListener<String> cssListener;

	ListValueCell(IValueEditorProvider<V> provider) {
		this.provider = provider;

		innerEditorNode = provider.createEditor(editorProxy);
		innerEditorNode.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ENTER) {
				e.consume();
				provider.applyEdit(editorProxy);
				commitEdit(editorProxy.get());
			} else if (e.getCode() == KeyCode.ESCAPE) {
				e.consume();
				editorProxy.set(originalValue);
				cancelEdit();
			}
		});

		if (provider.supportsDialog()) {
			Button editDialogBtn = new Button(null, ImageUtils.getIconView(IconspecUtils.getIconspec("editor/edit")));
			editDialogBtn.setFocusTraversable(false);
			editDialogBtn.setOnAction(e -> {
				Window owner = getScene() != null ? getScene().getWindow() : null;
				provider.showDialog(owner, editorProxy);
			});
			HBox editBox = new HBox(4, innerEditorNode, editDialogBtn);
			HBox.setHgrow(innerEditorNode, Priority.ALWAYS);
			editCellGraphic = editBox;

			displayLabel = new Label();
			Button displayDialogBtn = new Button(null,
					ImageUtils.getIconView(IconspecUtils.getIconspec("editor/edit")));
			displayDialogBtn.setFocusTraversable(false);
			displayDialogBtn.setOnAction(e -> {
				Window owner = getScene() != null ? getScene().getWindow() : null;
				provider.showDialog(owner, editorProxy);
				// Display-mode dialog: apply result immediately (dialog is its own confirm).
				if (currentEntry != null) {
					currentEntry.value.set(editorProxy.get());
					showDisplayState(editorProxy.get());
				}
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
		detachItemListeners();
		super.updateItem(item, empty);

		// Don't disrupt the edit session if the cell is recycled mid-edit.
		if (isEditing())
			return;

		currentEntry = null;

		if (empty || getTableRow() == null || getTableRow().getItem() == null) {
			setText(null);
			setGraphic(null);
			return;
		}

		currentEntry = getTableRow().getItem();
		editorProxy.set(currentEntry.value.get());
		showDisplayState(item);
		attachItemListeners(item);
	}

	// ---- item-listener lifecycle ----

	private void detachItemListeners() {
		if (watchedStatus != null) {
			watchedStatus.removeListener(statusListener);
			watchedStatus = null;
			statusListener = null;
		}
		if (appliedStatusClass != null) {
			getStyleClass().remove(appliedStatusClass);
			appliedStatusClass = null;
		}
		if (watchedCssList != null) {
			watchedCssList.removeListener(cssListener);
			getStyleClass().removeAll(watchedCssList);
			watchedCssList = null;
			cssListener = null;
		}
	}

	private void attachItemListeners(V item) {
		if (item instanceof IObjectStatusProvider sp) {
			watchedStatus = sp.objectStatusProperty();
			statusListener = (obs, o, n) -> applyStatusClass(n);
			watchedStatus.addListener(statusListener);
			applyStatusClass(watchedStatus.getValue());
		}
		if (item instanceof ICSSClassesProvider cp) {
			watchedCssList = cp.getCssClasses();
			cssListener = change -> {
				while (change.next()) {
					if (change.wasRemoved())
						getStyleClass().removeAll(change.getRemoved());
					if (change.wasAdded())
						getStyleClass().addAll(change.getAddedSubList());
				}
			};
			watchedCssList.addListener(cssListener);
			getStyleClass().addAll(watchedCssList);
		}
	}

	private void applyStatusClass(ObjectStatus status) {
		if (appliedStatusClass != null) {
			getStyleClass().remove(appliedStatusClass);
			appliedStatusClass = null;
		}
		if (status != null) {
			appliedStatusClass = "object-status-" + status.name().toLowerCase().replace('_', '-');
			getStyleClass().add(appliedStatusClass);
		}
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
