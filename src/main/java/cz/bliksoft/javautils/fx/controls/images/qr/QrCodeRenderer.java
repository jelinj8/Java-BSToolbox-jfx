package cz.bliksoft.javautils.fx.controls.images.qr;

import java.util.EnumMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import cz.bliksoft.javautils.StringUtils;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Encodes and rasterizes QR codes via ZXing. This class — together with
 * {@code QRLabel} / {@code QRLabelFileLoader} — is the only place that
 * references {@code com.google.zxing.*} types, so that {@code ImageUtils}
 * (which is loaded for virtually every icon resolution) stays free of a hard
 * compile/load-time dependency on the QR library.
 */
public class QrCodeRenderer {

	private static final Logger log = LogManager.getLogger();

	/**
	 * Rasterizes a ZXing bit matrix into a black/white image, replicating each
	 * module into a {@code moduleSize}×{@code moduleSize} pixel block. Shared by
	 * {@code QRLabel} and the {@code *QR} iconspec command.
	 */
	public static WritableImage rasterize(BitMatrix matrix, int moduleSize) {
		int modules = matrix.getWidth();
		int mult = Math.max(1, moduleSize);
		int pixelSize = modules * mult;

		WritableImage img = new WritableImage(pixelSize, pixelSize);
		PixelWriter pw = img.getPixelWriter();
		int[] row = new int[pixelSize];

		for (int y = 0; y < modules; y++) {
			for (int x = 0; x < modules; x++) {
				int argb = matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
				for (int dx = 0; dx < mult; dx++)
					row[x * mult + dx] = argb;
			}
			for (int dy = 0; dy < mult; dy++)
				pw.setPixels(0, y * mult + dy, pixelSize, 1, PixelFormat.getIntArgbInstance(), row, 0, pixelSize);
		}
		return img;
	}

	/**
	 * Encodes {@code data} as a QR code and rasterizes it for iconspec use.
	 * {@code errorCorrectionLevel} defaults to {@code M} (and falls back to it with
	 * a warning if unrecognized). {@code moduleSize} defaults to 2 pixels per
	 * module; if {@code targetSize} is given, the per-module pixel size is instead
	 * derived from the matrix's module count to best fit that overall size, and
	 * {@code moduleSize} is ignored.
	 */
	public static WritableImage render(String data, String errorCorrectionLevel, Integer moduleSize, Integer targetSize)
			throws WriterException {
		ErrorCorrectionLevel ec = ErrorCorrectionLevel.M;
		if (StringUtils.hasLength(errorCorrectionLevel)) {
			try {
				ec = ErrorCorrectionLevel.valueOf(errorCorrectionLevel.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				log.warn("Unknown QR error-correction level '{}', defaulting to M", errorCorrectionLevel);
			}
		}

		Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
		hints.put(EncodeHintType.ERROR_CORRECTION, ec);
		hints.put(EncodeHintType.MARGIN, 0);
		BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, hints);

		int moduleSz;
		if (targetSize != null && targetSize > 0)
			moduleSz = Math.max(1, targetSize / matrix.getWidth());
		else if (moduleSize != null && moduleSize > 0)
			moduleSz = moduleSize;
		else
			moduleSz = 2;

		return rasterize(matrix, moduleSz);
	}
}
