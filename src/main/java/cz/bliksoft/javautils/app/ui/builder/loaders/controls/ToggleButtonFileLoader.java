package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.ToggleButton;

public class ToggleButtonFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject f) {
		ToggleButton b = new ToggleButton();
		FxAttrHelper.applyLabeled(b, f);
		FxAttrHelper.applyControl(b, f);
		FxAttrHelper.applyCommon(b, f);

		if (f.getAttribute("selected", null) != null)
			b.setSelected(f.getBool("selected", false));

		return b;
	}

	@Override
	public String getExtension() {
		return ".ToggleButton";
	}
}
