package cz.bliksoft.javautils.app.ui.widgets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileObjectClassLoader;

public class WidgetFactoryFileLoader extends FileObjectClassLoader<IWidgetFactory> {

	private static final Logger log = LogManager.getLogger();

	/**
	 * Loads a widget factory from a {@link FileObject} whose name is the
	 * fully-qualified class name. Returns {@code null} and logs an error if the
	 * class cannot be loaded or instantiated.
	 */
	public IWidgetFactory tryLoadFile(FileObject fo) {
		try {
			IWidgetFactory factory = loadFile(fo);
			if (factory == null) {
				log.error("Widget factory {} has no default constructor.", fo.getName());
			}
			return factory;
		} catch (Exception e) {
			log.error("Failed to load widget factory {} ({})", fo.getName(), e.getMessage());
			return null;
		}
	}
}
