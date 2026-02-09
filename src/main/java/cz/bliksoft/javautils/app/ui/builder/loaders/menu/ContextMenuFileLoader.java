package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.ContextMenu;

public class ContextMenuFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		return new ContextMenu();
	}

	@Override
	public String getExtension() {
		return "ContextMenu";
	}
}
