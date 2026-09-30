package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

/**
 * Gives the keyboard focus to an inline cell editor that has just been created
 * and placed - once it is ready for it.
 *
 * <p>
 * A cell's editor gets its skin only in the next pulse, after a plain
 * {@code Platform.runLater(editor::requestFocus)} has already run. Most
 * controls don't mind, but an editable {@code ComboBox} tells its text field
 * to draw the caret only when its own focus <em>changes</em> while the skin is
 * there: focused that early it takes typing and never shows where. So a
 * control without a skin is focused when the skin arrives.
 */
final class EditorFocus {

	private EditorFocus() {
	}

	/** Focuses {@code editor} in a later run of the FX thread, skin permitting. */
	static void requestLater(Node editor) {
		Platform.runLater(() -> request(editor));
	}

	/** Focuses {@code editor} now, or as soon as it has a skin. */
	static void request(Node editor) {
		if (!(editor instanceof Control control) || control.getSkin() != null) {
			editor.requestFocus();
			return;
		}
		control.skinProperty().addListener(new ChangeListener<Skin<?>>() {
			@Override
			public void changed(ObservableValue<? extends Skin<?>> obs, Skin<?> old, Skin<?> skin) {
				if (skin != null) {
					control.skinProperty().removeListener(this);
					// not if the edit ended meanwhile and the editor left the scene
					if (control.getScene() != null)
						control.requestFocus();
				}
			}
		});
	}
}
