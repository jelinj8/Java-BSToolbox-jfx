package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.SeparatorMenuItem;

public class SeparatorMenuItemFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		return new SeparatorMenuItem();
	}

	@Override
	public String getExtension() {
		return "SeparatorMenuItem";
	}
}
