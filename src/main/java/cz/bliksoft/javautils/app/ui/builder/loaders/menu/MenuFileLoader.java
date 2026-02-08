package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.Menu;

public class MenuFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		Menu m = new Menu();
		m.setText(file.getAttribute("text", file.getName()));
		m.setDisable(file.getBool("disable", false));
		return m;
	}

	@Override
	public String getExtension() {
		return "Menu";
	}
}
