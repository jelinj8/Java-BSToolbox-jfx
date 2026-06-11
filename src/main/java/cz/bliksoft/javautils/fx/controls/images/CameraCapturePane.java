package cz.bliksoft.javautils.fx.controls.images;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import cz.bliksoft.javautils.fx.controls.images.cam.CameraNativeLock;
import cz.bliksoft.javautils.fx.controls.images.cam.ICameraPreviewSession;
import cz.bliksoft.javautils.fx.controls.images.cam.ICameraSource;
import cz.bliksoft.javautils.fx.controls.images.cam.NetworkCameraSource;
import cz.bliksoft.javautils.fx.controls.images.cam.WebcamCameraSource;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import cz.bliksoft.javautils.images.PixelOps;
import cz.bliksoft.javautils.xmlfilesystem.singletons.Services;
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
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
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
 *
 * <p>
 * BSToolbox-jfx also declares {@code org.bytedeco:opencv-platform} as
 * {@code provided}. When the consuming application provides it at runtime, this
 * pane uses OpenCV to discover and capture at resolutions beyond the 640x480
 * cap of Sarxos's default driver. If absent, capture silently falls back to
 * Sarxos's default behavior (max 640x480).
 *
 * <p>
 * In addition to physical webcams, the source combo also lists any
 * {@link NetworkCameraSource}s registered via the {@code /singletons}
 * XmlFilesystem registry — HTTP snapshot endpoints (e.g. a phone running an IP
 * camera app) captured via a plain HTTP GET, requiring no extra dependency. See
 * {@link NetworkCameraSource} for the registration format.
 */
public class CameraCapturePane extends VBox {

	private static final Logger log = LogManager.getLogger(CameraCapturePane.class);

	static final String PREF_CAMERA = "camera.lastSource";
	static final String PREF_RESOLUTION_PREFIX = "camera.resolution."; // + sourceId, stored as "WxH"
	static final String PREF_ROTATION_PREFIX = "camera.rotation."; // + sourceId, stored as 0/90/180/270

	static final String PREF_HANDSFREE_SOURCE = "camera.handsfree.source";
	static final String PREF_HANDSFREE_AUTOCROP = "camera.handsfree.autocrop"; // "true"/"false", default true
	static final String PREF_HANDSFREE_MAX_DIMENSION = "camera.handsfree.maxDimension"; // int, 0/absent = no limit

	// ---- Controls ----
	private final ComboBox<ICameraSource> sourceCombo = new ComboBox<>();
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

	// ---- Raw bytes of the last capture (if pass-through is possible) ----
	private volatile byte[] lastCaptureRawBytes;

	// ---- External hooks ----
	private Consumer<List<ICameraSource>> sourcesLoadedListener;

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
		lastCaptureRawBytes = null;
		cropPane.setImage(img);
	}

	/**
	 * Returns the original source bytes for the current image, if it is exactly the
	 * last captured frame with no crop/resize/rotation applied; otherwise
	 * {@code null} (caller should re-encode {@link #getImageAsWritable()}).
	 */
	public byte[] getResultRawBytes() {
		return cropPane.hasActiveSelection() ? null : lastCaptureRawBytes;
	}

	/** Starts background enumeration of available cameras. */
	public void initCameras() {
		loadCamerasAsync();
	}

	/**
	 * Registers a listener invoked on the FX thread once the camera source list
	 * (webcams + registered {@link ICameraSource} singletons) has been enumerated,
	 * letting callers populate additional, independent source selectors with the
	 * same list.
	 */
	public void setSourcesLoadedListener(Consumer<List<ICameraSource>> listener) {
		this.sourcesLoadedListener = listener;
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
		sourceCombo.setCellFactory(lv -> new CameraSourceListCell());
		sourceCombo.setButtonCell(new CameraSourceListCell());
		sourceCombo.setMinWidth(200);

		sourceCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			if (previewRunning)
				stopPreview();
			if (n != null) {
				BSApp.setLocalProperty(PREF_CAMERA, n.getId());
				try {
					BSApp.saveLocalProperties();
				} catch (ViewableException ex) {
					log.warn("Failed to save camera preference", ex);
				}
				loadCameraResolutionsAsync(n);
				restoreCameraRotation(n.getId());
			}
		});

		// Camera resolution combo (left group, hidden until camera reports >1 size)
		camResCombo.setCellFactory(lv -> new DimensionListCell(null));
		camResCombo.setButtonCell(new DimensionListCell(null));
		camResCombo.setVisible(false);
		camResCombo.setManaged(false);
		camResCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			ICameraSource src = sourceCombo.getValue();
			if (n != null && src != null) {
				BSApp.setLocalProperty(PREF_RESOLUTION_PREFIX + src.getId(), n.width + "x" + n.height);
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
			ICameraSource src = sourceCombo.getValue();
			if (n != null && src != null) {
				BSApp.setLocalProperty(PREF_ROTATION_PREFIX + src.getId(), String.valueOf(n));
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

		cropPane.setMinHeight(100);
		VBox.setVgrow(cropPane, Priority.ALWAYS);

		statusLabel.getStyleClass().add("error-label");

		getChildren().addAll(toolbar, statusLabel, cropPane);

		// Ensure the toolbar can always show the source/rotation combos plus its
		// overflow button, even if the rest of the window shrinks further.
		setMinWidth(360);

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
				CameraNativeLock.acquire();
				try {
					return Webcam.getWebcams();
				} finally {
					CameraNativeLock.release();
				}
			}
		};
		task.setOnSucceeded(e -> {
			java.util.List<Webcam> cams = task.getValue();

			List<ICameraSource> sources = new ArrayList<>();
			for (int i = 0; i < cams.size(); i++)
				sources.add(new WebcamCameraSource(cams.get(i), i));
			sources.addAll(Services.<ICameraSource>getServices(ICameraSource.class));

			sourceCombo.setItems(FXCollections.observableArrayList(sources));
			if (sourcesLoadedListener != null)
				sourcesLoadedListener.accept(sources);
			if (sources.isEmpty())
				return;
			// Restore last-used source by id
			String lastId = (String) BSApp.getProperty(PREF_CAMERA);
			ICameraSource preferred = sources.stream().filter(c -> c.getId().equals(lastId)).findFirst()
					.orElse(sources.get(0));
			sourceCombo.getSelectionModel().select(preferred);
		});
		task.setOnFailed(e -> log.warn("Failed to enumerate cameras", task.getException()));
		new Thread(task, "camera-enumerate").start();
	}

	private void loadCameraResolutionsAsync(ICameraSource src) {
		new Thread(() -> {
			Dimension[] sizes;
			try {
				sizes = src.getAvailableResolutions();
			} catch (Exception ex) {
				log.debug("Could not retrieve resolutions for camera {}", src.getDisplayName(), ex);
				sizes = new Dimension[0];
			}
			final Dimension[] finalSizes = sizes;
			Platform.runLater(() -> applyCameraResolutions(finalSizes, src.getId()));
		}, "camera-res-load").start();
	}

	private void applyCameraResolutions(Dimension[] sizes, String sourceId) {
		if (sizes.length <= 1) {
			camResCombo.setVisible(false);
			camResCombo.setManaged(false);
			return;
		}
		camResCombo.setItems(FXCollections.observableArrayList(sizes));

		// Restore per-source saved resolution
		Dimension saved = parseDimension((String) BSApp.getProperty(PREF_RESOLUTION_PREFIX + sourceId));
		Dimension toSelect = null;
		if (saved != null) {
			for (Dimension d : sizes) {
				if (d.width == saved.width && d.height == saved.height) {
					toSelect = d;
					break;
				}
			}
		}
		camResCombo.getSelectionModel().select(toSelect != null ? toSelect : sizes[sizes.length - 1]);
		camResCombo.setVisible(true);
		camResCombo.setManaged(true);
	}

	private void restoreCameraRotation(String sourceId) {
		String saved = (String) BSApp.getProperty(PREF_ROTATION_PREFIX + sourceId);
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
		ICameraSource src = sourceCombo.getValue();
		if (src == null)
			return;
		captureBtn.setDisable(true);
		captureSplitBtn.setDisable(true);
		statusLabel.setText(BSAppMessages.getString("CameraCaptureDialog.status.capturing"));
		Dimension selectedRes = camResCombo.isVisible() ? camResCombo.getValue() : null;
		int preRotation = camRotationCombo.getValue() != null ? camRotationCombo.getValue() : 0;
		new Thread(() -> {
			BufferedImage img = null;
			byte[] rawBytes = null;
			boolean autocropRequested = false;
			String error = null;
			ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "camera-capture");
				t.setDaemon(true);
				return t;
			});
			// cam.close() (run by ICameraSource implementations on capture) may block
			// for a while (~20 s with OBS Virtual Camera), so capture runs with a 10 s
			// timeout and the close happens on its own daemon thread.
			Future<BufferedImage> future = exec.submit(() -> src.grabFrame(selectedRes));
			try {
				img = future.get(10, TimeUnit.SECONDS);
				if (img == null)
					error = BSAppMessages.getString("CameraCaptureDialog.error.emptyFrame");
				else {
					if (preRotation != 0)
						img = applyRotation(img, preRotation);
					else
						rawBytes = grabFrameBytesQuietly(src, selectedRes);
					autocropRequested = Boolean.TRUE
							.equals(grabFrameMetadataQuietly(src, selectedRes).get(ICameraSource.METADATA_AUTOCROP));
				}
			} catch (TimeoutException ex) {
				future.cancel(true);
				error = BSAppMessages.getString("CameraCaptureDialog.error.timeout");
				log.warn("Camera capture timed out for {}", src.getDisplayName());
			} catch (Exception ex) {
				error = BSAppMessages.getString("CameraCaptureDialog.error.failed", ex.getMessage());
				log.warn("Camera capture failed", ex);
			} finally {
				exec.shutdownNow();
			}
			final BufferedImage finalImg = img;
			final byte[] finalRawBytes = rawBytes;
			final boolean finalAutocropRequested = autocropRequested;
			final String finalError = error;
			Platform.runLater(() -> {
				if (finalImg != null && finalError == null) {
					lastCaptureRawBytes = finalRawBytes;
					cropPane.setImage(finalImg);
					if (finalAutocropRequested)
						cropPane.autocrop();
					statusLabel.setText("");
				} else {
					lastCaptureRawBytes = null;
					statusLabel.setText(finalError != null ? finalError
							: BSAppMessages.getString("CameraCaptureDialog.error.unknown"));
				}
				captureBtn.setDisable(false);
				captureSplitBtn.setDisable(false);
			});
		}, "camera-capture-ctrl").start();
	}

	/**
	 * Calls {@link ICameraSource#grabFrameBytes(Dimension)}, swallowing any
	 * exception as {@code null}.
	 */
	private static byte[] grabFrameBytesQuietly(ICameraSource src, Dimension resolution) {
		try {
			return src.grabFrameBytes(resolution);
		} catch (Exception ex) {
			log.debug("grabFrameBytes failed for {}", src.getDisplayName(), ex);
			return null;
		}
	}

	/**
	 * Calls {@link ICameraSource#grabFrameMetadata(Dimension)}, swallowing any
	 * exception as an empty map.
	 */
	private static Map<String, Object> grabFrameMetadataQuietly(ICameraSource src, Dimension resolution) {
		try {
			Map<String, Object> meta = src.grabFrameMetadata(resolution);
			return meta != null ? meta : Map.of();
		} catch (Exception ex) {
			log.debug("grabFrameMetadata failed for {}", src.getDisplayName(), ex);
			return Map.of();
		}
	}

	private void startPreview() {
		ICameraSource src = sourceCombo.getValue();
		if (src == null)
			return;
		lastCaptureRawBytes = null;
		previewRunning = true;
		sourceCombo.setDisable(true);
		camResCombo.setDisable(true);
		camRotationCombo.setDisable(true);
		statusLabel.setText(BSAppMessages.getString("CameraCaptureDialog.status.previewing"));
		previewToggleItem.setText(BSAppMessages.getString("CameraCaptureDialog.toolbar.previewStop"));
		Dimension selectedRes = camResCombo.isVisible() ? camResCombo.getValue() : null;
		int preRotation = camRotationCombo.getValue() != null ? camRotationCombo.getValue() : 0;

		Thread t = new Thread(() -> {
			ICameraPreviewSession session = null;
			try {
				session = src.openPreview(selectedRes);
				if (session == null)
					throw new IllegalStateException("Could not open camera " + src.getDisplayName());
				while (previewRunning) {
					BufferedImage frame = session.readFrame();
					if (frame != null) {
						final BufferedImage rotated = preRotation != 0 ? applyRotation(frame, preRotation) : frame;
						Platform.runLater(() -> cropPane.setImage(rotated));
					}
				}
			} catch (Exception ex) {
				log.warn("Camera preview failed for {}", src.getDisplayName(), ex);
				final String msg = BSAppMessages.getString("CameraCaptureDialog.error.failed", ex.getMessage());
				Platform.runLater(() -> statusLabel.setText(msg));
			} finally {
				previewRunning = false;
				if (session != null)
					session.close();
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

	private record CaptureOutcome(BufferedImage image, String error) {
	}

	static void doHandsfree(Consumer<WritableImage> onSuccess, Runnable onUnavailable, Consumer<String> onError) {
		String sourceId = (String) BSApp.getProperty(PREF_HANDSFREE_SOURCE);
		if (sourceId == null)
			sourceId = (String) BSApp.getProperty(PREF_CAMERA);
		if (sourceId == null) {
			Platform.runLater(onUnavailable);
			return;
		}

		ICameraSource src = findSource(sourceId);
		if (src == null) {
			Platform.runLater(onUnavailable);
			return;
		}

		Dimension savedRes = parseDimension((String) BSApp.getProperty(PREF_RESOLUTION_PREFIX + sourceId));
		CaptureOutcome outcome = captureFrame(src, savedRes);

		BufferedImage captured = outcome.image();
		String error = outcome.error();
		if (captured == null || error != null) {
			final String msg = error != null ? error : BSAppMessages.getString("CameraCaptureDialog.error.unknown");
			Platform.runLater(() -> onError.accept(msg));
			return;
		}

		// Pre-rotation
		String savedRotStr = (String) BSApp.getProperty(PREF_ROTATION_PREFIX + sourceId);
		if (savedRotStr != null) {
			try {
				int rot = Integer.parseInt(savedRotStr);
				if (rot != 0)
					captured = applyRotation(captured, rot);
			} catch (NumberFormatException ignore) {
			}
		}

		// Autocrop
		BufferedImage result = captured;
		boolean autocrop = !"false".equals(BSApp.getProperty(PREF_HANDSFREE_AUTOCROP, "true"));
		if (autocrop) {
			java.awt.Rectangle cropRect = ImageCropPane.autocropRect(captured);
			if (cropRect != null && cropRect.width > 0 && cropRect.height > 0) {
				result = captured.getSubimage(cropRect.x, cropRect.y, cropRect.width, cropRect.height);
			}
		}

		// Downscale if a max dimension is configured
		int maxDimension = 0;
		try {
			maxDimension = Integer.parseInt((String) BSApp.getProperty(PREF_HANDSFREE_MAX_DIMENSION, "0"));
		} catch (NumberFormatException ignore) {
		}
		if (maxDimension > 0) {
			result = scaleAWT(result, maxDimension, maxDimension);
		}

		final BufferedImage finalResult = result;
		Platform.runLater(() -> onSuccess.accept(SwingFXUtils.toFXImage(finalResult, null)));
	}

	/**
	 * Resolves a saved {@link #PREF_CAMERA} id back to an {@link ICameraSource}:
	 * first among the registered {@code ICameraSource} singletons, then among
	 * currently-connected webcams. Returns {@code null} if neither matches.
	 */
	private static ICameraSource findSource(String sourceId) {
		for (ICameraSource src : Services.<ICameraSource>getServices(ICameraSource.class)) {
			if (src.getId().equals(sourceId))
				return src;
		}
		try {
			List<Webcam> cams = Webcam.getWebcams();
			for (int i = 0; i < cams.size(); i++) {
				if (cams.get(i).getName().equals(sourceId))
					return new WebcamCameraSource(cams.get(i), i);
			}
		} catch (Exception ex) {
			log.warn("Handsfree: failed to enumerate cameras", ex);
		}
		return null;
	}

	private static CaptureOutcome captureFrame(ICameraSource src, Dimension resolution) {
		BufferedImage captured = null;
		String error = null;

		ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "camera-capture");
			t.setDaemon(true);
			return t;
		});
		Future<BufferedImage> future = exec.submit(() -> src.grabFrame(resolution));
		try {
			captured = future.get(10, TimeUnit.SECONDS);
			if (captured == null)
				error = BSAppMessages.getString("CameraCaptureDialog.error.emptyFrame");
		} catch (TimeoutException ex) {
			future.cancel(true);
			error = BSAppMessages.getString("CameraCaptureDialog.error.timeout");
			log.warn("Handsfree capture timed out for {}", src.getDisplayName());
		} catch (Exception ex) {
			error = BSAppMessages.getString("CameraCaptureDialog.error.failed", ex.getMessage());
			log.warn("Handsfree capture failed", ex);
		} finally {
			exec.shutdownNow();
		}
		return new CaptureOutcome(captured, error);
	}

	/**
	 * Parses a {@code "WxH"} string as saved by {@link #PREF_RESOLUTION_PREFIX}, or
	 * {@code null}.
	 */
	private static Dimension parseDimension(String s) {
		if (s == null)
			return null;
		String[] parts = s.split("x", 2);
		if (parts.length != 2)
			return null;
		try {
			return new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
		} catch (NumberFormatException ex) {
			return null;
		}
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

	static class CameraSourceListCell extends ListCell<ICameraSource> {
		@Override
		protected void updateItem(ICameraSource item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty || item == null ? null : item.getDisplayName());
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
