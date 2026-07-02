package cz.bliksoft.javautils.fx.controls.images;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSAppJFX;
import cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages;
import cz.bliksoft.javautils.app.ui.interfaces.IAdministrationProvider;
import cz.bliksoft.javautils.exceptions.ViewableException;
import cz.bliksoft.javautils.fx.controls.images.cam.ICameraSource;
import cz.bliksoft.javautils.fx.controls.images.cam.NetworkCameraSource;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Administration provider that exposes {@link CameraCapturePane} as a camera
 * configuration and test panel.
 *
 * <p>
 * Operators can select a camera, set its preferred capture resolution and
 * pre-rotation, and test a live capture — all preferences are persisted
 * automatically via the same mechanism used by {@link CameraCaptureDialog}.
 *
 * <p>
 * Register this provider in your application module XML under
 * {@code core/administration}:
 *
 * <pre>{@code
 * <file name="camera">
 *     <file name=
"cz.bliksoft.javautils.fx.controls.images.CameraAdministrationProvider"/>
 * </file>
 * }</pre>
 *
 * <p>
 * This class relies on the Sarxos webcam-capture library, which BSToolbox-jfx
 * declares as {@code provided}. Only register this provider in applications
 * that include Sarxos at runtime.
 *
 * <p>
 * BSToolbox-jfx also declares {@code org.bytedeco:opencv-platform} as
 * {@code provided}; if the application provides it, the resolution list and
 * test capture reflect the camera's real capability instead of Sarxos's 640x480
 * cap.
 *
 * <p>
 * The camera source list also includes any {@link NetworkCameraSource}s
 * registered for the application (HTTP snapshot endpoints, e.g. a phone running
 * an IP camera app), with no extra dependency required.
 */
public class CameraAdministrationProvider implements IAdministrationProvider {

	private static final Logger log = LogManager.getLogger(CameraAdministrationProvider.class);

	private Node component;

	@Override
	public String getKey() {
		return "camera";
	}

	@Override
	public String getTreeTitle() {
		return BSAppAdministrationMessages.getString("CameraAdministrationProvider.treeTitle");
	}

	@Override
	public String getPanelTitle() {
		return BSAppAdministrationMessages.getString("CameraAdministrationProvider.panelTitle");
	}

	@Override
	public Node getSmallIcon() {
		return ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/camera"));
	}

	@Override
	public Node getAdministrationComponent() {
		if (component == null) {
			CameraCapturePane pane = new CameraCapturePane();
			pane.setPreviewEnabled(true);

			VBox box = new VBox(8);
			box.setPadding(new Insets(0, 0, 8, 8));
			box.getChildren().addAll(pane, buildHandsfreeSection(pane));
			VBox.setVgrow(pane, Priority.ALWAYS);

			pane.initCameras();
			component = box;
		}
		return component;
	}

	/**
	 * Builds the "Handsfree capture" settings section: camera selection, autocrop
	 * toggle, and optional max-dimension downscaling, used by
	 * {@link CameraCaptureDialog#captureHandsfree(java.util.function.Consumer, Runnable, java.util.function.Consumer)}.
	 */
	private Node buildHandsfreeSection(CameraCapturePane pane) {
		Label title = new Label(BSAppAdministrationMessages.getString("CameraAdministrationProvider.handsfree.title"));
		title.getStyleClass().add("h3");

		ComboBox<ICameraSource> sourceCombo = new ComboBox<>();
		sourceCombo.setCellFactory(lv -> new CameraCapturePane.CameraSourceListCell());
		sourceCombo.setButtonCell(new CameraCapturePane.CameraSourceListCell());
		sourceCombo.setMinWidth(200);
		pane.setSourcesLoadedListener(sources -> {
			sourceCombo.setItems(FXCollections.observableArrayList(sources));
			if (sources.isEmpty())
				return;
			String savedId = (String) BSAppJFX.getProperty(CameraCapturePane.PREF_HANDSFREE_SOURCE);
			ICameraSource preferred = sources.stream().filter(s -> s.getId().equals(savedId)).findFirst()
					.orElse(sources.get(0));
			sourceCombo.getSelectionModel().select(preferred);
		});
		sourceCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			if (n != null)
				saveProperty(CameraCapturePane.PREF_HANDSFREE_SOURCE, n.getId());
		});

		CheckBox autocropCheck = new CheckBox(
				BSAppAdministrationMessages.getString("CameraAdministrationProvider.handsfree.autocropCheckbox"));
		autocropCheck
				.setSelected(!"false".equals(BSAppJFX.getProperty(CameraCapturePane.PREF_HANDSFREE_AUTOCROP, "true")));
		autocropCheck.selectedProperty()
				.addListener((obs, o, n) -> saveProperty(CameraCapturePane.PREF_HANDSFREE_AUTOCROP, String.valueOf(n)));

		int savedMaxDimension;
		try {
			savedMaxDimension = Integer
					.parseInt((String) BSAppJFX.getProperty(CameraCapturePane.PREF_HANDSFREE_MAX_DIMENSION, "0"));
		} catch (NumberFormatException ex) {
			savedMaxDimension = 0;
		}
		Spinner<Integer> maxDimensionSpinner = new Spinner<>(
				new IntegerSpinnerValueFactory(0, 8000, savedMaxDimension, 100));
		maxDimensionSpinner.setEditable(true);
		maxDimensionSpinner.valueProperty().addListener((obs, o,
				n) -> saveProperty(CameraCapturePane.PREF_HANDSFREE_MAX_DIMENSION, String.valueOf(n == null ? 0 : n)));

		GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(8);
		grid.add(new Label(BSAppAdministrationMessages.getString("CameraAdministrationProvider.handsfree.sourceLabel")),
				0, 0);
		grid.add(new Label(
				BSAppAdministrationMessages.getString("CameraAdministrationProvider.handsfree.maxDimensionLabel")), 1,
				0);
		grid.add(sourceCombo, 0, 1);
		grid.add(maxDimensionSpinner, 1, 1);
		grid.add(autocropCheck, 0, 2, 2, 1);

		VBox section = new VBox(8, title, grid);
		section.setPadding(new Insets(8, 0, 0, 0));
		section.setFillWidth(true);
		section.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
		section.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
		return section;
	}

	private void saveProperty(String key, String value) {
		BSAppJFX.setLocalProperty(key, value);
		try {
			BSAppJFX.saveLocalProperties();
		} catch (ViewableException ex) {
			log.warn("Failed to save camera preference {}", key, ex);
		}
	}
}
