package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.layout.BorderPane;

/**
 * UI builder loader that creates a {@link BorderPane} from an XML descriptor.
 */
public class BorderPaneFileLoader extends FileLoader {

	/** Creates this loader. */
	public BorderPaneFileLoader() {
	}

	@Override
	public Object loadObject(FileObject file) {
		BorderPane bp = new BorderPane();

		return bp;
	}

	@Override
	public String getSupportedType() {
		return "BorderPane";
	}
}
