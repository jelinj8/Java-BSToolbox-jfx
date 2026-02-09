package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.RadioButton;

public class RadioButtonFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {
		RadioButton rb = new RadioButton();

		Boolean selected = file.getBool("selected");
		if (selected != null)
			rb.setSelected(selected);

		String data = file.getAttribute("data");
		if (data != null)
			rb.setUserData(data);

		return rb;
	}

	@Override
	public String getExtension() {
		return "RadioButton";
	}
}
