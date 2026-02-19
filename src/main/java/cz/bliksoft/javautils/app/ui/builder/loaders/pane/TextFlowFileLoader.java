package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class TextFlowFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		TextFlow tf = new TextFlow();

		String align = file.getAttribute("align", null);
		if (align != null) {
			TextAlignment a = TextAlignment.valueOf(align.toUpperCase());
			tf.setTextAlignment(a);
		}

		Double lineSpacing = file.getDouble("lineSpacing", null);
		if (lineSpacing != null)
			tf.setLineSpacing(lineSpacing);

		return tf;
	}

	@Override
	public String getSupportedType() {
		return "TextFlow";
	}
}
