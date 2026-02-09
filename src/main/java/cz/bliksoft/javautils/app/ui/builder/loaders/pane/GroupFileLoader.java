package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.Group;

public class GroupFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		Group g = new Group();
		return g;
	}

	@Override
	public String getExtension() {
		return "Group";
	}
}
