package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.Label;

public class LabelFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {
		Label result = new Label();

		return result;
	}

	@Override
	public String getSupportedType() {
		return "Label";
	}

}
