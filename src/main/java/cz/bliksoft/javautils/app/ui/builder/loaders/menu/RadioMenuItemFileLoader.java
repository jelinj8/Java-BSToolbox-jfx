package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.RadioMenuItem;

public class RadioMenuItemFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        RadioMenuItem mi = new RadioMenuItem();
        mi.setText(file.getAttribute("text", file.getName()));
        mi.setSelected(FxAttrHelper.bool(file, "selected", false));
        mi.setDisable(FxAttrHelper.bool(file, "disable", false));
        return mi;
    }

    @Override public String getExtension() { return "RadioMenuItem"; }
}
