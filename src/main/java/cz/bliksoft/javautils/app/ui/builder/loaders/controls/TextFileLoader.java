package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class TextFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		Text txt = new Text();

		String text = file.getAttribute("text", null);
		if (text != null)
			txt.setText(text);

		Paint p = FxAttrHelper.getPaint(file, "color");
		if (p != null)
			txt.setFill(p);

		Font f = FxAttrHelper.getFont(file);
		if (f != null)
			txt.setFont(f);
		
		return txt;
	}

	@Override
	public String getSupportedType() {
		return "Text";
	}
}
