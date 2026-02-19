package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

public class HBoxFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		HBox hb = new HBox();
		hb.setSpacing(file.getDouble("spacing", 0));
		hb.setAlignment(FxAttrHelper.pos(file, "alignment", Pos.TOP_LEFT));
		hb.setFillHeight(file.getBool("fillHeight", true));

		return hb;
	}

	@Override
	public String getSupportedType() {
		return "HBox";
	}
}
