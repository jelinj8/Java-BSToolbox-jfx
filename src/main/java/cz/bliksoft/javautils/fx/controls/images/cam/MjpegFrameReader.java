package cz.bliksoft.javautils.fx.controls.images.cam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Splits a raw MJPEG byte stream (as produced by e.g. {@code rpicam-vid --codec
 * mjpeg -o -}) into individual JPEG frames.
 *
 * <p>
 * Naively scanning for the {@code 0xFFD9} end-of-image marker is unreliable,
 * since other JPEG marker segments can contain that same byte pair inside their
 * payload. Instead this walks marker segments by their own length field up to
 * the start-of-scan marker, then relies on JPEG byte-stuffing (a genuine
 * {@code 0xFF} inside entropy-coded scan data is always followed by
 * {@code 0x00} or a restart marker) to find the real end-of-image
 * unambiguously.
 *
 * <p>
 * Not specific to the Pi camera stack - any concatenated-JPEG byte stream can
 * be fed through {@link #nextFrame()}.
 */
final class MjpegFrameReader {

	private static final int MARKER_PREFIX = 0xFF;
	private static final int SOI = 0xD8;
	private static final int EOI = 0xD9;
	private static final int SOS = 0xDA;

	private final InputStream in;

	MjpegFrameReader(InputStream in) {
		this.in = in;
	}

	/**
	 * Reads and returns the next complete JPEG frame's bytes (including the SOI/EOI
	 * markers), or {@code null} if the stream ended before a complete frame could
	 * be read.
	 */
	byte[] nextFrame() throws IOException {
		if (!skipToStartOfImage())
			return null;

		ByteArrayOutputStream frame = new ByteArrayOutputStream();
		frame.write(MARKER_PREFIX);
		frame.write(SOI);

		if (!copySegmentsUntilScan(frame))
			return null;
		if (!copyScanDataUntilEndOfImage(frame))
			return null;

		return frame.toByteArray();
	}

	/**
	 * Discards bytes until (and including) the next SOI marker. Returns false at
	 * EOF.
	 */
	private boolean skipToStartOfImage() throws IOException {
		int prev = -1;
		int b;
		while ((b = in.read()) != -1) {
			if (prev == MARKER_PREFIX && b == SOI)
				return true;
			prev = b;
		}
		return false;
	}

	/**
	 * Copies marker segments (each led by {@code 0xFF <marker> <len-hi> <len-lo>})
	 * into {@code frame} until the start-of-scan marker is reached (whose header is
	 * copied too). Returns false at EOF.
	 */
	private boolean copySegmentsUntilScan(ByteArrayOutputStream frame) throws IOException {
		while (true) {
			int marker = readMarker(frame);
			if (marker < 0)
				return false;
			if (marker == SOS)
				return true;

			int hi = readByte(frame);
			int lo = readByte(frame);
			if (hi < 0 || lo < 0)
				return false;

			int length = (hi << 8) | lo;
			for (int i = 0; i < length - 2; i++) {
				if (readByte(frame) < 0)
					return false;
			}
		}
	}

	/**
	 * Reads the next {@code 0xFF <marker>} pair, writing both bytes to
	 * {@code frame}. Any {@code 0xFF} fill bytes preceding the marker code are
	 * consumed and copied too. Returns -1 at EOF or if the stream isn't positioned
	 * at a marker.
	 */
	private int readMarker(ByteArrayOutputStream frame) throws IOException {
		int b = readByte(frame);
		if (b != MARKER_PREFIX)
			return -1;
		int marker;
		do {
			marker = readByte(frame);
			if (marker < 0)
				return -1;
		} while (marker == MARKER_PREFIX);
		return marker;
	}

	/**
	 * Copies entropy-coded scan data into {@code frame} until the unescaped EOI
	 * marker is found (also copied). Returns false at EOF.
	 */
	private boolean copyScanDataUntilEndOfImage(ByteArrayOutputStream frame) throws IOException {
		int prev = -1;
		while (true) {
			int b = readByte(frame);
			if (b < 0)
				return false;
			if (prev == MARKER_PREFIX && b == EOI)
				return true;
			prev = b == MARKER_PREFIX ? MARKER_PREFIX : -1;
		}
	}

	private int readByte(ByteArrayOutputStream frame) throws IOException {
		int b = in.read();
		if (b >= 0)
			frame.write(b);
		return b;
	}
}
