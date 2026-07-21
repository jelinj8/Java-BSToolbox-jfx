package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;

/**
 * {@link ICameraSource} for a Raspberry Pi Camera Module, driven by shelling
 * out to the {@code rpicam-still}/{@code rpicam-vid} command-line tools (the
 * Bookworm-era successors to {@code libcamera-still}/{@code libcamera-vid} -
 * the command names are configurable for older images).
 *
 * <p>
 * <b>Rotation:</b> {@code rpicam-apps} only supports {@code --rotation 0} or
 * {@code 180} in hardware - 90/270 require a sensor/ISP transpose that isn't
 * supported and fail with {@code ERROR: *** transforms requiring transpose not
 * supported ***}. This class therefore never asks the camera to rotate at all;
 * it only exposes the hardware-safe {@link #hflip}/{@link #vflip} (pure flips,
 * no transpose). Arbitrary 90/270 rotation is left entirely to
 * {@code CameraCapturePane}'s existing per-source rotation combo, which already
 * rotates frames from any {@link ICameraSource} in software.
 *
 * <p>
 * Registered via the {@code /services} XmlFilesystem registry (instantiated
 * eagerly at startup, looked up with
 * {@code Services.getServices(ICameraSource.class)} from
 * {@code CameraCapturePane}):
 *
 * <pre>{@code
 * <file name="services">
 *     <file name="pi-camera" type="Class">
 *         <attribute name="class" value=
"cz.bliksoft.javautils.fx.controls.images.cam.RaspberryPiCameraSource"/>
 *         <attribute name="name" value="Pi Camera"/>
 *         <attribute name="resolutions" value="1920x1080,1280x720,640x480"/>
 *         <attribute name="hflip" value="false"/>
 *         <attribute name="vflip" value="false"/>
 *     </file>
 * </file>
 * }</pre>
 */
public final class RaspberryPiCameraSource implements ICameraSource {

	private static final Logger log = LogManager.getLogger(RaspberryPiCameraSource.class);

	/** Prefix for {@link #getId()}, used to recognize Pi camera ids. */
	public static final String ID_PREFIX = "rpicam:";

	/**
	 * Internal timeout for a single still capture, deliberately shorter than the
	 * caller's own {@code grabFrame} timeout (currently 10s in
	 * {@code CameraCapturePane}), so a hung subprocess is forcibly killed - and the
	 * camera device released - before the caller gives up and moves on.
	 */
	private static final long DEFAULT_STILL_TIMEOUT_MS = 8_000;

	/**
	 * Max time a preview {@link ICameraPreviewSession#readFrame()} waits for the
	 * next frame.
	 */
	private static final long DEFAULT_PREVIEW_FRAME_TIMEOUT_MS = 2_000;

	private final String id;
	private final String displayName;
	private final String stillCommand;
	private final String vidCommand;
	private final Dimension[] resolutions;
	private final boolean hflip;
	private final boolean vflip;
	private final long stillTimeoutMs;
	private final long previewFrameTimeoutMs;

	private volatile byte[] lastGrabbedBytes;

	public RaspberryPiCameraSource(FileObject fo) {
		this.id = ID_PREFIX + fo.getName();
		this.displayName = fo.getLocalizedAttribute("name", fo.getName());
		this.stillCommand = fo.getAttribute("stillCommand", "rpicam-still");
		this.vidCommand = fo.getAttribute("vidCommand", "rpicam-vid");
		this.resolutions = parseResolutions(fo.getAttribute("resolutions", null));
		this.hflip = fo.getBool("hflip", false);
		this.vflip = fo.getBool("vflip", false);
		this.stillTimeoutMs = fo.getInt("stillTimeoutMs", (int) DEFAULT_STILL_TIMEOUT_MS);
		this.previewFrameTimeoutMs = fo.getInt("previewFrameTimeoutMs", (int) DEFAULT_PREVIEW_FRAME_TIMEOUT_MS);
	}

	private static Dimension[] parseResolutions(String csv) {
		if (csv == null || csv.isBlank())
			return new Dimension[0];

		List<Dimension> result = new ArrayList<>();
		for (String part : csv.split(",")) {
			String s = part.trim();
			int x = s.indexOf('x');
			if (x <= 0 || x == s.length() - 1)
				continue;
			try {
				int w = Integer.parseInt(s.substring(0, x).trim());
				int h = Integer.parseInt(s.substring(x + 1).trim());
				result.add(new Dimension(w, h));
			} catch (NumberFormatException e) {
				log.warn("Ignoring malformed resolution entry '{}'", s);
			}
		}
		return result.toArray(new Dimension[0]);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public Dimension[] getAvailableResolutions() {
		return resolutions;
	}

	@Override
	public BufferedImage grabFrame(Dimension resolution) throws IOException, InterruptedException {
		List<String> cmd = new ArrayList<>();
		cmd.add(stillCommand);
		cmd.add("-n");
		cmd.add("--immediate");
		cmd.add("--encoding");
		cmd.add("jpg");
		addResolutionAndFlipArgs(cmd, resolution);
		cmd.add("-o");
		cmd.add("-");

		byte[] bytes = runCapture(cmd, stillTimeoutMs);
		lastGrabbedBytes = bytes;
		return bytes == null ? null : ImageIO.read(new ByteArrayInputStream(bytes));
	}

	@Override
	public byte[] grabFrameBytes(Dimension resolution) {
		return lastGrabbedBytes;
	}

	@Override
	public ICameraPreviewSession openPreview(Dimension resolution) throws IOException {
		List<String> cmd = new ArrayList<>();
		cmd.add(vidCommand);
		cmd.add("-n");
		cmd.add("-t");
		cmd.add("0");
		cmd.add("--codec");
		cmd.add("mjpeg");
		cmd.add("--low-latency");
		addResolutionAndFlipArgs(cmd, resolution);
		cmd.add("-o");
		cmd.add("-");

		Process process = new ProcessBuilder(cmd).start();
		Thread stderrDrain = startStderrDrain(process, vidCommand);
		return new PreviewSession(process, stderrDrain);
	}

	private void addResolutionAndFlipArgs(List<String> cmd, Dimension resolution) {
		if (resolution != null) {
			cmd.add("--width");
			cmd.add(String.valueOf(resolution.width));
			cmd.add("--height");
			cmd.add(String.valueOf(resolution.height));
		}
		if (hflip)
			cmd.add("--hflip");
		if (vflip)
			cmd.add("--vflip");
	}

	/**
	 * Runs a one-shot capture command, returning its stdout bytes or {@code null}
	 * on failure.
	 */
	private byte[] runCapture(List<String> cmd, long timeoutMs) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(cmd).start();
		Thread stderrDrain = startStderrDrain(process, cmd.get(0));
		try {
			byte[] bytes = process.getInputStream().readAllBytes();
			boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
			if (!finished) {
				log.warn("{} timed out after {}ms, killing process", cmd.get(0), timeoutMs);
				process.destroyForcibly();
				return null;
			}
			if (process.exitValue() != 0) {
				log.warn("{} exited with code {}", cmd.get(0), process.exitValue());
				return null;
			}
			return bytes.length == 0 ? null : bytes;
		} finally {
			if (process.isAlive())
				process.destroyForcibly();
			stderrDrain.interrupt();
		}
	}

	/**
	 * Drains a process's stderr on a background thread, logging each line at DEBUG.
	 */
	private Thread startStderrDrain(Process process, String label) {
		Thread t = new Thread(() -> {
			try (InputStream err = process.getErrorStream()) {
				byte[] buf = new byte[4096];
				int n;
				while ((n = err.read(buf)) != -1) {
					if (log.isDebugEnabled() && n > 0)
						log.debug("{}: {}", label, new String(buf, 0, n, StandardCharsets.UTF_8).strip());
				}
			} catch (IOException e) {
				// process ended; nothing to do
			}
		}, "rpicam-stderr-" + label);
		t.setDaemon(true);
		t.start();
		return t;
	}

	/**
	 * A dedicated background thread continuously parses MJPEG frames off the
	 * process's stdout and hands the latest one to a single-slot queue, so
	 * {@link #readFrame()} can wait for the next frame with an actual timeout
	 * (plain {@link InputStream#read()} has no timeout support).
	 */
	private final class PreviewSession implements ICameraPreviewSession {

		private final Process process;
		private final Thread stderrDrain;
		private final Thread readerThread;
		private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1);
		private volatile boolean stopped;
		private volatile IOException readerError;

		PreviewSession(Process process, Thread stderrDrain) {
			this.process = process;
			this.stderrDrain = stderrDrain;
			MjpegFrameReader reader = new MjpegFrameReader(process.getInputStream());
			this.readerThread = new Thread(() -> {
				try {
					byte[] frame;
					while (!stopped && (frame = reader.nextFrame()) != null) {
						queue.poll();
						queue.offer(frame);
					}
				} catch (IOException e) {
					readerError = e;
				}
			}, "rpicam-preview-reader-" + id);
			readerThread.setDaemon(true);
			readerThread.start();
		}

		@Override
		public BufferedImage readFrame() throws IOException, InterruptedException {
			if (readerError != null)
				throw readerError;

			byte[] frame = queue.poll(previewFrameTimeoutMs, TimeUnit.MILLISECONDS);
			if (frame == null) {
				if (!process.isAlive())
					throw new IOException(vidCommand + " process is no longer running");
				return null;
			}
			return ImageIO.read(new ByteArrayInputStream(frame));
		}

		@Override
		public void close() {
			stopped = true;
			process.destroyForcibly();
			readerThread.interrupt();
			stderrDrain.interrupt();
		}
	}
}
