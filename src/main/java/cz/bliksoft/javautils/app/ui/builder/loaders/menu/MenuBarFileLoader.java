package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.MenuBar;

/**
 * {@code MenuBar}; attribute {@code autoHideInFullScreen} (boolean) hides it
 * while its stage is full screen and shows it on Alt / F10 / mouse at the top
 * edge - see {@link MenuBarAutoHide}.
 */
public class MenuBarFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		MenuBar mb = new MenuBar();
		if (Boolean.TRUE.equals(file.getBool("autoHideInFullScreen")))
			MenuBarAutoHide.install(mb);
		return mb;
	}

	@Override
	public String getSupportedType() {
		return "MenuBar";
	}
}
