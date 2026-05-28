package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.function.Function;

import cz.bliksoft.javautils.app.ui.interfaces.ICSSClassesProvider;
import cz.bliksoft.javautils.app.ui.interfaces.IObjectStatusProvider;
import cz.bliksoft.javautils.fx.binding.ObjectStatus;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Display/edit-mode tree cell for {@link TreeEditor}.
 *
 * <p>
 * Shows display text (and optional icon) from the node's {@link ITreeNodeType}.
 * Inline editing is available when {@code type.inlineEditor() != null}:
 * double-click or ENTER starts edit; ENTER commits, ESC cancels.
 *
 * <p>
 * The editor node is obtained fresh on each {@code startEdit()} call via
 * {@link IValueEditorProvider#createEditor}, so it may be a simple text field
 * or any composite panel that binds to the node's observable sub-properties.
 *
 * <p>
 * When the item implements {@link IObjectStatusProvider}, a CSS class of the
 * form {@code object-status-<name>} is applied and updated on each status
 * change. When the item implements {@link ICSSClassesProvider}, the returned
 * list's classes are merged into this cell's style-class list and kept in sync
 * via a {@code ListChangeListener}.
 */
final class TreeValueCell<N> extends TreeCell<N> {

	private final Function<N, ITreeNodeType<N>> typeResolver;
	private final Runnable dialogOpener;

	private final ObjectProperty<N> editProxy = new SimpleObjectProperty<>();
	private N originalItem;
	private IValueEditorProvider<N> currentProvider;

	// ---- item-listener tracking ----
	private ObservableValue<ObjectStatus> watchedStatus;
	private ChangeListener<ObjectStatus> statusListener;
	private String appliedStatusClass;
	private ObservableList<String> watchedCssList;
	private ListChangeListener<String> cssListener;

	TreeValueCell(Function<N, ITreeNodeType<N>> typeResolver, Runnable dialogOpener) {
		this.typeResolver = typeResolver;
		this.dialogOpener = dialogOpener;

		setOnMouseClicked(e -> {
			if (e.getClickCount() == 2 && !isEmpty() && getItem() != null) {
				ITreeNodeType<N> type = typeResolver.apply(getItem());
				if (type == null)
					return;
				if (type.inlineEditor() != null)
					startEdit();
				else if (type.supportsDialog())
					dialogOpener.run();
			}
		});
	}

	@Override
	public void startEdit() {
		N item = getItem();
		if (item == null)
			return;
		ITreeNodeType<N> type = typeResolver.apply(item);
		IValueEditorProvider<N> provider = type != null ? type.inlineEditor() : null;
		if (provider == null)
			return;

		super.startEdit();

		originalItem = item;
		currentProvider = provider;
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
		detachItemListeners();
		super.updateItem(item, empty);
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
			return;
		}
		if (!isEditing())
			restoreDisplayState();
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

	private void attachItemListeners(N item) {
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

	// ---- edit helpers ----

	private void applyCurrentEdit() {
		N item = getItem();
		if (item != null) {
			if (currentProvider != null)
				currentProvider.applyEdit(editProxy);
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
