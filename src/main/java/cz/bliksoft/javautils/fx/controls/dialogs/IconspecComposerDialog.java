package cz.bliksoft.javautils.fx.controls.dialogs;

import cz.bliksoft.javautils.app.iconspec.IconspecMessages;
import cz.bliksoft.javautils.fx.controls.editors.iconspec.IconspecComposer;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Wrapper around {@link IconspecComposer} providing both a modal dialog and a
 * non-modal stage variant for toolbar use.
 */
public class IconspecComposerDialog {

	private IconspecComposerDialog() {
	}

	/**
	 * Opens an {@link IconspecComposer} in a modal dialog and returns the resulting
	 * iconspec string when the user confirms, or {@code null} on cancel.
	 */
	public static String showAndWait(Window owner, String existingSpec) {
		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle(IconspecMessages.getString("IconspecComposerDialog.title"));
		dialog.initOwner(owner);

		IconspecComposer composer = new IconspecComposer();
		if (existingSpec != null && !existingSpec.isBlank())
			composer.setIconspec(existingSpec);

		DialogPane pane = dialog.getDialogPane();
		pane.setContent(composer);
		pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		pane.setPrefSize(900, 600);

		// Keep OK button enabled always — empty spec is a valid result
		Button okButton = (Button) pane.lookupButton(ButtonType.OK);
		okButton.setDefaultButton(true);

		dialog.setResultConverter(bt -> bt == ButtonType.OK ? composer.getIconspec() : null);
		return dialog.showAndWait().orElse(null);
	}

	/**
	 * Opens an {@link IconspecComposer} in a non-modal stage. Intended for toolbar
	 * / dev-tool use where the user keeps the window open while working.
	 */
	public static void openAsStage(String existingSpec) {
		IconspecComposer composer = new IconspecComposer();
		if (existingSpec != null && !existingSpec.isBlank())
			composer.setIconspec(existingSpec);

		Stage stage = new Stage();
		stage.setTitle(IconspecMessages.getString("IconspecComposerDialog.title"));
		stage.setScene(new javafx.scene.Scene(composer, 900, 600));
		stage.show();
	}
}
