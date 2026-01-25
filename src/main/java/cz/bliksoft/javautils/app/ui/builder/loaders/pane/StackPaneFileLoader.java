package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

public class StackPaneFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        StackPane sp = new StackPane();
        sp.setAlignment(FxAttrHelper.pos(file, "alignment", Pos.CENTER));
        FxAttrHelper.applyRegionSizing(sp, file);
        return sp;
    }

    @Override public String getExtension() { return "StackPane"; }
}
