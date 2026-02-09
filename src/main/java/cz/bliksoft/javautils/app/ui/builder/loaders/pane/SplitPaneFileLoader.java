package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

public class SplitPaneFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		SplitPane sp = new SplitPane();
		sp.setOrientation(FxAttrHelper.orientation(file, "orientation", Orientation.HORIZONTAL));

		// Optional: "dividerPositions=0.3,0.7"
		String div = file.getAttribute("dividerPositions", null);
		if (div != null && !div.isBlank()) {
			String[] parts = div.split(",");
			double[] vals = new double[parts.length];
			int n = 0;
			for (String p : parts) {
				try {
					vals[n++] = Double.parseDouble(p.trim());
				} catch (Exception ignored) {
				}
			}
			if (n > 0)
				sp.setDividerPositions(vals);
		}

		return sp;
	}

	@Override
	public String getExtension() {
		return "SplitPane";
	}
}
