package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.TextField;

public class TextFieldFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {
		TextField tf = new TextField();

		Integer prefColCount = file.getInteger("columns", null);
		if (prefColCount != null)
			tf.setPrefColumnCount(prefColCount);

		return tf;
	}

	@Override
	public String getSupportedType() {
		return "TextField";
	}
}
