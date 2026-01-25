package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.Menu;

public class MenuFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        Menu m = new Menu();
        m.setText(file.getAttribute("text", file.getName()));
        m.setDisable(FxAttrHelper.bool(file, "disable", false));
        return m;
    }

    @Override public String getExtension() { return "Menu"; }
}
