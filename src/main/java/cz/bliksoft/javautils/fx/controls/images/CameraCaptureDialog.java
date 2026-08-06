package cz.bliksoft.javautils.fx.controls.images;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.utils.StageAutoSizer;
import cz.bliksoft.javautils.app.ui.utils.state.binders.StageStateBinder;
import cz.bliksoft.javautils.fx.controls.images.cam.NetworkCameraSource;
import cz.bliksoft.javautils.fx.customization.BSButtonTypes;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
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
 *
 * <p>
 * BSToolbox-jfx also declares {@code org.bytedeco:opencv-platform} as
 * {@code provided}; if the consuming application provides it, captures use
 * higher resolutions than Sarxos's 640x480 cap. See {@link CameraCapturePane}
 * for details.
 *
 * <p>
 * The camera source list also includes any {@link NetworkCameraSource}s
 * registered for the application (HTTP snapshot endpoints, e.g. a phone running
 * an IP camera app), with no extra dependency required.
 */
public class CameraCaptureDialog extends Dialog<WritableImage> {

	/** Accepts the current capture, provided there's actually one to accept. */
	private static final KeyCombination SHORTCUT_ACCEPT = KeyCombination.keyCombination("Ctrl+Enter");

	/**
	 * {@link StageStateBinder} key — stable across instances so position/size
	 * persist.
	 */
	private static final String STATE_KEY = "CameraCaptureDialog";

	private final CameraCapturePane capturePane = new CameraCapturePane();

	// =========================================================================
	// Constructor
	// =========================================================================

	public CameraCaptureDialog() {
		setTitle(BSAppJFXMessages.getString("CameraCaptureDialog.title"));
		getDialogPane().getButtonTypes().setAll(BSButtonTypes.OK, BSButtonTypes.CANCEL);
		getDialogPane().setContent(capturePane);
		getDialogPane().setPrefSize(960, 520);
		setResizable(true);

		Button okButton = (Button) getDialogPane().lookupButton(BSButtonTypes.OK);
		okButton.setDisable(true);
		okButton.setTooltip(new Tooltip(SHORTCUT_ACCEPT.getDisplayText()));
		capturePane.imageProperty().addListener((obs, o, n) -> okButton.setDisable(n == null));

		getDialogPane().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (SHORTCUT_ACCEPT.match(e)) {
				if (!okButton.isDisabled())
					okButton.fire();
				e.consume();
			}
		});

		setResultConverter(bt -> bt == BSButtonTypes.OK ? capturePane.getImageAsWritable() : null);

		// StageAutoSizer floors the window at the toolbar's actual content width, so
		// resizing (or restoring a saved size from a wider screen/toolbar state)
		// can't shrink it enough to force the toolbar into overflow. Wired in
		// onShowing (not the constructor): the dialog's underlying Stage/Scene don't
		// exist yet at construction time — Dialog builds them lazily, immediately
		// before firing this event.
		setOnShowing(e -> {
			if (getDialogPane().getScene().getWindow() instanceof Stage stage) {
				StageAutoSizer.install(stage);
				StageStateBinder.restore(stage, STATE_KEY);
			}
		});
		setOnHiding(e -> {
			if (getDialogPane().getScene().getWindow() instanceof Stage stage) {
				StageStateBinder.save(stage, STATE_KEY);
			}
		});

		setOnShown(e -> capturePane.initCameras());
	}

	// =========================================================================
	// Output size presets
	// =========================================================================

	/**
	 * Enables or disables the live-preview split-button mode. Must be called before
	 * the dialog is shown.
	 */
	public void setPreviewEnabled(boolean v) {
		capturePane.setPreviewEnabled(v);
	}

	/**
	 * Returns the original source bytes (e.g. JPEG) for the result image, iff it is
	 * exactly the last captured frame with no crop/resize/rotation applied;
	 * otherwise {@code null} (caller should re-encode the result image).
	 */
	public byte[] getResultRawBytes() {
		return capturePane.getResultRawBytes();
	}

	/**
	 * Returns the captured frame before any crop was applied, letting a caller keep
	 * the full-resolution source around for a later re-crop. See
	 * {@link CameraCapturePane#getUncroppedImage()}.
	 */
	public BufferedImage getUncroppedImage() {
		return capturePane.getUncroppedImage();
	}

	/**
	 * Returns the crop rectangle (in {@link #getUncroppedImage()}'s pixel
	 * coordinates) applied to produce the result, or {@code null} if none.
	 */
	public java.awt.Rectangle getCropRect() {
		return capturePane.getCropRect();
	}

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
	 * Captures an image without showing any UI, using the camera, autocrop, and
	 * max-dimension settings configured in the Cameras administration panel
	 * ({@code CameraAdministrationProvider}). If no handsfree camera is configured,
	 * falls back to the last camera used in the interactive dialog. Resolution and
	 * pre-rotation are loaded the same way as in the dialog (per-camera saved
	 * preferences).
	 *
	 * <p>
	 * This method returns immediately; {@code onSuccess}, {@code onUnavailable} or
	 * {@code onError} is called on the JavaFX Application Thread when done.
	 *
	 * @param onSuccess     called with the resulting image on the FX thread
	 * @param onUnavailable called on the FX thread if no camera is configured for
	 *                      handsfree capture, or the configured camera can't be
	 *                      found (caller should fall back to the interactive
	 *                      dialog)
	 * @param onError       called with an error message on the FX thread if a
	 *                      configured camera was found but the capture itself
	 *                      failed (timeout, empty frame, exception)
	 */
	public static void captureHandsfree(Consumer<WritableImage> onSuccess, Runnable onUnavailable,
			Consumer<String> onError) {
		Thread t = new Thread(() -> CameraCapturePane.doHandsfree(onSuccess, onUnavailable, onError),
				"camera-handsfree");
		t.setDaemon(true);
		t.start();
	}
}
