package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.MenuBar;

/**
 * {@code MenuBar}. Attributes:
 * <ul>
 * <li>{@code useSystemMenuBar} (boolean) - {@link MenuBar#setUseSystemMenuBar}:
 * on macOS the menus go to the system menu bar (the in-window bar takes no
 * space); ignored by JavaFX where there is no system menu bar.</li>
 * <li>{@code autoHideInFullScreen} (boolean) - hides the bar while its stage is
 * full screen and shows it on Alt / F10 / mouse at the top edge, see
 * {@link MenuBarAutoHide}; not applied while the system menu bar is used.</li>
 * </ul>
 */
public class MenuBarFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		MenuBar mb = new MenuBar();
		if (Boolean.TRUE.equals(file.getBool("useSystemMenuBar")))
			mb.setUseSystemMenuBar(true);
		if (Boolean.TRUE.equals(file.getBool("autoHideInFullScreen")))
			MenuBarAutoHide.install(mb);
		return mb;
	}

	@Override
	public String getSupportedType() {
		return "MenuBar";
	}
}
