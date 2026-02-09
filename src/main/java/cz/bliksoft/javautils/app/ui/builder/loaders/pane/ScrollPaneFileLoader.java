package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.ScrollPane;

public class ScrollPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		ScrollPane sp = new ScrollPane();

		sp.setFitToWidth(file.getBool("fitToWidth", false));
		sp.setFitToHeight(file.getBool("fitToHeight", false));
		sp.setPannable(file.getBool("pannable", false));
		sp.setHbarPolicy(parseBarPolicy(file.getAttribute("hbar", null), sp.getHbarPolicy()));
		sp.setVbarPolicy(parseBarPolicy(file.getAttribute("vbar", null), sp.getVbarPolicy()));

		return sp;
	}

	private static ScrollPane.ScrollBarPolicy parseBarPolicy(String s, ScrollPane.ScrollBarPolicy def) {
		if (s == null)
			return def;
		try {
			return ScrollPane.ScrollBarPolicy.valueOf(s.trim().toUpperCase());
		} catch (Exception e) {
			return def;
		}
	}

	@Override
	public String getExtension() {
		return "ScrollPane";
	}
}
