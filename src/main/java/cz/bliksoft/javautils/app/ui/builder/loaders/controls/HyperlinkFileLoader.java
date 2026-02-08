package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.Hyperlink;

public class HyperlinkFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {
		Hyperlink hl = new Hyperlink();

		// text
		hl.setText(file.getAttribute("text", ""));

		// visited="true|false" (optional)
		hl.setVisited(file.getBool("visited", false));

		// underline="true|false" (optional)
		hl.setUnderline(file.getBool("underline", true));

		// focusTraversable / disable (optional)
		FxAttrHelper.applyControl(hl, file);

		// sizing/padding (Hyperlink is a Control -> Region)
		FxAttrHelper.applyCommon(hl, file);

		return hl;

	}

	@Override
	public String getExtension() {
		return ".Hyperlink";
	}
}
