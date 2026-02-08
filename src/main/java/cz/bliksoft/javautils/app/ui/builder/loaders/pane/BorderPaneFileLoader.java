package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.BorderPane;

public class BorderPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		BorderPane bp = new BorderPane();

		FxAttrHelper.applyCommon(bp, file);
		return bp;
	}

	@Override
	public String getExtension() {
		return "BorderPane";
	}
}
