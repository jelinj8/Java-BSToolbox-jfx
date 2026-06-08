package cz.bliksoft.javautils.fx.controls;

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

import cz.bliksoft.javautils.fx.controls.images.qr.QrCodeRenderer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

public class QRLabel extends Region {

	private static final Logger log = LogManager.getLogger();

	private final StringProperty text = new SimpleStringProperty();
	private final IntegerProperty modulusMultiplier = new SimpleIntegerProperty(4);
	private final ObjectProperty<ErrorCorrectionLevel> errorCorrectionLevel = new SimpleObjectProperty<>(
			ErrorCorrectionLevel.M);

	private final ImageView imageView = new ImageView();

	public QRLabel() {
		imageView.setSmooth(false);
		imageView.setPreserveRatio(true);
		getChildren().add(imageView);
		getStyleClass().add("qr-label");

		text.addListener((obs, o, n) -> render());
		modulusMultiplier.addListener((obs, o, n) -> render());
		errorCorrectionLevel.addListener((obs, o, n) -> render());
	}

	public StringProperty textProperty() {
		return text;
	}

	public String getText() {
		return text.get();
	}

	public void setText(String value) {
		text.set(value);
	}

	public IntegerProperty modulusMultiplierProperty() {
		return modulusMultiplier;
	}

	public int getModulusMultiplier() {
		return modulusMultiplier.get();
	}

	public void setModulusMultiplier(int value) {
		modulusMultiplier.set(value);
	}

	public ObjectProperty<ErrorCorrectionLevel> errorCorrectionLevelProperty() {
		return errorCorrectionLevel;
	}

	public ErrorCorrectionLevel getErrorCorrectionLevel() {
		return errorCorrectionLevel.get();
	}

	public void setErrorCorrectionLevel(ErrorCorrectionLevel value) {
		errorCorrectionLevel.set(value);
	}

	private void render() {
		String t = getText();
		if (t == null || t.isBlank()) {
			imageView.setImage(null);
			setPrefSize(0, 0);
			setMinSize(0, 0);
			setMaxSize(0, 0);
			return;
		}

		try {
			Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
			hints.put(EncodeHintType.ERROR_CORRECTION, getErrorCorrectionLevel());
			hints.put(EncodeHintType.MARGIN, 0);

			BitMatrix matrix = new QRCodeWriter().encode(t, BarcodeFormat.QR_CODE, 0, 0, hints);
			int mult = getModulusMultiplier();
			WritableImage img = QrCodeRenderer.rasterize(matrix, mult);
			int pixelSize = (int) img.getWidth();

			imageView.setImage(img);
			imageView.setFitWidth(pixelSize);
			imageView.setFitHeight(pixelSize);
			setPrefSize(pixelSize, pixelSize);
			setMinSize(pixelSize, pixelSize);
			setMaxSize(pixelSize, pixelSize);

		} catch (WriterException e) {
			log.warn("Failed to render QR code for text '{}': {}", t, e.getMessage());
			imageView.setImage(null);
		}
	}
}
