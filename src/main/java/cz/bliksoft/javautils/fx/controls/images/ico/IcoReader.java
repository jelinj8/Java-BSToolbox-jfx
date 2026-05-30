package cz.bliksoft.javautils.fx.controls.images.ico;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Reads multi-frame Windows ICO files and converts the best-matching frame to a
 * JavaFX {@link Image}. Supports PNG-in-ICO (Vista+), 32-bpp BGRA DIB, 24-bpp
 * BGR DIB, and indexed (≤8-bpp) DIB frames.
 */
public class IcoReader {

	private static final Logger log = LogManager.getLogger();

	private static final int ICO_TYPE = 1;
	private static final int PNG_MAGIC = 0x89504E47;

	private record IcoEntry(int width, int height, int colorCount, int planes, int bitCount, int dataSize,
			int dataOffset) {
	}

	// ---- Public API --------------------------------------------------------

	public static Image loadFromFile(File file, Integer targetW, Integer targetH) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return load(fis, targetW, targetH);
		}
	}

	public static Image loadFromResource(String resourcePath, Integer targetW, Integer targetH) throws IOException {
		try (InputStream in = IcoReader.class.getResourceAsStream(resourcePath)) {
			if (in == null)
				throw new IOException("ICO resource not found: " + resourcePath);
			return load(in, targetW, targetH);
		}
	}

	// ---- Core loading ------------------------------------------------------

	private static Image load(InputStream in, Integer targetW, Integer targetH) throws IOException {
		byte[] all = in.readAllBytes();

		if (all.length < 6)
			throw new IOException("Not an ICO file: too short");

		int type = readUShortLE(all, 2);
		if (type != ICO_TYPE)
			throw new IOException("Not an ICO file: type=" + type);

		int count = readUShortLE(all, 4);
		if (count <= 0 || count > 256)
			throw new IOException("Invalid ICO frame count: " + count);

		List<IcoEntry> entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int base = 6 + i * 16;
			if (base + 16 > all.length) {
				log.warn("ICO directory truncated at entry {}", i);
				break;
			}
			IcoEntry e = buildEntry(all, base);
			if (e.dataOffset() + e.dataSize() > all.length) {
				log.warn("ICO entry {} out of bounds (offset={} size={}), skipping", i, e.dataOffset(), e.dataSize());
				continue;
			}
			entries.add(e);
		}

		if (entries.isEmpty())
			throw new IOException("ICO file has no valid entries");

		IcoEntry best = selectBestEntry(entries, targetW, targetH);
		BufferedImage bi = decodeFrame(all, best);
		return SwingFXUtils.toFXImage(bi, null);
	}

	private static IcoEntry buildEntry(byte[] b, int off) {
		int w = readUByte(b, off);
		int h = readUByte(b, off + 1);
		if (w == 0)
			w = 256;
		if (h == 0)
			h = 256;
		int colorCount = readUByte(b, off + 2);
		int planes = readUShortLE(b, off + 4);
		int bitCount = readUShortLE(b, off + 6);
		int dataSize = readIntLE(b, off + 8);
		int dataOffset = readIntLE(b, off + 12);
		return new IcoEntry(w, h, colorCount, planes, bitCount, dataSize, dataOffset);
	}

	// ---- Frame selection ---------------------------------------------------

	private static IcoEntry selectBestEntry(List<IcoEntry> entries, Integer targetW, Integer targetH) {
		int target = -1;
		if (targetW != null && targetH != null)
			target = Math.min(targetW, targetH);
		else if (targetW != null)
			target = targetW;
		else if (targetH != null)
			target = targetH;

		if (target < 0) {
			return entries.stream().max(Comparator.comparingInt(e -> e.width() * e.height())).orElseThrow();
		}

		final int t = target;

		// Pass 1: exact match, prefer higher bpp
		Optional<IcoEntry> exact = entries.stream().filter(e -> e.width() == t && e.height() == t)
				.max(Comparator.comparingInt(IcoEntry::bitCount));
		if (exact.isPresent())
			return exact.get();

		// Pass 2: next-larger — smallest frame still >= target
		Optional<IcoEntry> larger = entries.stream().filter(e -> e.width() >= t && e.height() >= t)
				.min(Comparator.comparingInt(e -> e.width() + e.height()));
		if (larger.isPresent())
			return larger.get();

		// Pass 3: largest available
		return entries.stream().max(Comparator.comparingInt(e -> e.width() * e.height())).orElseThrow();
	}

	// ---- Frame decoding ----------------------------------------------------

	private static BufferedImage decodeFrame(byte[] all, IcoEntry e) throws IOException {
		if (isPng(all, e.dataOffset()))
			return decodePngFrame(all, e);
		return decodeDibFrame(all, e);
	}

	private static boolean isPng(byte[] b, int off) {
		if (off + 4 > b.length)
			return false;
		return readIntLE(b, off) == PNG_MAGIC;
	}

	private static BufferedImage decodePngFrame(byte[] all, IcoEntry e) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(all, e.dataOffset(), e.dataSize())) {
			BufferedImage img = ImageIO.read(bais);
			if (img == null)
				throw new IOException("ImageIO failed to decode PNG frame in ICO");
			return ensureArgb(img);
		}
	}

	private static BufferedImage decodeDibFrame(byte[] all, IcoEntry e) throws IOException {
		int off = e.dataOffset();
		if (off + 40 > all.length)
			throw new IOException("ICO DIB header truncated");

		int biSize = readIntLE(all, off);
		if (biSize < 40)
			throw new IOException("ICO DIB biSize too small: " + biSize);

		int biBitCount = readUShortLE(all, off + 14);
		int biCompression = readIntLE(all, off + 16);

		if (biBitCount == 32 && (biCompression == 0 || biCompression == 3))
			return decode32bppDib(all, e, off);
		if (biBitCount == 24 && biCompression == 0)
			return decode24bppDib(all, e, off);
		if (biBitCount <= 8 && biCompression == 0)
			return decodeIndexedDibViaBmp(all, e, off);

		throw new IOException("Unsupported ICO DIB format: bpp=" + biBitCount + " compression=" + biCompression);
	}

	private static BufferedImage decode32bppDib(byte[] all, IcoEntry e, int dibOff) throws IOException {
		int w = e.width();
		int h = e.height();
		int biHeightRaw = readIntLE(all, dibOff + 8);
		int pixelDataOff = dibOff + 40; // no palette for 32bpp BI_RGB/BI_BITFIELDS

		// Skip 12 extra bytes if BI_BITFIELDS
		if (readIntLE(all, dibOff + 16) == 3)
			pixelDataOff += 12;

		if (pixelDataOff + w * h * 4 > all.length)
			throw new IOException("ICO 32bpp data truncated");

		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

		boolean allAlphaZero = true;
		int[] argbRows = new int[w * h];

		for (int row = 0; row < h; row++) {
			int srcRow = h - 1 - row; // bottom-up
			int rowOff = pixelDataOff + srcRow * w * 4;
			for (int col = 0; col < w; col++) {
				int bv = all[rowOff + col * 4] & 0xFF;
				int gv = all[rowOff + col * 4 + 1] & 0xFF;
				int rv = all[rowOff + col * 4 + 2] & 0xFF;
				int av = all[rowOff + col * 4 + 3] & 0xFF;
				if (av != 0)
					allAlphaZero = false;
				argbRows[row * w + col] = (av << 24) | (rv << 16) | (gv << 8) | bv;
			}
		}

		out.setRGB(0, 0, w, h, argbRows, 0, w);

		// XP-era ICO: alpha all zero → derive transparency from AND mask
		if (allAlphaZero && Math.abs(biHeightRaw) == 2 * h) {
			int andMaskOff = pixelDataOff + w * h * 4;
			applyAndMask(out, all, w, h, andMaskOff);
		}

		return out;
	}

	private static BufferedImage decode24bppDib(byte[] all, IcoEntry e, int dibOff) throws IOException {
		int w = e.width();
		int h = e.height();
		int biHeightRaw = readIntLE(all, dibOff + 8);
		int rowStride = ((w * 3 + 3) / 4) * 4;
		int pixelDataOff = dibOff + 40;
		boolean hasAndMask = Math.abs(biHeightRaw) == 2 * h;
		int andMaskOff = pixelDataOff + rowStride * h;
		int andRowStride = ((w + 31) / 32) * 4;

		if (pixelDataOff + rowStride * h > all.length)
			throw new IOException("ICO 24bpp data truncated");
		if (hasAndMask && andMaskOff + andRowStride * h > all.length)
			hasAndMask = false;

		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int row = 0; row < h; row++) {
			int srcRow = h - 1 - row;
			int rowOff = pixelDataOff + srcRow * rowStride;
			int andRowOff = andMaskOff + srcRow * andRowStride;
			for (int col = 0; col < w; col++) {
				int bv = all[rowOff + col * 3] & 0xFF;
				int gv = all[rowOff + col * 3 + 1] & 0xFF;
				int rv = all[rowOff + col * 3 + 2] & 0xFF;
				int av;
				if (hasAndMask) {
					int byteIdx = col / 8;
					int bitIdx = 7 - (col % 8);
					int andBit = (all[andRowOff + byteIdx] >> bitIdx) & 1;
					av = (andBit == 1) ? 0 : 255;
				} else {
					av = 255;
				}
				out.setRGB(col, row, (av << 24) | (rv << 16) | (gv << 8) | bv);
			}
		}
		return out;
	}

	private static BufferedImage decodeIndexedDibViaBmp(byte[] all, IcoEntry e, int dibOff) throws IOException {
		int w = e.width();
		int h = e.height();
		int biHeightRaw = readIntLE(all, dibOff + 8);
		int biBitCount = readUShortLE(all, dibOff + 14);
		int biClrUsed = readIntLE(all, dibOff + 32);
		int paletteEntries = (biClrUsed != 0) ? biClrUsed : (1 << biBitCount);
		int paletteBytes = paletteEntries * 4;
		int rowStride = ((w * biBitCount + 31) / 32) * 4;
		int pixelBytes = rowStride * h;

		boolean hasAndMask = Math.abs(biHeightRaw) == 2 * h;
		int andMaskOff = dibOff + 40 + paletteBytes + pixelBytes;

		// Reconstruct a standard BMP: file header + DIB header (biHeight corrected) +
		// palette + pixels
		byte[] dibCopy = Arrays.copyOfRange(all, dibOff, dibOff + 40 + paletteBytes + pixelBytes);
		writeIntLE(dibCopy, 8, h); // correct biHeight to actual (not 2*h)

		int pixelOffset = 14 + 40 + paletteBytes;
		int fileSize = pixelOffset + pixelBytes;
		byte[] bmp = new byte[fileSize];
		bmp[0] = 'B';
		bmp[1] = 'M';
		writeIntLE(bmp, 2, fileSize);
		writeIntLE(bmp, 6, 0);
		writeIntLE(bmp, 10, pixelOffset);
		System.arraycopy(dibCopy, 0, bmp, 14, dibCopy.length);

		BufferedImage raw;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bmp)) {
			raw = ImageIO.read(bais);
		}
		if (raw == null)
			throw new IOException("ImageIO failed to decode indexed DIB frame in ICO");

		raw = ensureArgb(raw);
		if (hasAndMask && andMaskOff + ((w + 31) / 32) * 4 * h <= all.length) {
			applyAndMask(raw, all, w, h, andMaskOff);
		}
		return raw;
	}

	// ---- AND mask application ----------------------------------------------

	private static void applyAndMask(BufferedImage img, byte[] all, int w, int h, int andMaskOff) {
		int andRowStride = ((w + 31) / 32) * 4;
		for (int row = 0; row < h; row++) {
			int srcRow = h - 1 - row;
			int andRowOff = andMaskOff + srcRow * andRowStride;
			for (int col = 0; col < w; col++) {
				int byteIdx = col / 8;
				int bitIdx = 7 - (col % 8);
				if (andRowOff + byteIdx >= all.length)
					break;
				int andBit = (all[andRowOff + byteIdx] >> bitIdx) & 1;
				if (andBit == 1) {
					img.setRGB(col, row, img.getRGB(col, row) & 0x00FFFFFF);
				} else {
					img.setRGB(col, row, img.getRGB(col, row) | 0xFF000000);
				}
			}
		}
	}

	// ---- Helpers -----------------------------------------------------------

	private static BufferedImage ensureArgb(BufferedImage src) {
		if (src.getType() == BufferedImage.TYPE_INT_ARGB)
			return src;
		BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return out;
	}

	private static int readUByte(byte[] b, int off) {
		return b[off] & 0xFF;
	}

	private static int readUShortLE(byte[] b, int off) {
		return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
	}

	private static int readIntLE(byte[] b, int off) {
		return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
	}

	private static void writeIntLE(byte[] b, int off, int val) {
		b[off] = (byte) val;
		b[off + 1] = (byte) (val >> 8);
		b[off + 2] = (byte) (val >> 16);
		b[off + 3] = (byte) (val >> 24);
	}
}
