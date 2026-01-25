package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Orientation;
import javafx.scene.layout.FlowPane;

public class FlowPaneFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        FlowPane fp = new FlowPane();
        fp.setOrientation(FxAttrHelper.orientation(file, "orientation", Orientation.HORIZONTAL));
        fp.setHgap(FxAttrHelper.d(file, "hgap", 0));
        fp.setVgap(FxAttrHelper.d(file, "vgap", 0));
        if (file.getAttribute("prefWrapLength", null) != null) {
            fp.setPrefWrapLength(FxAttrHelper.d(file, "prefWrapLength", fp.getPrefWrapLength()));
        }
        FxAttrHelper.applyCommon(fp, file);
        return fp;
    }
    @Override public String getExtension() { return "FlowPane"; }
}
