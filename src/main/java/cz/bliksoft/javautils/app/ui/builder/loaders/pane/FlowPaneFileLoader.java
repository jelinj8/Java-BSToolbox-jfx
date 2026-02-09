package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Orientation;
import javafx.scene.layout.FlowPane;

public class FlowPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		FlowPane fp = new FlowPane();
		fp.setOrientation(FxAttrHelper.orientation(file, "orientation", Orientation.HORIZONTAL));
		fp.setHgap(file.getDouble("hgap", 0));
		fp.setVgap(file.getDouble("vgap", 0));
		if (file.getAttribute("prefWrapLength", null) != null) {
			fp.setPrefWrapLength(file.getDouble("prefWrapLength", fp.getPrefWrapLength()));
		}
		return fp;
	}

	@Override
	public String getExtension() {
		return "FlowPane";
	}
}
