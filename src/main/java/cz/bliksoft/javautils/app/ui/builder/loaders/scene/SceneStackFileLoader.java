package cz.bliksoft.javautils.app.ui.builder.loaders.scene;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

public class SceneStackFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		double w = file.getDouble("width", 800);
		double h = file.getDouble("height", 600);

		StackPane root = new StackPane();
		Scene scene = new Scene(root, w, h);
		return scene;
	}

	@Override
	public String getExtension() {
		return "SceneStack";
	}
}
