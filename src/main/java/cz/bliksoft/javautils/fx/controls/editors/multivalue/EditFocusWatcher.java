package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Commits an inline cell edit when keyboard focus moves somewhere outside the
 * cell's editor (another control, another row, another table) - without it an
 * edit only ever committed on ENTER, and anything typed was silently dropped as
 * soon as the user clicked elsewhere.
 *
 * <p>
 * Watches the scene's {@link Scene#focusOwnerProperty() focus owner} rather
 * than the editor node's own {@code focused} property on purpose: the latter
 * also turns {@code false} whenever the whole window loses focus - e.g. while a
 * provider's modal dialog (opened from the cell's own "…" button) or some popup
 * is showing - which must not end the edit. The focus owner only changes when
 * focus really moves to another node of the same scene.
 */
final class EditFocusWatcher {

	private Scene scene;
	private ChangeListener<Node> listener;

	/**
	 * Starts watching {@code scene} (replacing any previous watch): the first time
	 * the focus owner becomes a node outside {@code editorRoot}, the watch ends and
	 * {@code onFocusLeft} runs.
	 */
	void watch(Scene scene, Node editorRoot, Runnable onFocusLeft) {
		stop();
		if (scene == null || editorRoot == null)
			return;
		this.scene = scene;
		listener = (obs, o, n) -> {
			if (n != null && !isInside(n, editorRoot)) {
				stop();
				onFocusLeft.run();
			}
		};
		scene.focusOwnerProperty().addListener(listener);
	}

	/** Ends the current watch, if any. Safe to call repeatedly. */
	void stop() {
		if (scene != null && listener != null)
			scene.focusOwnerProperty().removeListener(listener);
		scene = null;
		listener = null;
	}

	private static boolean isInside(Node node, Node root) {
		for (Node p = node; p != null; p = p.getParent())
			if (p == root)
				return true;
		return false;
	}
}
