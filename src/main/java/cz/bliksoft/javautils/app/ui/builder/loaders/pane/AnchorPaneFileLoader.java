package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.AnchorPane;

public class AnchorPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		AnchorPane ap = new AnchorPane();
		return ap;
	}

	@Override
	public String getSupportedType() {
		return "AnchorPane";
	}
}
