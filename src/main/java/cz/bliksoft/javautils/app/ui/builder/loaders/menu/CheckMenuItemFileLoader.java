package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.CheckMenuItem;

public class CheckMenuItemFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        CheckMenuItem mi = new CheckMenuItem();
        mi.setText(file.getAttribute("text", file.getName()));
        mi.setSelected(FxAttrHelper.bool(file, "selected", false));
        mi.setDisable(FxAttrHelper.bool(file, "disable", false));
        return mi;
    }

    @Override public String getExtension() { return "CheckMenuItem"; }
}
