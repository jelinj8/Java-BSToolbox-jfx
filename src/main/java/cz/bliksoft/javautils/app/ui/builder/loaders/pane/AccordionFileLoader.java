package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.Accordion;

public class AccordionFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		Accordion a = new Accordion();
		FxAttrHelper.applyRegionSizing(a, file);
		return a;
	}

	@Override
	public String getExtension() {
		return "Accordion";
	}
}
