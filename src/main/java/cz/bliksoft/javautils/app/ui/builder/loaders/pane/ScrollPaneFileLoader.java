package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.ScrollPane;

public class ScrollPaneFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        ScrollPane sp = new ScrollPane();

        sp.setFitToWidth(FxAttrHelper.bool(file, "fitToWidth", false));
        sp.setFitToHeight(FxAttrHelper.bool(file, "fitToHeight", false));
        sp.setPannable(FxAttrHelper.bool(file, "pannable", false));
        sp.setHbarPolicy(parseBarPolicy(file.getAttribute("hbar", null), sp.getHbarPolicy()));
        sp.setVbarPolicy(parseBarPolicy(file.getAttribute("vbar", null), sp.getVbarPolicy()));

        FxAttrHelper.applyRegionSizing(sp, file);
        return sp;
    }

    private static ScrollPane.ScrollBarPolicy parseBarPolicy(String s, ScrollPane.ScrollBarPolicy def) {
        if (s == null) return def;
        try { return ScrollPane.ScrollBarPolicy.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return def; }
    }

    @Override public String getExtension() { return "ScrollPane"; }
}
