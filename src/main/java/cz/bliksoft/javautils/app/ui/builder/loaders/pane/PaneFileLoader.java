package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.Pane;

public class PaneFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        Pane p = new Pane();
        FxAttrHelper.applyRegionSizing(p, file);
        return p;
    }

    @Override public String getExtension() { return "Pane"; }
}
