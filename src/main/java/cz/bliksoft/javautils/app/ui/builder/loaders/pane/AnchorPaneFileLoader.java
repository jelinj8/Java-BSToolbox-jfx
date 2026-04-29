package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.AnchorPane;

/**
 * UI builder loader that creates an {@link AnchorPane} from an XML descriptor.
 */
public class AnchorPaneFileLoader extends FileLoader {

	/** Creates this loader. */
	public AnchorPaneFileLoader() {
	}

	@Override
	public Object loadObject(FileObject file) {
		AnchorPane ap = new AnchorPane();
		return ap;
	}

	@Override
	public String getSupportedType() {
		return "AnchorPane";
	}
}
