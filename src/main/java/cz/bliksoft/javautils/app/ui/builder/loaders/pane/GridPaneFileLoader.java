package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.GridPane;

public class GridPaneFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        GridPane gp = new GridPane();

        gp.setHgap(FxAttrHelper.d(file, "hgap", 0));
        gp.setVgap(FxAttrHelper.d(file, "vgap", 0));

        FxAttrHelper.applyRegionSizing(gp, file);
        return gp;
    }

    @Override public String getExtension() { return "GridPane"; }
}
