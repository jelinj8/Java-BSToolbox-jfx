package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.AnchorPane;

public class AnchorPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		AnchorPane ap = new AnchorPane();
		FxAttrHelper.applyCommon(ap, file);
		return ap;
	}

	@Override
	public String getExtension() {
		return "AnchorPane";
	}
}
