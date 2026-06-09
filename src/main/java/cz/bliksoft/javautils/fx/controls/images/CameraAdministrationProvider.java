package cz.bliksoft.javautils.fx.controls.images;

import cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages;
import cz.bliksoft.javautils.app.ui.interfaces.IAdministrationProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.scene.Node;

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
 */
public class CameraAdministrationProvider implements IAdministrationProvider {

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
			pane.initCameras();
			component = pane;
		}
		return component;
	}
}
