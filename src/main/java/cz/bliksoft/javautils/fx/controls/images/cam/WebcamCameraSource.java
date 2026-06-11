package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import com.github.sarxos.webcam.Webcam;

/**
 * {@link ICameraSource} wrapping a physical Sarxos {@link Webcam}, as returned
 * by {@link Webcam#getWebcams()}.
 *
 * <p>
 * If {@code openCvIndex} is non-negative and OpenCV
 * ({@link OpenCvResolutionProbe#isAvailable()}) is available, resolution
 * discovery, capture, and preview use {@link OpenCvResolutionProbe} /
 * {@link OpenCvCapture} for resolutions beyond Sarxos's hardcoded 640x480 cap;
 * otherwise Sarxos's own capture is used.
 */
public final class WebcamCameraSource implements ICameraSource {

	/** Frame interval used to cap webcam preview at roughly 30 fps. */
	private static final int PREVIEW_FRAME_MS = 33;

	private final Webcam webcam;
	private final int openCvIndex;

	/**
	 * @param webcam      the Sarxos webcam to wrap
	 * @param openCvIndex the index of {@code webcam} in the same
	 *                    {@link Webcam#getWebcams()} call, used to align with
	 *                    OpenCV's device indexing; pass {@code -1} if unknown or
	 *                    unavailable
	 */
	public WebcamCameraSource(Webcam webcam, int openCvIndex) {
		this.webcam = webcam;
		this.openCvIndex = openCvIndex;
	}

	/** The wrapped Sarxos webcam. */
	public Webcam webcam() {
		return webcam;
	}

	@Override
	public String getId() {
		return webcam.getName();
	}

	@Override
	public String getDisplayName() {
		return webcam.getName();
	}

	private boolean useOpenCv() {
		return openCvIndex >= 0 && OpenCvResolutionProbe.isAvailable();
	}

	@Override
	public Dimension[] getAvailableResolutions() {
		Dimension[] sizes;
		CameraNativeLock.acquire();
		try {
			sizes = webcam.getViewSizes();
		} catch (Exception ex) {
			sizes = new Dimension[0];
		} finally {
			CameraNativeLock.release();
		}
		if (useOpenCv()) {
			try {
				Dimension[] probed = OpenCvResolutionProbe.probeResolutions(openCvIndex);
				if (probed != null && probed.length > 1)
					sizes = probed;
			} catch (Exception ignore) {
			}
		}
		return sizes;
	}

	@Override
	public BufferedImage grabFrame(Dimension resolution) throws Exception {
		if (useOpenCv()) {
			CameraNativeLock.acquire();
			try {
				return OpenCvCapture.grabFrame(openCvIndex, resolution);
			} finally {
				CameraNativeLock.release();
			}
		}
		if (resolution != null)
			webcam.setViewSize(resolution);
		CameraNativeLock.acquire();
		try {
			webcam.open();
			return webcam.getImage();
		} finally {
			// Hands the lock off to the closer thread - released once webcam.close()
			// completes.
			closeAsync(webcam, true);
		}
	}

	@Override
	public ICameraPreviewSession openPreview(Dimension resolution) {
		if (useOpenCv()) {
			CameraNativeLock.acquire();
			VideoCapture cap;
			try {
				cap = OpenCvCapture.openForPreview(openCvIndex, resolution);
			} finally {
				CameraNativeLock.release();
			}
			return cap == null ? null : new OpenCvPreviewSession(cap);
		}
		if (resolution != null)
			webcam.setViewSize(resolution);
		CameraNativeLock.acquire();
		try {
			webcam.open();
		} finally {
			CameraNativeLock.release();
		}
		return new WebcamPreviewSession(webcam);
	}

	/**
	 * Closes {@code webcam} on a daemon thread - {@link Webcam#close()} blocks
	 * while Sarxos's internal capture thread winds down (~20 s with OBS Virtual
	 * Camera).
	 *
	 * @param releaseLock if {@code true}, releases {@link CameraNativeLock} once
	 *                    {@link Webcam#close()} completes - used when the caller
	 *                    already held the lock for the preceding open/capture and
	 *                    hands it off here.
	 */
	static void closeAsync(Webcam webcam, boolean releaseLock) {
		Thread closer = new Thread(() -> {
			try {
				webcam.close();
			} catch (Exception ignore) {
			} finally {
				if (releaseLock)
					CameraNativeLock.release();
			}
		}, "camera-close");
		closer.setDaemon(true);
		closer.start();
	}

	private static void capFrameRate() {
		try {
			Thread.sleep(PREVIEW_FRAME_MS);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	private record OpenCvPreviewSession(VideoCapture cap) implements ICameraPreviewSession {
		@Override
		public BufferedImage readFrame() {
			BufferedImage frame = OpenCvCapture.readFrame(cap);
			capFrameRate();
			return frame;
		}

		@Override
		public void close() {
			cap.release();
		}
	}

	private record WebcamPreviewSession(Webcam webcam) implements ICameraPreviewSession {
		@Override
		public BufferedImage readFrame() {
			BufferedImage frame = webcam.getImage();
			capFrameRate();
			return frame;
		}

		@Override
		public void close() {
			closeAsync(webcam, false);
		}
	}
}
