package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

/**
 * Captures still frames from a camera via OpenCV at a given resolution.
 *
 * <p>
 * Used as the higher-resolution alternative to Sarxos webcam-capture, whose
 * default driver caps capture at 640x480. See {@link OpenCvResolutionProbe} for
 * resolution discovery.
 */
public final class OpenCvCapture {

	/**
	 * Number of frames to read and discard before keeping one, to allow
	 * auto-exposure to settle.
	 */
	private static final int WARMUP_FRAMES = 2;

	private OpenCvCapture() {
	}

	/**
	 * Opens the camera at {@code deviceIndex}, sets the requested resolution,
	 * captures a frame (discarding a few warm-up frames first) and returns it as a
	 * {@link BufferedImage}. Returns {@code null} if the device could not be opened
	 * or no frame could be read.
	 */
	public static BufferedImage grabFrame(int deviceIndex, Dimension resolution) {
		try (VideoCapture cap = new VideoCapture(deviceIndex, OpenCvResolutionProbe.backend())) {
			if (!cap.isOpened())
				return null;
			if (resolution != null) {
				cap.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, resolution.width);
				cap.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, resolution.height);
			}
			try (Mat mat = new Mat()) {
				BufferedImage result = null;
				for (int i = 0; i <= WARMUP_FRAMES; i++) {
					if (!cap.read(mat))
						return result;
					result = matToBufferedImage(mat);
				}
				return result;
			}
		}
	}

	/**
	 * Opens a {@link VideoCapture} for live preview. Caller is responsible for
	 * calling {@link VideoCapture#release()} when done.
	 */
	public static VideoCapture openForPreview(int deviceIndex, Dimension resolution) {
		VideoCapture cap = new VideoCapture(deviceIndex, OpenCvResolutionProbe.backend());
		if (!cap.isOpened()) {
			cap.release();
			return null;
		}
		if (resolution != null) {
			cap.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, resolution.width);
			cap.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, resolution.height);
		}
		return cap;
	}

	/**
	 * Reads a single frame from an already-opened capture, or {@code null} if none
	 * is available.
	 */
	public static BufferedImage readFrame(VideoCapture cap) {
		Mat mat = new Mat();
		try {
			if (!cap.read(mat))
				return null;
			return matToBufferedImage(mat);
		} finally {
			mat.release();
		}
	}

	/**
	 * Converts a {@code CV_8UC3} BGR {@link Mat} to a {@link BufferedImage} of type
	 * {@link BufferedImage#TYPE_3BYTE_BGR}, which uses the same byte layout so the
	 * pixel data can be copied directly.
	 */
	private static BufferedImage matToBufferedImage(Mat mat) {
		int w = mat.cols();
		int h = mat.rows();
		if (w <= 0 || h <= 0)
			return null;

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		byte[] dst = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

		long step = mat.step();
		long rowBytes = (long) w * mat.channels();
		ByteBuffer src = mat.data().capacity(step * h).limit(step * h).asByteBuffer();

		if (step == rowBytes) {
			src.get(dst, 0, dst.length);
		} else {
			for (int row = 0; row < h; row++) {
				src.position((int) (row * step));
				src.get(dst, (int) (row * rowBytes), (int) rowBytes);
			}
		}
		return img;
	}
}
