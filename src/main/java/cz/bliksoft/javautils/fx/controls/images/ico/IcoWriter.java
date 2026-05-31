package cz.bliksoft.javautils.fx.controls.images.ico;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Writes multi-frame Windows ICO files using PNG-in-ICO encoding (Vista+
 * format). Each frame is stored as a raw PNG stream embedded in the ICO
 * container; this is the same format that {@link IcoReader} already handles on
 * the read side.
 */
public class IcoWriter {

	private IcoWriter() {
	}

	/**
	 * Writes an ICO file to {@code target} containing one frame per element of
	 * {@code frames}. Frames may have different sizes; typical icon sets use 16,
	 * 32, 48, and 256 px.
	 *
	 * @param target the destination file (created or overwritten)
	 * @param frames one or more images to embed as ICO frames; must not be empty
	 *
	 * @throws IllegalArgumentException if {@code frames} is empty
	 * @throws IOException              on any I/O error
	 */
	public static void write(File target, List<BufferedImage> frames) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(target)) {
			write(fos, frames);
		}
	}

	/**
	 * Writes an ICO stream to {@code out} containing one frame per element of
	 * {@code frames}.
	 *
	 * @param out    the destination stream (not closed by this method)
	 * @param frames one or more images to embed as ICO frames; must not be empty
	 *
	 * @throws IllegalArgumentException if {@code frames} is empty
	 * @throws IOException              on any I/O error
	 */
	public static void write(OutputStream out, List<BufferedImage> frames) throws IOException {
		if (frames == null || frames.isEmpty())
			throw new IllegalArgumentException("At least one frame is required");

		byte[][] pngData = new byte[frames.size()][];
		for (int i = 0; i < frames.size(); i++) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			if (!ImageIO.write(frames.get(i), "png", bos)) //$NON-NLS-1$
				throw new IOException("No PNG ImageIO writer available");
			pngData[i] = bos.toByteArray();
		}

		// Directory entries start immediately after the 6-byte header
		int dataOffset = 6 + 16 * frames.size();

		DataOutputStream dos = new DataOutputStream(out);

		// ICO header
		writeLE16(dos, 0); // reserved
		writeLE16(dos, 1); // type: 1 = ICO
		writeLE16(dos, frames.size()); // image count

		// Directory
		int offset = dataOffset;
		for (int i = 0; i < frames.size(); i++) {
			BufferedImage bi = frames.get(i);
			int w = bi.getWidth();
			int h = bi.getHeight();
			dos.writeByte(w >= 256 ? 0 : w); // 0 means 256 in ICO spec
			dos.writeByte(h >= 256 ? 0 : h);
			dos.writeByte(0); // color count (0 = truecolor)
			dos.writeByte(0); // reserved
			writeLE16(dos, 1); // planes
			writeLE16(dos, 32); // bit count
			writeLE32(dos, pngData[i].length);
			writeLE32(dos, offset);
			offset += pngData[i].length;
		}

		// Image data
		for (byte[] png : pngData) {
			dos.write(png);
		}
		dos.flush();
	}

	private static void writeLE16(DataOutputStream dos, int value) throws IOException {
		dos.writeByte(value & 0xFF);
		dos.writeByte((value >> 8) & 0xFF);
	}

	private static void writeLE32(DataOutputStream dos, int value) throws IOException {
		dos.writeByte(value & 0xFF);
		dos.writeByte((value >> 8) & 0xFF);
		dos.writeByte((value >> 16) & 0xFF);
		dos.writeByte((value >> 24) & 0xFF);
	}
}
