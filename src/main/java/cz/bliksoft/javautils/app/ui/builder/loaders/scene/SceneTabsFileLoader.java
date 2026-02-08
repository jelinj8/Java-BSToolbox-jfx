package cz.bliksoft.javautils.app.ui.builder.loaders.scene;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;

public class SceneTabsFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		double w = file.getDouble("width", 1000);
		double h = file.getDouble("height", 700);

		TabPane root = new TabPane();
		Scene scene = new Scene(root, w, h);
		return scene;
	}

	@Override
	public String getExtension() {
		return "SceneTabs";
	}
}
