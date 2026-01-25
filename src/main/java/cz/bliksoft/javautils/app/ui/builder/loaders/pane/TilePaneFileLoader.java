package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Orientation;
import javafx.scene.layout.TilePane;

public class TilePaneFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        TilePane tp = new TilePane();
        tp.setOrientation(FxAttrHelper.orientation(file, "orientation", Orientation.HORIZONTAL));
        tp.setHgap(FxAttrHelper.d(file, "hgap", 0));
        tp.setVgap(FxAttrHelper.d(file, "vgap", 0));
        if (file.getAttribute("prefColumns", null) != null) tp.setPrefColumns(FxAttrHelper.i(file, "prefColumns", tp.getPrefColumns()));
        if (file.getAttribute("prefRows", null) != null) tp.setPrefRows(FxAttrHelper.i(file, "prefRows", tp.getPrefRows()));
        if (file.getAttribute("prefTileWidth", null) != null) tp.setPrefTileWidth(FxAttrHelper.d(file, "prefTileWidth", tp.getPrefTileWidth()));
        if (file.getAttribute("prefTileHeight", null) != null) tp.setPrefTileHeight(FxAttrHelper.d(file, "prefTileHeight", tp.getPrefTileHeight()));
        FxAttrHelper.applyCommon(tp, file);
        return tp;
    }
    @Override public String getExtension() { return "TilePane"; }
}
