package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Orientation;
import javafx.scene.control.ToolBar;

public class ToolBarFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        ToolBar tb = new ToolBar();
        tb.setOrientation(FxAttrHelper.orientation(file, "orientation", Orientation.HORIZONTAL));
        return tb;
    }

    @Override public String getExtension() { return "ToolBar"; }
}
