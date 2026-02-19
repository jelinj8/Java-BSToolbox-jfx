package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Orientation;
import javafx.scene.layout.TilePane;

public class TilePaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		TilePane tp = new TilePane();
		tp.setOrientation(FxAttrHelper.orientation(file, "orientation", Orientation.HORIZONTAL));
		tp.setHgap(file.getDouble("hgap", 0));
		tp.setVgap(file.getDouble("vgap", 0));
		if (file.getAttribute("prefColumns", null) != null)
			tp.setPrefColumns(file.getInt("prefColumns", tp.getPrefColumns()));
		if (file.getAttribute("prefRows", null) != null)
			tp.setPrefRows(file.getInt("prefRows", tp.getPrefRows()));
		if (file.getAttribute("prefTileWidth", null) != null)
			tp.setPrefTileWidth(file.getDouble("prefTileWidth", tp.getPrefTileWidth()));
		if (file.getAttribute("prefTileHeight", null) != null)
			tp.setPrefTileHeight(file.getDouble("prefTileHeight", tp.getPrefTileHeight()));
		return tp;
	}

	@Override
	public String getSupportedType() {
		return "TilePane";
	}
}
