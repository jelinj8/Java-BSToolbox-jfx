package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.TextArea;

public class TextAreaFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		TextArea txt = new TextArea();

		String text = file.getAttribute("text");
		if (text != null)
			txt.setText(text);

		Boolean b = file.getBool("wrap");
		if (b != null)
			txt.setWrapText(b);

		Integer prefColCount = file.getInteger("columns");
		if (prefColCount != null)
			txt.setPrefColumnCount(prefColCount);

		Integer prefRowCount = file.getInteger("rows");
		if (prefRowCount != null)
			txt.setPrefRowCount(prefRowCount);

		return txt;
	}

	@Override
	public String getExtension() {
		return "TextArea";
	}
}
