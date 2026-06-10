package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import cz.bliksoft.javautils.fx.controls.images.CameraCapturePane;

/**
 * A source of capture frames selectable in {@link CameraCapturePane}'s source
 * combo.
 *
 * <p>
 * Implementations include {@link WebcamCameraSource} (a physical Sarxos
 * {@code Webcam}, enumerated at runtime) and {@link NetworkCameraSource} (a
 * configured HTTP snapshot endpoint, registered as a singleton via the
 * {@code /singletons} XmlFilesystem registry). Applications may implement this
 * interface for other camera types and register them the same way -
 * {@link CameraCapturePane} drives resolution discovery, capture, and preview
 * entirely through this interface, with no type-specific handling.
 */
public interface ICameraSource {

	/**
	 * Stable key used for per-source preferences ({@code camera.lastSource},
	 * {@code camera.resolution.*}, {@code camera.rotation.*}).
	 */
	String getId();

	/** Display name shown in the source combo. */
	String getDisplayName();

	/**
	 * Returns the resolutions this source can capture at, or an empty array if not
	 * applicable or unknown - in which case the resolution combo is hidden. May
	 * perform blocking I/O; called from a background thread.
	 */
	default Dimension[] getAvailableResolutions() {
		return new Dimension[0];
	}

	/**
	 * Captures a single still frame, optionally at {@code resolution} (may be
	 * {@code null}, or ignored if unsupported). Returns {@code null} if no frame
	 * could be captured. Performs blocking I/O; called from a background thread
	 * with a timeout.
	 */
	BufferedImage grabFrame(Dimension resolution) throws Exception;

	/**
	 * Opens a live-preview session, optionally at {@code resolution} (may be
	 * {@code null}, or ignored if unsupported). Returns {@code null} if previewing
	 * is not supported or the device could not be opened. Performs blocking I/O;
	 * called from a background thread.
	 */
	ICameraPreviewSession openPreview(Dimension resolution) throws Exception;
}
