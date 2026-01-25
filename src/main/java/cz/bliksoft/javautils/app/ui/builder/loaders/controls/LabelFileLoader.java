package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.Label;

public class LabelFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {
		Label result = new Label();
		result.setText(file.getAttribute("text", "!no text!"));

		String iconDef = file.getAttribute("icon", null);
		if (iconDef != null) {
			result.setGraphic(ImageUtils.getIconNode(iconDef));
		}
		return result;
	}

	@Override
	public String getExtension() {
		return "Label";
	}

}
