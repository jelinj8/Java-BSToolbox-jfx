package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.Region;

public class RegionFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		Region bp = new Region();
		return bp;
	}

	@Override
	public String getSupportedType() {
		return "Region";
	}
}
