package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

public class VBoxFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		VBox vb = new VBox();
		vb.setSpacing(file.getDouble("spacing", 0));
		vb.setAlignment(FxAttrHelper.pos(file, "alignment", Pos.TOP_LEFT));
		vb.setFillWidth(file.getBool("fillWidth", true));

		return vb;
	}

	@Override
	public String getExtension() {
		return "VBox";
	}
}
