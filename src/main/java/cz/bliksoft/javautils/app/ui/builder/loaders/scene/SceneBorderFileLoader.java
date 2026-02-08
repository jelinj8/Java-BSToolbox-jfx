package cz.bliksoft.javautils.app.ui.builder.loaders.scene;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

public class SceneBorderFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		double w = file.getDouble("width", 800);
		double h = file.getDouble("height", 600);

		BorderPane root = new BorderPane();
		Scene scene = new Scene(root, w, h);

		// Optional fill:
		// scene.setFill(FxAttrHelper.color(file, "fill",
		// javafx.scene.paint.Color.WHITE));

		return scene;
	}

	@Override
	public String getExtension() {
		return "SceneBorder";
	}
}
