package cz.bliksoft.javautils.fx.controls.codebooks;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.function.Consumer;

public class CodebookPopup extends PopupControl {

	private final Region content;

	public CodebookPopup(Region content) {
		this.content = content;

		setAutoHide(true);
		setAutoFix(true);

		content.getStyleClass().add("codebook-popup");
		Scene scene = getScene();
		scene.setRoot(content);

		// ESC always closes
		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				hide();
				e.consume();
			}
		});

		setOnShown(e -> {
			// Scene scene = getScene();

			// Optionally inherit owner stylesheets (recommended so popup matches app)
			Window owner = getOwnerWindow();
			if (owner instanceof Stage st && st.getScene() != null) {
				scene.getStylesheets().setAll(st.getScene().getStylesheets());
			}

			// Add popup-specific stylesheet
			// var css = getClass().getResource("/css/icon-text-cell.css");
			// if (css != null) {
			// String u = css.toExternalForm();
			// if (!scene.getStylesheets().contains(u)) {
			// scene.getStylesheets().add(u);
			// }
			// }

			// Focus: prefer text input, then table, then list, then root
			Platform.runLater(() -> {
				Node filter = findFirstTextInput(content);
				if (filter != null) {
					filter.requestFocus();
					return;
				}
				TableView<?> tv = findFirstTableView(content);
				if (tv != null) {
					tv.requestFocus();
					return;
				}
				ListView<?> lv = findFirstListView(content);
				if (lv != null) {
					lv.requestFocus();
					return;
				}
				content.requestFocus();
			});
		});
	}

	/** ENTER confirms selection from TableView/ListView; respects filter focus. */
	public <T> void installEnterToConfirm(Consumer<T> confirmSelection) {
		Scene scene = getScene();

		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() != KeyCode.ENTER)
				return;

			Node focusOwner = scene.getFocusOwner();

			// If focus is in filter input, ENTER should not confirm; move focus to
			// table/list.
			if (isInTextInput(focusOwner)) {
				@SuppressWarnings("unchecked")
				TableView<T> tv = (TableView<T>) findFirstTableView(content);
				if (tv != null) {
					ensureTableHasSelection(tv);
					tv.requestFocus();
					e.consume();
					return;
				}
				@SuppressWarnings("unchecked")
				ListView<T> lv = (ListView<T>) findFirstListView(content);
				if (lv != null) {
					ensureListHasSelection(lv);
					lv.requestFocus();
					e.consume();
				}
				return;
			}

			// Prefer TableView confirm if present
			@SuppressWarnings("unchecked")
			TableView<T> tv = (TableView<T>) findFirstTableView(content);
			if (tv != null) {
				ensureTableHasSelection(tv);
				T sel = tv.getSelectionModel().getSelectedItem();
				if (sel != null) {
					confirmSelection.accept(sel);
					e.consume();
				}
				return;
			}

			@SuppressWarnings("unchecked")
			ListView<T> lv = (ListView<T>) findFirstListView(content);
			if (lv != null) {
				ensureListHasSelection(lv);
				T sel = lv.getSelectionModel().getSelectedItem();
				if (sel != null) {
					confirmSelection.accept(sel);
					e.consume();
				}
			}
		});
	}

	private static boolean isInTextInput(Node n) {
		while (n != null) {
			if (n instanceof TextInputControl)
				return true;
			n = n.getParent();
		}
		return false;
	}

	private static Node findFirstTextInput(Node root) {
		if (root instanceof TextInputControl)
			return root;
		if (root instanceof Parent p) {
			for (Node c : p.getChildrenUnmodifiable()) {
				Node found = findFirstTextInput(c);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	private static ListView<?> findFirstListView(Node root) {
		if (root instanceof ListView<?> lv)
			return lv;
		if (root instanceof Parent p) {
			for (Node c : p.getChildrenUnmodifiable()) {
				ListView<?> found = findFirstListView(c);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	private static TableView<?> findFirstTableView(Node root) {
		if (root instanceof TableView<?> tv)
			return tv;
		if (root instanceof Parent p) {
			for (Node c : p.getChildrenUnmodifiable()) {
				TableView<?> found = findFirstTableView(c);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	private static <T> void ensureListHasSelection(ListView<T> lv) {
		if (lv.getSelectionModel().getSelectedItem() == null && !lv.getItems().isEmpty()) {
			lv.getSelectionModel().selectFirst();
		}
	}

	private static <T> void ensureTableHasSelection(TableView<T> tv) {
		if (tv.getSelectionModel().getSelectedItem() == null && !tv.getItems().isEmpty()) {
			tv.getSelectionModel().select(0);
		}
	}
}
