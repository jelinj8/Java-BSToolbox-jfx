package cz.bliksoft.javautils.fx.controls.images;

import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.fx.customization.BSButtonTypes;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.image.WritableImage;
import javafx.stage.Window;

/**
 * Dialog for capturing an image from a connected camera and optionally cropping
 * it.
 *
 * <p>
 * The content is a {@link CameraCapturePane}; the toolbar at the top lets the
 * user select the capture source and trigger a capture. The OK button is
 * disabled until an image is loaded; it returns the cropped image (if a
 * selection was drawn) or the full captured image.
 *
 * <p>
 * The dialog can also be pre-loaded with an existing image via the
 * {@link #capture(Window, WritableImage)} factory.
 *
 * <p>
 * This class lives in BSToolbox-jfx, which declares the Sarxos webcam-capture
 * dependency as {@code provided}. The consuming application must provide it at
 * runtime.
 */
public class CameraCaptureDialog extends Dialog<WritableImage> {

	private final CameraCapturePane capturePane = new CameraCapturePane();

	// =========================================================================
	// Constructor
	// =========================================================================

	public CameraCaptureDialog() {
		setTitle(BSAppMessages.getString("CameraCaptureDialog.title"));
		getDialogPane().getButtonTypes().setAll(BSButtonTypes.OK, BSButtonTypes.CANCEL);
		getDialogPane().setContent(capturePane);

		Button okButton = (Button) getDialogPane().lookupButton(BSButtonTypes.OK);
		okButton.setDisable(true);
		capturePane.imageProperty().addListener((obs, o, n) -> okButton.setDisable(n == null));

		setResultConverter(bt -> bt == BSButtonTypes.OK ? capturePane.getImageAsWritable() : null);

		setOnShown(e -> capturePane.initCameras());
	}

	// =========================================================================
	// Output size presets
	// =========================================================================

	/**
	 * Configures the output-size preset list shown in the toolbar. When the user
	 * picks a preset the cropped result will be downscaled to fit within those
	 * dimensions. Must be called before the dialog is shown.
	 */
	public void setOutputPresets(Dimension... presets) {
		capturePane.setOutputPresets(presets);
	}

	/**
	 * Configures the output-size preset list shown in the toolbar.
	 */
	public void setOutputPresets(List<Dimension> presets) {
		capturePane.setOutputPresets(presets);
	}

	// =========================================================================
	// Static factories
	// =========================================================================

	/**
	 * Opens the dialog and returns the captured/cropped image, or {@code null} if
	 * cancelled.
	 */
	public static WritableImage capture(Window owner) {
		return capture(owner, null);
	}

	/**
	 * Opens the dialog pre-loaded with {@code existing} (may be {@code null}).
	 * Returns the captured/cropped image, or {@code null} if cancelled.
	 */
	public static WritableImage capture(Window owner, WritableImage existing) {
		CameraCaptureDialog dlg = new CameraCaptureDialog();
		if (owner != null)
			dlg.initOwner(owner);
		if (existing != null)
			dlg.capturePane.setExistingImage(existing);
		dlg.showAndWait();
		return dlg.getResult();
	}

	/**
	 * Captures an image from the last-used camera without showing any UI:
	 * autocropping is applied and the result is downscaled if {@code maxWidth} or
	 * {@code maxHeight} is &gt; 0.
	 *
	 * <p>
	 * This method returns immediately; {@code onSuccess} (or {@code onError}) is
	 * called on the JavaFX Application Thread when done.
	 *
	 * @param maxWidth  maximum result width in pixels, 0 = no limit
	 * @param maxHeight maximum result height in pixels, 0 = no limit
	 * @param onSuccess called with the resulting image on the FX thread
	 * @param onError   called with an error message on the FX thread
	 */
	public static void captureHandsfree(int maxWidth, int maxHeight, Consumer<WritableImage> onSuccess,
			Consumer<String> onError) {
		Thread t = new Thread(() -> CameraCapturePane.doHandsfree(maxWidth, maxHeight, onSuccess, onError),
				"camera-handsfree");
		t.setDaemon(true);
		t.start();
	}
}
