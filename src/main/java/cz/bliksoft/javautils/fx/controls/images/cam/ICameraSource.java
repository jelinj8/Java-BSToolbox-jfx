package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Map;

import cz.bliksoft.javautils.fx.controls.images.CameraCapturePane;
import cz.bliksoft.javautils.fx.controls.images.ImageCropPane;

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

	/**
	 * Returns the original encoded bytes (e.g. JPEG) of the most recently grabbed
	 * frame, iff they represent the exact same pixels as that {@link #grabFrame}
	 * result - i.e. no source-side transform (such as EXIF-orientation correction)
	 * was applied when decoding. Returns {@code null} if unavailable, or if
	 * {@link #grabFrame}'s pixels differ from the source bytes.
	 */
	default byte[] grabFrameBytes(Dimension resolution) throws Exception {
		return null;
	}

	/**
	 * Well-known {@link #grabFrameMetadata} key: a {@link Boolean} - if
	 * {@code true}, the caller should run {@link ImageCropPane#autocrop()} on the
	 * captured image.
	 */
	String METADATA_AUTOCROP = "autocrop";

	/**
	 * Returns additional metadata about the most recently grabbed frame (e.g.
	 * whether the uploader requested auto-crop via {@link #METADATA_AUTOCROP}).
	 * Returns an empty map if the source has no metadata to offer.
	 */
	default Map<String, Object> grabFrameMetadata(Dimension resolution) throws Exception {
		return Map.of();
	}
}
