package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.Button;

public class ButtonFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject f) {
		Button b = new Button();

		// defaultButton / cancelButton
		if (f.getAttribute("defaultButton", null) != null)
			b.setDefaultButton(f.getBool("defaultButton", false));
		if (f.getAttribute("cancelButton", null) != null)
			b.setCancelButton(f.getBool("cancelButton", false));

		return b;
	}

	@Override
	public String getExtension() {
		return "Button";
	}
}
