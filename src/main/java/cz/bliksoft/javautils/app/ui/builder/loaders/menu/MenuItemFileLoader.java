package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.app.ui.actions.ActionBinder;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.MenuItem;

public class MenuItemFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		MenuItem mi = new MenuItem();
		mi.setText(ActionBinder.withMnemonic(file.getLocalizedAttribute("text", file.getName()),
				file.getLocalizedAttribute("mnemonic", null)));
		mi.setDisable(file.getBool("disable", false));

		// Optional action id (you wire this to your action registry)
		// String action = file.getAttribute("action", null);
		// if (action != null) mi.setOnAction(e -> ctx.actions().invoke(action));

		return mi;
	}

	@Override
	public String getSupportedType() {
		return "MenuItem";
	}
}
