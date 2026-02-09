package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.CheckBox;

public class CheckBoxFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {
		CheckBox cb = new CheckBox();

		Boolean selected = file.getBool("selected", null);
		if (selected != null)
			cb.setSelected(selected);

		Boolean indeterminate = file.getBool("indeterminate", null);
		if (indeterminate != null)
			cb.setIndeterminate(indeterminate);

		Boolean allowIndeterminate = file.getBool("allowIndeterminate", null);
		if (allowIndeterminate != null)
			cb.setAllowIndeterminate(allowIndeterminate);

		return cb;
	}

	@Override
	public String getExtension() {
		return "CheckBox";
	}
}
