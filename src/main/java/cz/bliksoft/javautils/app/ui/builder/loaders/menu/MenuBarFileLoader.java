package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.MenuBar;

public class MenuBarFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		return new MenuBar();
	}

	@Override
	public String getExtension() {
		return "MenuBar";
	}
}
