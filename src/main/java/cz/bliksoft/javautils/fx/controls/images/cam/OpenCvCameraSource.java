package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_videoio.VideoCapture;

/**
 * {@link ICameraSource} for a USB/V4L2 camera captured purely via OpenCV, with
 * no dependency on Sarxos webcam-capture.
 *
 * <p>
 * Sarxos's built-in driver initializes native code (BridJ / OpenIMAJ) that only
 * ships x86/x86_64 binaries - it cannot be loaded on aarch64 Linux (e.g.
 * Raspberry Pi), so {@code Webcam.getWebcams()} always throws there and no
 * camera - including this one - can be discovered through it. Instances of this
 * class are produced by {@link OpenCvDeviceDiscovery#discover()} as a fallback
 * used only when Sarxos enumeration is unusable, so USB webcams still show up
 * in {@code CameraCapturePane}'s source combo on such platforms. See
 * {@link WebcamCameraSource} for the normal Sarxos-backed source, which already
 * uses OpenCV for capture/preview once a device has been found, but still
 * relies on Sarxos for discovery itself.
 */
public final class OpenCvCameraSource implements ICameraSource {

	/** Prefix for {@link #getId()}, used to recognize OpenCV-discovered ids. */
	public static final String ID_PREFIX = "opencv:";

	/** Frame interval used to cap preview at roughly 30 fps. */
	private static final int PREVIEW_FRAME_MS = 33;

	private final int deviceIndex;
	private final String displayName;

	OpenCvCameraSource(int deviceIndex, String displayName) {
		this.deviceIndex = deviceIndex;
		this.displayName = displayName;
	}

	@Override
	public String getId() {
		return ID_PREFIX + deviceIndex;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public Dimension[] getAvailableResolutions() {
		Dimension[] sizes = OpenCvResolutionProbe.probeResolutions(deviceIndex);
		return sizes != null ? sizes : new Dimension[0];
	}

	@Override
	public BufferedImage grabFrame(Dimension resolution) {
		CameraNativeLock.acquire();
		try {
			return OpenCvCapture.grabFrame(deviceIndex, resolution);
		} finally {
			CameraNativeLock.release();
		}
	}

	@Override
	public ICameraPreviewSession openPreview(Dimension resolution) {
		CameraNativeLock.acquire();
		VideoCapture cap;
		try {
			cap = OpenCvCapture.openForPreview(deviceIndex, resolution);
		} finally {
			CameraNativeLock.release();
		}
		return cap == null ? null : new PreviewSession(cap);
	}

	private record PreviewSession(VideoCapture cap) implements ICameraPreviewSession {
		@Override
		public BufferedImage readFrame() {
			BufferedImage frame = OpenCvCapture.readFrame(cap);
			try {
				Thread.sleep(PREVIEW_FRAME_MS);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
			return frame;
		}

		@Override
		public void close() {
			cap.release();
		}
	}
}
