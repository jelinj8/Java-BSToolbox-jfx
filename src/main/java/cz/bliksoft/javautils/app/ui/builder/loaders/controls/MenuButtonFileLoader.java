package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Side;
import javafx.scene.control.MenuButton;

public class MenuButtonFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject f) {
		MenuButton mb = new MenuButton();

		// popupSide="TOP|RIGHT|BOTTOM|LEFT"
		if (f.getAttribute("popupSide", null) != null) {
			Side s = FxAttrHelper.side(f, "popupSide", mb.getPopupSide());
			mb.setPopupSide(s);
		}

		return mb;
	}

	@Override
	public String getSupportedType() {
		return "MenuButton";
	}
}
