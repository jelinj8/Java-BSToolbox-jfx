package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.TitledPane;

public class TitledPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		TitledPane tp = new TitledPane();
		tp.setExpanded(file.getBool("expanded", false));
		tp.setCollapsible(file.getBool("collapsible", true));
		return tp;
	}

	@Override
	public String getSupportedType() {
		return "TitledPane";
	}
}
