package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

/**
 * Probes a camera's actually-supported capture resolutions via OpenCV.
 *
 * <p>
 * Sarxos webcam-capture's default driver hardcodes
 * {@code Webcam.getViewSizes()} to {@code {176x144, 320x240, 640x480}}
 * regardless of the camera's real capability. This class uses OpenCV's
 * {@link VideoCapture} to negotiate a set of common candidate resolutions with
 * the camera and report back what was actually accepted, giving access to
 * higher resolutions (e.g. the full resolution of a 2MP+ sensor).
 *
 * <p>
 * This class lives in BSToolbox-jfx, which declares
 * {@code org.bytedeco:opencv-platform} as {@code provided}. If the consuming
 * application does not provide it at runtime, {@link #isAvailable()} returns
 * {@code false} and callers should fall back to Sarxos's reported sizes.
 */
public final class OpenCvResolutionProbe {

	private static final Logger log = LogManager.getLogger(OpenCvResolutionProbe.class);

	/**
	 * Common UVC resolutions to probe, in addition to the guaranteed 640x480
	 * baseline.
	 */
	private static final Dimension[] CANDIDATES = { new Dimension(640, 480), new Dimension(1280, 720),
			new Dimension(1280, 960), new Dimension(1600, 1200), new Dimension(1920, 1080), new Dimension(2048, 1536),
			new Dimension(2592, 1944) };

	private static volatile Boolean available;

	private OpenCvResolutionProbe() {
	}

	/**
	 * Returns {@code true} if OpenCV's video capture API is usable on this runtime.
	 */
	public static boolean isAvailable() {
		Boolean result = available;
		if (result == null) {
			synchronized (OpenCvResolutionProbe.class) {
				result = available;
				if (result == null) {
					result = check();
					available = result;
				}
			}
		}
		return result;
	}

	private static boolean check() {
		CameraNativeLock.acquire();
		try (VideoCapture cap = new VideoCapture()) {
			return true;
		} catch (Throwable t) {
			log.info("OpenCV not available - camera resolution probing limited to Sarxos defaults (max 640x480)", t);
			return false;
		} finally {
			CameraNativeLock.release();
		}
	}

	/**
	 * Returns the OpenCV capture backend to use for the given device index,
	 * preferring DirectShow on Windows to align with Sarxos's native
	 * DirectShow-based device enumeration.
	 */
	static int backend() {
		String os = System.getProperty("os.name", "").toLowerCase();
		return os.contains("win") ? opencv_videoio.CAP_DSHOW : opencv_videoio.CAP_ANY;
	}

	/**
	 * Probes which of {@link #CANDIDATES} the camera at {@code deviceIndex}
	 * actually negotiates, returning the distinct negotiated sizes sorted ascending
	 * by area. Returns {@code null} if the device could not be opened.
	 */
	public static Dimension[] probeResolutions(int deviceIndex) {
		CameraNativeLock.acquire();
		try (VideoCapture cap = new VideoCapture(deviceIndex, backend())) {
			if (!cap.isOpened()) {
				log.debug("OpenCV could not open camera device index {}", deviceIndex);
				return null;
			}
			Set<Dimension> found = new LinkedHashSet<>();
			for (Dimension candidate : CANDIDATES) {
				cap.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, candidate.width);
				cap.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, candidate.height);
				int w = (int) Math.round(cap.get(opencv_videoio.CAP_PROP_FRAME_WIDTH));
				int h = (int) Math.round(cap.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT));
				if (w > 0 && h > 0)
					found.add(new Dimension(w, h));
			}
			Dimension[] result = found.toArray(new Dimension[0]);
			java.util.Arrays.sort(result, (a, b) -> (a.width * a.height) - (b.width * b.height));
			return result;
		} finally {
			CameraNativeLock.release();
		}
	}
}
