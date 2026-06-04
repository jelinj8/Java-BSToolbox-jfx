package cz.bliksoft.javautils.fx.controls.images;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.sarxos.webcam.Webcam;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.app.exceptions.ViewableException;
import cz.bliksoft.javautils.fx.customization.BSButtonTypes;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Dialog for capturing an image from a connected camera and optionally cropping
 * it.
 *
 * <p>
 * The center pane is an {@link ImageCropPane}; the toolbar at the top lets the
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

	private static final Logger log = LogManager.getLogger(CameraCaptureDialog.class);
	private static final String PREF_CAMERA = "camera.lastSource";

	// ---- Controls ----
	private final ComboBox<Webcam> sourceCombo = new ComboBox<>();
	private final Button captureBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/camera")));
	private final ImageCropPane cropPane = new ImageCropPane();
	private final Label statusLabel = new Label();

	// ---- OK button reference (wired after pane construction) ----
	private Button okButton;

	// =========================================================================
	// Constructor
	// =========================================================================

	public CameraCaptureDialog() {
		setTitle(BSAppMessages.getString("CameraCaptureDialog.title"));
		getDialogPane().getButtonTypes().setAll(BSButtonTypes.OK, BSButtonTypes.CANCEL);
		getDialogPane().setContent(buildContent());

		okButton = (Button) getDialogPane().lookupButton(BSButtonTypes.OK);
		okButton.setDisable(true);

		cropPane.imageProperty().addListener((obs, o, n) -> okButton.setDisable(n == null));

		setResultConverter(bt -> {
			if (bt != BSButtonTypes.OK)
				return null;
			WritableImage cropped = cropPane.getCroppedImage();
			return cropped != null ? cropped : toWritable(cropPane.imageProperty().get());
		});

		wireCaptureButton();

		setOnShown(e -> loadCamerasAsync());
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
			dlg.cropPane.setImage((javafx.scene.image.Image) existing);
		dlg.showAndWait();
		return dlg.getResult();
	}

	// =========================================================================
	// UI construction
	// =========================================================================

	private VBox buildContent() {
		sourceCombo.setPromptText(BSAppMessages.getString("CameraCaptureDialog.sourceCombo.prompt"));
		sourceCombo.setCellFactory(lv -> new WebcamListCell());
		sourceCombo.setButtonCell(new WebcamListCell());
		sourceCombo.setMinWidth(200);

		sourceCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			if (n != null) {
				BSApp.setLocalProperty(PREF_CAMERA, n.getName());
				try {
					BSApp.saveLocalProperties();
				} catch (ViewableException ex) {
					log.warn("Failed to save camera preference", ex);
				}
			}
		});

		ToolBar toolbar = new ToolBar(new Label(BSAppMessages.getString("CameraCaptureDialog.toolbar.cameraLabel")), sourceCombo, captureBtn);

		cropPane.setMinHeight(400);
		VBox.setVgrow(cropPane, Priority.ALWAYS);

		statusLabel.getStyleClass().add("error-label");

		VBox content = new VBox(8, toolbar, statusLabel, cropPane);
		content.setPrefWidth(640);
		content.setPrefHeight(520);
		return content;
	}

	// =========================================================================
	// Camera loading
	// =========================================================================

	private void loadCamerasAsync() {
		Task<java.util.List<Webcam>> task = new Task<>() {
			@Override
			protected java.util.List<Webcam> call() {
				return Webcam.getWebcams();
			}
		};
		task.setOnSucceeded(e -> {
			java.util.List<Webcam> cams = task.getValue();
			sourceCombo.setItems(FXCollections.observableArrayList(cams));
			if (cams.isEmpty())
				return;
			// Restore last-used camera by name
			String lastName = (String) BSApp.getProperty(PREF_CAMERA);
			Webcam preferred = cams.stream().filter(c -> c.getName().equals(lastName)).findFirst().orElse(cams.get(0));
			sourceCombo.getSelectionModel().select(preferred);
		});
		task.setOnFailed(e -> log.warn("Failed to enumerate cameras", task.getException()));
		new Thread(task, "camera-enumerate").start();
	}

	// =========================================================================
	// Capture
	// =========================================================================

	private void wireCaptureButton() {
		captureBtn.setOnAction(e -> {
			Webcam cam = sourceCombo.getValue();
			if (cam == null)
				return;
			captureBtn.setDisable(true);
			statusLabel.setText(BSAppMessages.getString("CameraCaptureDialog.status.capturing"));
			new Thread(() -> {
				BufferedImage img = null;
				String error = null;
				ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
					Thread t = new Thread(r, "camera-capture");
					t.setDaemon(true);
					return t;
				});
				Future<BufferedImage> future = exec.submit(() -> {
					cam.open();
					return cam.getImage();
				});
				try {
					img = future.get(10, TimeUnit.SECONDS);
					if (img == null)
						error = BSAppMessages.getString("CameraCaptureDialog.error.emptyFrame");
				} catch (TimeoutException ex) {
					future.cancel(true);
					error = BSAppMessages.getString("CameraCaptureDialog.error.timeout");
					log.warn("Camera capture timed out for {}", cam.getName());
				} catch (Exception ex) {
					error = BSAppMessages.getString("CameraCaptureDialog.error.failed", ex.getMessage());
					log.warn("Camera capture failed", ex);
				} finally {
					exec.shutdownNow();
					// cam.close() blocks while Sarxos' internal capture thread winds down
					// (~20 s with OBS Virtual Camera). Run it on a daemon thread so the
					// UI error message appears immediately after the 10 s timeout.
					Thread closer = new Thread(() -> {
						try {
							cam.close();
						} catch (Exception ignore) {
						}
					}, "camera-close");
					closer.setDaemon(true);
					closer.start();
				}
				final BufferedImage finalImg = img;
				final String finalError = error;
				Platform.runLater(() -> {
					if (finalImg != null && finalError == null) {
						cropPane.setImage(finalImg);
						statusLabel.setText("");
					} else {
						statusLabel.setText(finalError != null ? finalError : BSAppMessages.getString("CameraCaptureDialog.error.unknown"));
					}
					captureBtn.setDisable(false);
				});
			}, "camera-capture-ctrl").start();
		});
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private static WritableImage toWritable(javafx.scene.image.Image img) {
		if (img == null)
			return null;
		if (img instanceof WritableImage wi)
			return wi;
		WritableImage out = new WritableImage((int) img.getWidth(), (int) img.getHeight());
		out.getPixelWriter().setPixels(0, 0, (int) img.getWidth(), (int) img.getHeight(), img.getPixelReader(), 0, 0);
		return out;
	}

	private static class WebcamListCell extends javafx.scene.control.ListCell<Webcam> {
		@Override
		protected void updateItem(Webcam item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty || item == null ? null : item.getName());
		}
	}
}
