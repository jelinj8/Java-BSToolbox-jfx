package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.TitledPane;

public class TitledPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		TitledPane tp = new TitledPane();
		tp.setText(file.getAttribute("text", file.getName()));
		tp.setExpanded(file.getBool("expanded", false));
		tp.setCollapsible(file.getBool("collapsible", true));
		FxAttrHelper.applyCommon(tp, file);
		return tp;
	}

	@Override
	public String getExtension() {
		return "TitledPane";
	}
}
