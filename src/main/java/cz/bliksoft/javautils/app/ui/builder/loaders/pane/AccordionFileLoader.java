package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.control.Accordion;

/**
 * UI builder loader that creates an {@link Accordion} from an XML descriptor.
 */
public class AccordionFileLoader extends FileLoader {

	/** Creates this loader. */
	public AccordionFileLoader() {
	}

	@Override
	public Object loadObject(FileObject file) {
		Accordion a = new Accordion();
		return a;
	}

	@Override
	public String getSupportedType() {
		return "Accordion";
	}
}
