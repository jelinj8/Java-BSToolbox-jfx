package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import cz.bliksoft.javautils.fx.controls.QRLabel;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;

public class QRLabelFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject f) {
		QRLabel label = new QRLabel();

		String text = f.getLocalizedAttribute("text", null);
		if (text != null)
			label.setText(text);

		Integer mult = f.getInteger("modulusMultiplier", null);
		if (mult != null)
			label.setModulusMultiplier(mult);

		String ecl = f.getAttribute("errorCorrectionLevel", null);
		if (ecl != null)
			label.setErrorCorrectionLevel(ErrorCorrectionLevel.valueOf(ecl.toUpperCase()));

		return label;
	}

	@Override
	public String getSupportedType() {
		return "QRLabel";
	}

}
