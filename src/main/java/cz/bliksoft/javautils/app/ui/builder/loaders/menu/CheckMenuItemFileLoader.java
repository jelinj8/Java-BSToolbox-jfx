package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.app.ui.actions.ActionBinder;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.CheckMenuItem;

public class CheckMenuItemFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		CheckMenuItem mi = new CheckMenuItem();
		mi.setText(ActionBinder.withMnemonic(file.getLocalizedAttribute("text", file.getName()),
				file.getLocalizedAttribute("mnemonic", null)));
		mi.setSelected(file.getBool("selected", false));
		mi.setDisable(file.getBool("disable", false));
		return mi;
	}

	@Override
	public String getSupportedType() {
		return "CheckMenuItem";
	}
}
