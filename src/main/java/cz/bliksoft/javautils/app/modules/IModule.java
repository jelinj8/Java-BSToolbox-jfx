package cz.bliksoft.javautils.app.modules;

import java.io.InputStream;
import java.util.Map;

/**
 *
 */
public interface IModule {

	/**
	 * @return název modulu
	 */
	public String getModuleName();

	/**
	 * kořenové XML virtuálního filesystému
	 * 
	 * @return
	 */
	public InputStream getRootXml();

	/**
	 * inicializace modulu při načtení
	 */
	public void init();

	/**
	 * úklid před uzavřením aplikace
	 */
	public void cleanup();

	/**
	 * inicializace modulu po načtení celého systému
	 */
	public void install();

	/**
	 * mapa jazykových překladů
	 * 
	 * @return
	 */
	public Map<String, String> getTranslations();

	// public String getVersion();

	/**
	 * povolení či zakázání modulu pomocí konfigurace
	 * 
	 * @return
	 */
	public boolean isEnabled();

	/**
	 * povolení či zakázání modulu pomocí konfigurace
	 */
	public void setEnabled(boolean enabled);

	public String getVersionInfo();

	public int getModuleLoadingOrder();
}
