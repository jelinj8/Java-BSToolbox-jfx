package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.GridPane;

public class GridPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		GridPane gp = new GridPane();

		gp.setHgap(file.getDouble("hgap", 0));
		gp.setVgap(file.getDouble("vgap", 0));

		return gp;
	}

	@Override
	public String getSupportedType() {
		return "GridPane";
	}
}
