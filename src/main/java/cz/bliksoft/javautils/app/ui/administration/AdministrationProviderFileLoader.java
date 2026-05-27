package cz.bliksoft.javautils.app.ui.administration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.ui.interfaces.IAdministrationProvider;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileObjectClassLoader;

public class AdministrationProviderFileLoader extends FileObjectClassLoader<IAdministrationProvider> {

	private static final Logger log = LogManager.getLogger();

	/**
	 * Loads a provider from a FileObject whose name is the fully qualified class
	 * name. Returns null and logs an error if the class cannot be loaded or
	 * instantiated.
	 */
	public IAdministrationProvider tryLoadFile(FileObject fo) {
		try {
			IAdministrationProvider provider = loadFile(fo);
			if (provider == null) {
				log.error("Administration provider {} has no default constructor.", fo.getName());
			}
			return provider;
		} catch (Exception e) {
			log.error("Failed to load administration provider {} ({})", fo.getName(), e.getMessage());
			return null;
		}
	}
}
