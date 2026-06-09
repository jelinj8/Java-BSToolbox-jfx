package cz.bliksoft.javautils.fx.controls.images;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.sarxos.webcam.Webcam;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.app.exceptions.ViewableException;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import cz.bliksoft.javautils.images.PixelOps;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Camera selection toolbar + {@link ImageCropPane} panel.
 *
 * <p>
 * Used as the content of {@link CameraCaptureDialog} and by
 * {@code CameraAdministrationProvider}. Call {@link #initCameras()} after the
 * pane is added to a visible scene to start background camera enumeration.
 *
 * <p>
 * This class lives in BSToolbox-jfx, which declares the Sarxos webcam-capture
 * dependency as {@code provided}. The consuming application must provide it at
 * runtime.
 */
public class CameraCapturePane extends VBox {

	private static final Logger log = LogManager.getLogger(CameraCapturePane.class);

	static final String PREF_CAMERA = "camera.lastSource";
	static final String PREF_RESOLUTION_PREFIX = "camera.resolution."; // + cameraName, stored as "WxH"
	static final String PREF_ROTATION_PREFIX = "camera.rotation."; // + cameraName, stored as 0/90/180/270

	// ---- Controls ----
	private final ComboBox<Webcam> sourceCombo = new ComboBox<>();
	private final Button captureBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/camera")));
	private final ComboBox<Dimension> camResCombo = new ComboBox<>();
	private final ComboBox<Integer> camRotationCombo = new ComboBox<>();
	private final ComboBox<Dimension> outResCombo = new ComboBox<>();
	private final Button autocropBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/crop")));
	private final Button rotateLeftBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/rotate-left")));
	private final Button rotateRightBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/rotate-right")));
	private final ImageCropPane cropPane = new ImageCropPane();
	private final Label statusLabel = new Label();

	// ---- Output size presets ----
	private final List<Dimension> outputPresets = new ArrayList<>();
	private Label outResLabel;

	// ---- Live preview ----
	private final BooleanProperty previewEnabled = new SimpleBooleanProperty(false);
	private final SplitMenuButton captureSplitBtn = new SplitMenuButton();
	private final MenuItem previewToggleItem = new MenuItem();
	private volatile boolean previewRunning = false;

	// =========================================================================
	// Constructor
	// =========================================================================

	public CameraCapturePane() {
		super(8);
		buildContent();
		wireCaptureButton();
	}

	// =========================================================================
	// Public API
	// =========================================================================

	/** Returns the image property of the embedded crop pane. */
	public ObjectProperty<Image> imageProperty() {
		return cropPane.imageProperty();
	}

	/**
	 * Returns the cropped region as a {@link WritableImage}, or the full image if
	 * no crop selection is active, or {@code null} if no image is loaded. If a live
	 * preview is running it is stopped first so the last frame is frozen.
	 */
	public WritableImage getImageAsWritable() {
		if (previewRunning)
			stopPreview();
		WritableImage cropped = cropPane.getCroppedImage();
		if (cropped != null)
			return cropped;
		Image img = cropPane.imageProperty().get();
		if (img == null)
			return null;
		if (img instanceof WritableImage wi)
			return wi;
		WritableImage out = new WritableImage((int) img.getWidth(), (int) img.getHeight());
		out.getPixelWriter().setPixels(0, 0, (int) img.getWidth(), (int) img.getHeight(), img.getPixelReader(), 0, 0);
		return out;
	}

	/**
	 * Pre-loads the crop pane with an existing image (e.g. from a previous
	 * capture).
	 */
	public void setExistingImage(Image img) {
		cropPane.setImage(img);
	}

	/** Starts background enumeration of available cameras. */
	public void initCameras() {
		loadCamerasAsync();
	}

	/**
	 * Configures the output-size preset list shown in the toolbar. When the user
	 * picks a preset the cropped result will be downscaled to fit within those
	 * dimensions. Must be called before the pane is shown.
	 */
	public void setOutputPresets(Dimension... presets) {
		setOutputPresets(Arrays.asList(presets));
	}

	/** Configures the output-size preset list shown in the toolbar. */
	public void setOutputPresets(List<Dimension> presets) {
		outputPresets.clear();
		outputPresets.addAll(presets);
		updateOutResCombo();
	}

	/**
	 * When true, the capture button becomes a split button with a live-preview
	 * toggle in its dropdown. Default: false.
	 */
	public BooleanProperty previewEnabledProperty() {
		return previewEnabled;
	}

	public boolean isPreviewEnabled() {
		return previewEnabled.get();
	}

	public void setPreviewEnabled(boolean v) {
		previewEnabled.set(v);
	}

	// =========================================================================
	// UI construction
	// =========================================================================

	private void buildContent() {
		sourceCombo.setPromptText(BSAppMessages.getString("CameraCaptureDialog.sourceCombo.prompt"));
		sourceCombo.setCellFactory(lv -> new WebcamListCell());
		sourceCombo.setButtonCell(new WebcamListCell());
		sourceCombo.setMinWidth(200);

		sourceCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			if (previewRunning)
				stopPreview();
			if (n != null) {
				BSApp.setLocalProperty(PREF_CAMERA, n.getName());
				try {
					BSApp.saveLocalProperties();
				} catch (ViewableException ex) {
					log.warn("Failed to save camera preference", ex);
				}
				loadCameraResolutionsAsync(n);
				restoreCameraRotation(n.getName());
			}
		});

		// Camera resolution combo (left group, hidden until camera reports >1 size)
		camResCombo.setCellFactory(lv -> new DimensionListCell(null));
		camResCombo.setButtonCell(new DimensionListCell(null));
		camResCombo.setVisible(false);
		camResCombo.setManaged(false);
		camResCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			Webcam cam = sourceCombo.getValue();
			if (n != null && cam != null) {
				BSApp.setLocalProperty(PREF_RESOLUTION_PREFIX + cam.getName(), n.width + "x" + n.height);
				try {
					BSApp.saveLocalProperties();
				} catch (ViewableException ex) {
					log.warn("Failed to save camera resolution preference", ex);
				}
			}
		});

		// Camera pre-rotation combo (left group, always visible)
		camRotationCombo.setItems(FXCollections.observableArrayList(0, 90, 180, 270));
		camRotationCombo.setCellFactory(lv -> new RotationListCell());
		camRotationCombo.setButtonCell(new RotationListCell());
		camRotationCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			Webcam cam = sourceCombo.getValue();
			if (n != null && cam != null) {
				BSApp.setLocalProperty(PREF_ROTATION_PREFIX + cam.getName(), String.valueOf(n));
				try {
					BSApp.saveLocalProperties();
				} catch (ViewableException ex) {
					log.warn("Failed to save camera rotation preference", ex);
				}
			}
		});
		camRotationCombo.getSelectionModel().selectFirst();

		// Output resolution combo (right group, hidden until presets configured)
		String origLabel = BSAppMessages.getString("CameraCaptureDialog.toolbar.outputResOriginal");
		outResCombo.setCellFactory(lv -> new DimensionListCell(origLabel));
		outResCombo.setButtonCell(new DimensionListCell(origLabel));
		outResCombo.setVisible(false);
		outResCombo.setManaged(false);
		outResCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			if (n == null) {
				cropPane.setMaxResultWidth(0);
				cropPane.setMaxResultHeight(0);
			} else {
				cropPane.setMaxResultWidth(n.width);
				cropPane.setMaxResultHeight(n.height);
			}
		});

		// Right-group buttons
		autocropBtn.setTooltip(new Tooltip(BSAppMessages.getString("CameraCaptureDialog.toolbar.autocropButton")));
		autocropBtn.setOnAction(e -> cropPane.autocrop());
		autocropBtn.setDisable(true);

		rotateLeftBtn.setTooltip(new Tooltip(BSAppMessages.getString("CameraCaptureDialog.toolbar.rotateLeftButton")));
		rotateLeftBtn.setOnAction(e -> cropPane.rotateLeft());
		rotateLeftBtn.setDisable(true);

		rotateRightBtn
				.setTooltip(new Tooltip(BSAppMessages.getString("CameraCaptureDialog.toolbar.rotateRightButton")));
		rotateRightBtn.setOnAction(e -> cropPane.rotateRight());
		rotateRightBtn.setDisable(true);

		cropPane.imageProperty().addListener((obs, o, n) -> {
			boolean hasImage = n != null;
			autocropBtn.setDisable(!hasImage);
			rotateLeftBtn.setDisable(!hasImage);
			rotateRightBtn.setDisable(!hasImage);
		});

		// Split capture button for live-preview mode
		captureSplitBtn.setGraphic(ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/camera")));
		previewToggleItem.setText(BSAppMessages.getString("CameraCaptureDialog.toolbar.previewStart"));
		captureSplitBtn.getItems().add(previewToggleItem);
		captureBtn.visibleProperty().bind(previewEnabled.not());
		captureBtn.managedProperty().bind(previewEnabled.not());
		captureSplitBtn.visibleProperty().bind(previewEnabled);
		captureSplitBtn.managedProperty().bind(previewEnabled);

		// Spacer between left and right toolbar groups
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		outResLabel = new Label(BSAppMessages.getString("CameraCaptureDialog.toolbar.outputResLabel"));
		outResLabel.setVisible(false);
		outResLabel.setManaged(false);

		ToolBar toolbar = new ToolBar(new Label(BSAppMessages.getString("CameraCaptureDialog.toolbar.cameraLabel")),
				sourceCombo, captureBtn, captureSplitBtn, camResCombo,
				new Label(BSAppMessages.getString("CameraCaptureDialog.toolbar.cameraRotationLabel")), camRotationCombo,
				spacer, outResLabel, outResCombo, autocropBtn, rotateLeftBtn, rotateRightBtn);

		cropPane.setMinHeight(400);
		VBox.setVgrow(cropPane, Priority.ALWAYS);

		statusLabel.getStyleClass().add("error-label");

		getChildren().addAll(toolbar, statusLabel, cropPane);
		setPrefWidth(860);
		setPrefHeight(520);

		updateOutResCombo();

		sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene == null)
				stopPreview();
		});
	}

	private void updateOutResCombo() {
		if (outResCombo == null)
			return;
		boolean show = !outputPresets.isEmpty();
		if (show) {
			List<Dimension> items = new ArrayList<>();
			items.add(null); // "Original"
			items.addAll(outputPresets);
			outResCombo.setItems(FXCollections.observableArrayList(items));
			outResCombo.getSelectionModel().selectFirst();
		}
		outResCombo.setVisible(show);
		outResCombo.setManaged(show);
		if (outResLabel != null) {
			outResLabel.setVisible(show);
			outResLabel.setManaged(show);
		}
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

	private void loadCameraResolutionsAsync(Webcam cam) {
		new Thread(() -> {
			Dimension[] sizes;
			try {
				sizes = cam.getViewSizes();
			} catch (Exception ex) {
				log.debug("Could not retrieve view sizes for camera {}", cam.getName(), ex);
				sizes = new Dimension[0];
			}
			final Dimension[] finalSizes = sizes;
			Platform.runLater(() -> applyCameraResolutions(finalSizes, cam.getName()));
		}, "camera-res-load").start();
	}

	private void applyCameraResolutions(Dimension[] sizes, String cameraName) {
		if (sizes.length <= 1) {
			camResCombo.setVisible(false);
			camResCombo.setManaged(false);
			return;
		}
		camResCombo.setItems(FXCollections.observableArrayList(sizes));

		// Restore per-camera saved resolution
		String saved = (String) BSApp.getProperty(PREF_RESOLUTION_PREFIX + cameraName);
		Dimension toSelect = null;
		if (saved != null) {
			String[] parts = saved.split("x", 2);
			if (parts.length == 2) {
				try {
					int sw = Integer.parseInt(parts[0]);
					int sh = Integer.parseInt(parts[1]);
					for (Dimension d : sizes) {
						if (d.width == sw && d.height == sh) {
							toSelect = d;
							break;
						}
					}
				} catch (NumberFormatException ignore) {
				}
			}
		}
		camResCombo.getSelectionModel().select(toSelect != null ? toSelect : sizes[sizes.length - 1]);
		camResCombo.setVisible(true);
		camResCombo.setManaged(true);
	}

	private void restoreCameraRotation(String cameraName) {
		String saved = (String) BSApp.getProperty(PREF_ROTATION_PREFIX + cameraName);
		Integer rot = null;
		if (saved != null) {
			try {
				rot = Integer.valueOf(saved);
			} catch (NumberFormatException ignore) {
			}
		}
		camRotationCombo.getSelectionModel().select(rot != null ? rot : Integer.valueOf(0));
	}

	// =========================================================================
	// Capture
	// =========================================================================

	private void wireCaptureButton() {
		captureBtn.setOnAction(e -> doCapture());
		captureSplitBtn.setOnAction(e -> {
			if (previewRunning)
				stopPreview(); // freeze: last preview frame is already in the crop pane
			else
				doCapture();
		});
		previewToggleItem.setOnAction(e -> {
			if (previewRunning)
				stopPreview();
			else
				startPreview();
		});
	}

	private void doCapture() {
		Webcam cam = sourceCombo.getValue();
		if (cam == null)
			return;
		captureBtn.setDisable(true);
		captureSplitBtn.setDisable(true);
		statusLabel.setText(BSAppMessages.getString("CameraCaptureDialog.status.capturing"));
		Dimension selectedRes = camResCombo.isVisible() ? camResCombo.getValue() : null;
		int preRotation = camRotationCombo.getValue() != null ? camRotationCombo.getValue() : 0;
		new Thread(() -> {
			BufferedImage img = null;
			String error = null;
			ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "camera-capture");
				t.setDaemon(true);
				return t;
			});
			Future<BufferedImage> future = exec.submit(() -> {
				if (selectedRes != null)
					cam.setViewSize(selectedRes);
				cam.open();
				return cam.getImage();
			});
			try {
				img = future.get(10, TimeUnit.SECONDS);
				if (img == null)
					error = BSAppMessages.getString("CameraCaptureDialog.error.emptyFrame");
				else if (preRotation != 0)
					img = applyRotation(img, preRotation);
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
					statusLabel.setText(finalError != null ? finalError
							: BSAppMessages.getString("CameraCaptureDialog.error.unknown"));
				}
				captureBtn.setDisable(false);
				captureSplitBtn.setDisable(false);
			});
		}, "camera-capture-ctrl").start();
	}

	private void startPreview() {
		Webcam cam = sourceCombo.getValue();
		if (cam == null)
			return;
		previewRunning = true;
		sourceCombo.setDisable(true);
		camResCombo.setDisable(true);
		camRotationCombo.setDisable(true);
		statusLabel.setText(BSAppMessages.getString("CameraCaptureDialog.status.previewing"));
		previewToggleItem.setText(BSAppMessages.getString("CameraCaptureDialog.toolbar.previewStop"));
		Dimension selectedRes = camResCombo.isVisible() ? camResCombo.getValue() : null;
		int preRotation = camRotationCombo.getValue() != null ? camRotationCombo.getValue() : 0;
		Thread t = new Thread(() -> {
			try {
				if (selectedRes != null)
					cam.setViewSize(selectedRes);
				cam.open();
				while (previewRunning) {
					BufferedImage frame = cam.getImage();
					if (frame != null) {
						final BufferedImage rotated = preRotation != 0 ? applyRotation(frame, preRotation) : frame;
						Platform.runLater(() -> cropPane.setImage(rotated));
					}
					try {
						Thread.sleep(33); // ~30 fps cap
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			} catch (Exception ex) {
				log.warn("Camera preview failed for {}", cam.getName(), ex);
				final String msg = BSAppMessages.getString("CameraCaptureDialog.error.failed", ex.getMessage());
				Platform.runLater(() -> statusLabel.setText(msg));
			} finally {
				previewRunning = false;
				Thread closer = new Thread(() -> {
					try {
						cam.close();
					} catch (Exception ignore) {
					}
				}, "camera-close");
				closer.setDaemon(true);
				closer.start();
				Platform.runLater(this::resetPreviewControls);
			}
		}, "camera-preview");
		t.setDaemon(true);
		t.start();
	}

	private void stopPreview() {
		previewRunning = false;
		statusLabel.setText("");
		resetPreviewControls();
	}

	private void resetPreviewControls() {
		sourceCombo.setDisable(false);
		camResCombo.setDisable(false);
		camRotationCombo.setDisable(false);
		previewToggleItem.setText(BSAppMessages.getString("CameraCaptureDialog.toolbar.previewStart"));
	}

	// =========================================================================
	// Handsfree (package-private, called from CameraCaptureDialog)
	// =========================================================================

	static void doHandsfree(int maxWidth, int maxHeight, Consumer<WritableImage> onSuccess, Consumer<String> onError) {
		String cameraName = (String) BSApp.getProperty(PREF_CAMERA);
		if (cameraName == null) {
			Platform.runLater(() -> onError.accept(BSAppMessages.getString("CameraCaptureDialog.error.noSavedCamera")));
			return;
		}

		List<Webcam> cams;
		try {
			cams = Webcam.getWebcams();
		} catch (Exception ex) {
			log.warn("Handsfree: failed to enumerate cameras", ex);
			Platform.runLater(
					() -> onError.accept(BSAppMessages.getString("CameraCaptureDialog.error.failed", ex.getMessage())));
			return;
		}

		Webcam cam = cams.stream().filter(c -> c.getName().equals(cameraName)).findFirst().orElse(null);
		if (cam == null) {
			Platform.runLater(() -> onError
					.accept(BSAppMessages.getString("CameraCaptureDialog.error.cameraNotFound", cameraName)));
			return;
		}

		// Apply saved capture resolution if available
		String savedRes = (String) BSApp.getProperty(PREF_RESOLUTION_PREFIX + cameraName);
		if (savedRes != null) {
			String[] parts = savedRes.split("x", 2);
			if (parts.length == 2) {
				try {
					cam.setViewSize(new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
				} catch (Exception ignore) {
				}
			}
		}

		BufferedImage captured = null;
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
			captured = future.get(10, TimeUnit.SECONDS);
			if (captured == null)
				error = BSAppMessages.getString("CameraCaptureDialog.error.emptyFrame");
		} catch (TimeoutException ex) {
			future.cancel(true);
			error = BSAppMessages.getString("CameraCaptureDialog.error.timeout");
			log.warn("Handsfree capture timed out for {}", cameraName);
		} catch (Exception ex) {
			error = BSAppMessages.getString("CameraCaptureDialog.error.failed", ex.getMessage());
			log.warn("Handsfree capture failed", ex);
		} finally {
			exec.shutdownNow();
			Thread closer = new Thread(() -> {
				try {
					cam.close();
				} catch (Exception ignore) {
				}
			}, "camera-close");
			closer.setDaemon(true);
			closer.start();
		}

		if (captured == null || error != null) {
			final String msg = error != null ? error : BSAppMessages.getString("CameraCaptureDialog.error.unknown");
			Platform.runLater(() -> onError.accept(msg));
			return;
		}

		// Pre-rotation
		String savedRotStr = (String) BSApp.getProperty(PREF_ROTATION_PREFIX + cameraName);
		if (savedRotStr != null) {
			try {
				int rot = Integer.parseInt(savedRotStr);
				if (rot != 0)
					captured = applyRotation(captured, rot);
			} catch (NumberFormatException ignore) {
			}
		}

		// Autocrop
		java.awt.Rectangle cropRect = ImageCropPane.autocropRect(captured);
		BufferedImage result;
		if (cropRect != null && cropRect.width > 0 && cropRect.height > 0) {
			result = captured.getSubimage(cropRect.x, cropRect.y, cropRect.width, cropRect.height);
		} else {
			result = captured;
		}

		// Downscale if limits specified
		if (maxWidth > 0 || maxHeight > 0) {
			result = scaleAWT(result, maxWidth, maxHeight);
		}

		final BufferedImage finalResult = result;
		Platform.runLater(() -> onSuccess.accept(SwingFXUtils.toFXImage(finalResult, null)));
	}

	static BufferedImage applyRotation(BufferedImage img, int degrees) {
		int d = ((degrees % 360) + 360) % 360;
		return d == 0 ? img : cz.bliksoft.javautils.images.ImageUtils.rotate(img, d);
	}

	static BufferedImage scaleAWT(BufferedImage src, int maxWidth, int maxHeight) {
		int w = src.getWidth(), h = src.getHeight();
		double scaleW = (maxWidth > 0) ? (double) maxWidth / w : Double.MAX_VALUE;
		double scaleH = (maxHeight > 0) ? (double) maxHeight / h : Double.MAX_VALUE;
		double scale = Math.min(scaleW, scaleH);
		if (scale >= 1.0)
			return src;

		int targetW = Math.max(1, (int) Math.round(w * scale));
		int targetH = Math.max(1, (int) Math.round(h * scale));

		int[] pixels = src.getRGB(0, 0, w, h, null, 0, w);
		int[] scaled = PixelOps.scale(pixels, w, h, targetW, targetH);
		BufferedImage dst = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
		dst.setRGB(0, 0, targetW, targetH, scaled, 0, targetW);
		return dst;
	}

	// =========================================================================
	// Inner cell classes
	// =========================================================================

	private static class RotationListCell extends ListCell<Integer> {
		@Override
		protected void updateItem(Integer item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty || item == null ? null : item + "°");
		}
	}

	private static class WebcamListCell extends ListCell<Webcam> {
		@Override
		protected void updateItem(Webcam item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty || item == null ? null : item.getName());
		}
	}

	private static class DimensionListCell extends ListCell<Dimension> {
		private final String nullLabel;

		DimensionListCell(String nullLabel) {
			this.nullLabel = nullLabel;
		}

		@Override
		protected void updateItem(Dimension item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
			} else if (item == null) {
				setText(nullLabel);
			} else {
				setText(item.width + " × " + item.height);
			}
		}
	}
}
