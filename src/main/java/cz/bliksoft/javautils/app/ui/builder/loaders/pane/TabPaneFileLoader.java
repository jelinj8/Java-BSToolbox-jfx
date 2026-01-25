package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;

public class TabPaneFileLoader extends FileLoader {
    @Override public Object loadObject(FileObject file) {
        TabPane tp = new TabPane();

        String policy = file.getAttribute("closingPolicy", null);
        if (policy != null) {
            try { tp.setTabClosingPolicy(TabClosingPolicy.valueOf(policy.trim().toUpperCase())); }
            catch (Exception ignored) {}
        }

        tp.setSide(FxAttrHelper.side(file, "side", tp.getSide()));
        FxAttrHelper.applyRegionSizing(tp, file);
        return tp;
    }

    @Override public String getExtension() { return "TabPane"; }
}
