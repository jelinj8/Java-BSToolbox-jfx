package cz.bliksoft.javautils.fx.controls.images.cam;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Verifies {@link MjpegFrameReader} splits a concatenated-JPEG byte stream into
 * individual frames using length-aware marker-segment walking, rather than
 * naive scanning for {@code 0xFFD9} (which a segment payload could contain
 * without being an actual end-of-image).
 */
class MjpegFrameReaderTest {

	@Test
	void splitsConsecutiveFramesAndIgnoresEmbeddedEoiLikeBytesInSegmentPayload() throws IOException {
		byte[] leadingGarbage = { 0x00, 0x01, (byte) 0xFF, 0x00 };

		// APP0 payload deliberately contains the byte pair 0xFF 0xD9, which must be
		// skipped as part of the segment (via its length field) rather than
		// misread as an end-of-image marker.
		byte[] frame1 = buildJpeg(new byte[] { (byte) 0xFF, (byte) 0xD9, 0x00, 0x00 }, new byte[] { 1, 2, 3, 4 });
		byte[] frame2 = buildJpeg(new byte[] { 0x00, 0x00, 0x00, 0x00 }, new byte[] { 5, 6, 7 });

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(leadingGarbage);
		stream.write(frame1);
		stream.write(frame2);

		MjpegFrameReader reader = new MjpegFrameReader(new ByteArrayInputStream(stream.toByteArray()));

		assertArrayEquals(frame1, reader.nextFrame());
		assertArrayEquals(frame2, reader.nextFrame());
		assertNull(reader.nextFrame());
	}

	/**
	 * Builds a minimal synthetic JPEG: SOI, one APP0 (0xFFE0) segment carrying
	 * {@code app0Payload}, SOS (0xFFDA) with an empty scan header, then
	 * {@code scanData} as the entropy-coded payload, terminated by EOI.
	 */
	private static byte[] buildJpeg(byte[] app0Payload, byte[] scanData) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(0xFF);
		out.write(0xD8); // SOI

		out.write(0xFF);
		out.write(0xE0); // APP0
		int segLen = app0Payload.length + 2;
		out.write((segLen >> 8) & 0xFF);
		out.write(segLen & 0xFF);
		out.write(app0Payload);

		out.write(0xFF);
		out.write(0xDA); // SOS
		out.write(0x00);
		out.write(0x02); // empty scan header (length field only)

		out.write(scanData);

		out.write(0xFF);
		out.write(0xD9); // EOI

		return out.toByteArray();
	}
}
