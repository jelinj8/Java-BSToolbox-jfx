package cz.bliksoft.javautils.fx.controls.images.qr;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import cz.bliksoft.javautils.barcodes.QRGenerator;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

/**
 * JavaFX-side adapter for {@link QRGenerator}: converts its
 * {@code BufferedImage} output to {@link WritableImage} for use by
 * {@code QRLabel} and the {@code *QR} iconspec command. This class — together
 * with {@code QRLabel} / {@code QRLabelFileLoader} — is the only place that
 * references {@code com.google.zxing.*} types, so that {@code ImageUtils}
 * (which is loaded for virtually every icon resolution) stays free of a hard
 * compile/load-time dependency on the QR library.
 */
public class QrCodeRenderer {

	/**
	 * Rasterizes a ZXing bit matrix into a black/white image, replicating each
	 * module into a {@code moduleSize}×{@code moduleSize} pixel block. Shared by
	 * {@code QRLabel} and the {@code *QR} iconspec command.
	 */
	public static WritableImage rasterize(BitMatrix matrix, int moduleSize) {
		return SwingFXUtils.toFXImage(QRGenerator.rasterize(matrix, moduleSize), null);
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
		return SwingFXUtils.toFXImage(QRGenerator.render(data, errorCorrectionLevel, moduleSize, targetSize), null);
	}
}
