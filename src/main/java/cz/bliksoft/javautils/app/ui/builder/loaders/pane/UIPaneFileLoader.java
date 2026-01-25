package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.BorderPane;

public class UIPaneFileLoader extends BorderPaneFileLoader {
	@Override
	public Object loadObject(FileObject file) {
		BorderPane bp = (BorderPane) super.loadObject(file);
		BSAppUI.mainPane = bp;
		return bp;
	}

	@Override
	public String getExtension() {
		return "UIPane";
	}
}
