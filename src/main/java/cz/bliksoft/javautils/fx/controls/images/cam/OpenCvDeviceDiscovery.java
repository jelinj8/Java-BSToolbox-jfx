package cz.bliksoft.javautils.fx.controls.images.cam;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

/**
 * Enumerates V4L2 capture devices ({@code /dev/videoN}) directly via OpenCV,
 * without going through Sarxos webcam-capture.
 *
 * <p>
 * Used by {@code CameraCapturePane} as a fallback camera source when Sarxos's
 * {@code Webcam.getWebcams()} itself is unusable (e.g. its native BridJ /
 * OpenIMAJ driver has no aarch64 build, so it always throws on a Raspberry
 * Pi) - see {@link OpenCvCameraSource}. Linux-only: on other platforms Sarxos
 * enumeration works, so this is never invoked there.
 */
public final class OpenCvDeviceDiscovery {

	private static final Logger log = LogManager.getLogger(OpenCvDeviceDiscovery.class);

	private static final Pattern VIDEO_NODE = Pattern.compile("video(\\d+)");

	private OpenCvDeviceDiscovery() {
	}

	/**
	 * Probes every {@code /dev/videoN} node backed by the {@code uvcvideo} driver (see
	 * {@link #isUvcDevice(int)}) and returns an {@link OpenCvCameraSource} for each one
	 * that actually yields a frame, sorted by device index. Returns an empty list on
	 * non-Linux platforms or if {@code /dev} cannot be listed.
	 */
	public static List<OpenCvCameraSource> discover() {
		List<OpenCvCameraSource> result = new ArrayList<>();
		if (!System.getProperty("os.name", "").toLowerCase().contains("nux"))
			return result;

		File[] nodes = new File("/dev").listFiles((dir, name) -> VIDEO_NODE.matcher(name).matches());
		if (nodes == null)
			return result;

		List<Integer> indices = new ArrayList<>();
		for (File node : nodes) {
			Matcher m = VIDEO_NODE.matcher(node.getName());
			if (m.matches())
				indices.add(Integer.parseInt(m.group(1)));
		}
		indices.sort(Comparator.naturalOrder());

		for (int index : indices) {
			if (isUvcDevice(index) && canCapture(index))
				result.add(new OpenCvCameraSource(index, deviceName(index)));
		}
		return result;
	}

	/**
	 * Checks whether {@code /dev/videoN} is backed by the {@code uvcvideo} kernel
	 * driver (the standard Linux USB Video Class driver used by virtually every USB
	 * webcam), via the standard sysfs layout
	 * {@code /sys/class/video4linux/videoN/device/driver} - a symlink whose target's
	 * file name is the bound driver's name.
	 *
	 * <p>
	 * A Raspberry Pi's internal ISP/codec media pipeline (RP1's {@code rp1-cfe},
	 * {@code pispbe}, {@code bcm2835-codec}, etc.) exposes dozens of unrelated
	 * {@code /dev/videoN} nodes that are never {@code uvcvideo}-backed; several of
	 * them "open" successfully via OpenCV's V4L2 backend but then block for ~10s on a
	 * frame read that never arrives (OpenCV's hardcoded V4L2 read timeout - not
	 * configurable via {@link VideoCapture} properties). Filtering to {@code uvcvideo}
	 * nodes first avoids ever opening/reading those, without which
	 * {@link #discover()} would take tens of seconds on such hardware.
	 */
	private static boolean isUvcDevice(int index) {
		Path driverLink = Path.of("/sys/class/video4linux/video" + index + "/device/driver");
		try {
			return "uvcvideo".equals(Files.readSymbolicLink(driverLink).getFileName().toString());
		} catch (Exception ex) {
			log.debug("Could not resolve driver for /dev/video{}", index, ex);
			return false;
		}
	}

	/**
	 * Some {@code /dev/videoN} nodes exposed by a V4L2 device are metadata-only
	 * (e.g. UVC controls), not actual capture nodes - opening them "succeeds" but no
	 * frame can be read. A single frame read distinguishes real capture devices.
	 */
	private static boolean canCapture(int index) {
		CameraNativeLock.acquire();
		try (VideoCapture cap = new VideoCapture(index, OpenCvResolutionProbe.backend())) {
			if (!cap.isOpened())
				return false;
			try (Mat mat = new Mat()) {
				return cap.read(mat) && !mat.empty();
			}
		} catch (Exception ex) {
			log.debug("OpenCV could not probe /dev/video{}", index, ex);
			return false;
		} finally {
			CameraNativeLock.release();
		}
	}

	/**
	 * Reads the device's friendly name from {@code /sys/class/video4linux/videoN/name},
	 * falling back to {@code "Camera N"} if unavailable.
	 */
	private static String deviceName(int index) {
		Path namePath = Path.of("/sys/class/video4linux/video" + index + "/name");
		try {
			String name = Files.readString(namePath, StandardCharsets.UTF_8).strip();
			if (!name.isBlank())
				return name;
		} catch (Exception ignore) {
		}
		return "Camera " + index;
	}
}
