package cz.bliksoft.javautils.app;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.WeakHashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.ClasspathUtils;
import cz.bliksoft.javautils.EnvironmentUtils;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.app.events.AppClosedEvent;
import cz.bliksoft.javautils.app.events.TryCloseEvent;
import cz.bliksoft.javautils.app.exceptions.ViewableException;
import cz.bliksoft.javautils.app.properties.XmlProperties;
import cz.bliksoft.javautils.app.rights.DefaultUnrestrictedSessionManager;
import cz.bliksoft.javautils.app.rights.SessionManager;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.events.EventListener;
import cz.bliksoft.javautils.fx.controls.images.AnyImageLoader;
import cz.bliksoft.javautils.fx.controls.images.ImageLoader;
import cz.bliksoft.javautils.modules.Modules;
import cz.bliksoft.javautils.xmlfilesystem.singletons.Singletons;
import javafx.application.Application;
import javafx.application.Platform;

/**
 * jádro frameworku
 * 
 * @author Jakub Jelínek
 */
public class BSApp {

	static Logger log = null;

	public static final String CORE_CONFIG_FOLDER = "core";

	public static final String PREF_LIBDIR = "libDir"; //$NON-NLS-1$
	public static final String PREF_LIBDIR_DEFAULT = "lib"; //$NON-NLS-1$
	public static final String PREF_MODULEDIR = "moduleDir"; //$NON-NLS-1$
	public static final String PREF_DISABLED_MODULES = "DisabledModules"; //$NON-NLS-1$
	public static final String PREF_ENABLED_MODULES = "EnabledModules"; //$NON-NLS-1$

	private static Application jFXApp = null;

	public static Application getApplication() {
		return jFXApp;
	}

	/**
	 * zjišťuje zapisovatelnost globálního konfiguračního souboru
	 * 
	 * @return
	 */
	public static boolean isGlobalPropertiesWritable() {
		return getGlobalProperties().isWritable();
	}

	private static XmlProperties defaultGlobalProperties = null;

	public static XmlProperties getGlobalProperties() {
		if (defaultGlobalProperties == null) {
			defaultGlobalProperties = new XmlProperties(
					new File(BSApp.getUserAppDir(), Constants.DEFAULT_PROPERTIES_FILENAME));
		}
		return defaultGlobalProperties;
	}

	public static void reloadGlobal() {
		defaultGlobalProperties = null;
		getGlobalProperties();
	}

	private static XmlProperties defaultLocalProperties = null;

	public static XmlProperties getLocalProperties() {
		if (defaultLocalProperties == null) {
			defaultLocalProperties = new XmlProperties(
					new File(BSApp.getUserHomeDir(), Constants.DEFAULT_PROPERTIES_FILENAME));
		}
		return defaultLocalProperties;
	}

	public static void reloadLocal() {
		defaultLocalProperties = null;
		getLocalProperties();
	}

	private static final WeakHashMap<Object, HashMap<String, Object>> objectAttributes = new WeakHashMap<>();

	public static Object getObjectProperty(Object obj, String propertyName) {
		HashMap<String, Object> props = objectAttributes.get(obj);
		if (props == null)
			return null;
		return props.get(propertyName);
	}

	public static void setObjectProperty(Object obj, String propertyName, Object value) {
		HashMap<String, Object> props = objectAttributes.get(obj);
		if (props == null) {
			props = new HashMap<String, Object>();
			objectAttributes.put(obj, props);
		}
		props.put(propertyName, value);
	}

	/**
	 * načte hodnotu z properties, přednostně z lokálních, fallback na globální
	 * 
	 * @param key
	 * @return
	 */
	public static Object getProperty(String key) {
		return getLocalProperties().getOrDefault(key, getGlobalProperties().get(key));
	}

	/**
	 * načte hodnotu z properties, přednostně z lokálních, fallback na globální,
	 * potom na {@code defaultValue}
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static Object getProperty(String key, String defaultValue) {
		return getLocalProperties().getOrDefault(key, getGlobalProperties().getOrDefault(key, defaultValue));
	}

	/**
	 * nastavení hodnoty v local properties
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public static Object setLocalProperty(String key, String value) {
		return getLocalProperties().put(key, value);
	}

	/**
	 * odebrání hodnoty z local properties
	 * 
	 * @param key
	 * @return
	 */
	public static Object removeLocalProperty(String key) {
		return getLocalProperties().remove(key);
	}

	/**
	 * nastavení hodnoty v global properties
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public static Object setGlobalProperty(String key, String value) {
		return getGlobalProperties().put(key, value);
	}

	/**
	 * odebrání hodnoty z global properties
	 * 
	 * @param key
	 * @return
	 */
	public static Object removeGlobalProperty(String key) {
		return getGlobalProperties().remove(key);
	}

	/**
	 * nastavení hodnoty v local properties s klíčem doplněným o název
	 * konfiguračního prostředí
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public static Object setLocalEnvironmentProperty(String key, String value) {
		return getLocalProperties().put(getEnvironmentName() + "." + key, value); //$NON-NLS-1$
	}

	/**
	 * odebrání hodnoty z local properties s klíčem doplněným o název konfiguračního
	 * prostředí
	 * 
	 * @param key
	 * @return
	 */
	public static Object removeLocalEnvironmentProperty(String key) {
		return getLocalProperties().remove(getEnvironmentName() + "." + key); //$NON-NLS-1$
	}

	/**
	 * nastavení hodnoty v globálních properties s klíčem doplněným o název
	 * konfiguračního prostředí
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public static Object setGlobalEnvironmentProperty(String key, String value) {
		return getGlobalProperties().put(getEnvironmentName() + "." + key, value); //$NON-NLS-1$
	}

	/**
	 * odebere z globálních properties hodnotu s klíčem doplněným o název
	 * konfiguračního prostředí
	 * 
	 * @param key
	 * @return
	 */
	public static Object removeGlobalEnvironmentProperty(String key) {
		return getGlobalProperties().remove(getEnvironmentName() + "." + key); //$NON-NLS-1$
	}

	/**
	 * vrátí hodnotu property s klíčem doplněným o název prostředí - přednostně z
	 * lokálního nastavení, fallback na globální, jinak null
	 * 
	 * @param key
	 * @return
	 */
	public static Object getEnvironmentProperty(String key) {
		return getLocalProperties().getOrDefault(getEnvironmentName() + "." + key, //$NON-NLS-1$
				getGlobalProperties().get(getEnvironmentName() + "." + key)); //$NON-NLS-1$
	}

	/**
	 * vrátí hodnotu property s klíčem doplněným o název prostředí - přednostně z
	 * lokálního nastavení, fallback na globální, jinak {@code defaultValue}
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static Object getEnvironmentProperty(String key, String defaultValue) {
		return getLocalProperties().getOrDefault(getEnvironmentName() + "." + key, //$NON-NLS-1$
				getGlobalProperties().getOrDefault(getEnvironmentName() + "." + key, defaultValue)); //$NON-NLS-1$
	}

	/**
	 * uloží lokální properties do souboru
	 * 
	 * @throws ViewableException
	 */
	public static void saveLocalProperties() throws ViewableException {
		getLocalProperties().save();
	}

	/**
	 * pokud je soubor s globálním nastavením zapisovatelný, uloží ho, jinak oznámí
	 * chybu
	 * 
	 * @throws ViewableException
	 */
	public static void saveGlobalProperties() throws ViewableException {
		if (getGlobalProperties().isWritable())
			getGlobalProperties().save();
		else {
			log.error("Can't write global properties to " + getGlobalProperties().getPath());
//			throw new ViewableException();
		}
	}

	/**
	 * název konfiguračního prostředí
	 */
	private static String environmentName = "default"; //$NON-NLS-1$

	/**
	 * název konfiguračního prostředí
	 * 
	 * @return
	 */
	public static String getEnvironmentName() {
		return environmentName;
	}

//	private static String appName = null;

	public static void setAppName(String name) throws Exception {
		EnvironmentUtils.setAppName(name);
		System.setProperty("appName", name);
	}

	public static String getAppName() {
		return EnvironmentUtils.getAppName();
	}

	private static File userAppdir = null;

	public static File getUserAppDir() {
		if (userAppdir == null)
			userAppdir = new File(Constants.USER_APPDIR, "." + EnvironmentUtils.getAppName());
		if (!userAppdir.exists()) {
			try {
				userAppdir.mkdirs();
			} catch (Exception e) {
				log.error("Failed to create app settings directory.", e);
			}
		}
		return userAppdir;
	}

	private static File userHomedir = null;

	public static File getUserHomeDir() {
		if (userHomedir == null)
			userHomedir = new File(Constants.USER_HOMEDIR, "." + EnvironmentUtils.getAppName());
		if (!userHomedir.exists()) {
			try {
				userHomedir.mkdirs();
			} catch (Exception e) {
				log.error("Failed to create app home directory.", e);
			}
		}
		return userHomedir;
	}

	/**
	 * inicializace aplikačního frameworku
	 * 
	 * @throws Exception
	 */
	public static void init(Application app) {
		jFXApp = app;

		log = LogManager.getLogger();

		String plugDir = BSApp.getGlobalProperties().getProperty(PREF_MODULEDIR);
		if (StringUtils.hasText(plugDir)) {
			File pluginsDir = new File(plugDir); // $NON-NLS-1$ //$NON-NLS-2$
			if (pluginsDir.isDirectory()) {
				ClasspathUtils.addDirectory(pluginsDir);
				log.log(Level.DEBUG, BSAppMessages.getString("App.pluginDirAddedToCp"), pluginsDir); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				log.log(Level.WARN, BSAppMessages.getString("App.pluginsDirNotFound"), pluginsDir); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			log.debug("No \"modules\" directory configured.");
		}

		{
			String tmp_disabledModules = (String) BSApp.getEnvironmentProperty(PREF_DISABLED_MODULES, ""); //$NON-NLS-1$ //$NON-NLS-2$
			for (String s : tmp_disabledModules.split(";")) {
				if (StringUtils.hasText(s))
					Modules.disableModule(s);
			}
		}
		{
			String tmp_enabledModules = (String) BSApp.getEnvironmentProperty(PREF_ENABLED_MODULES, "*"); //$NON-NLS-1$ //$NON-NLS-2$
			for (String s : tmp_enabledModules.split(";")) {
				if (StringUtils.hasText(s))
					Modules.enableModule(s);
			}
		}

		// getAppName();
		environmentName = getGlobalProperties().getProperty("app.configname", "default"); //$NON-NLS-1$ //$NON-NLS-2$

		log = LogManager.getLogger();

		String langCode = getGlobalProperties().getProperty(environmentName + ".lang", "--"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!"--".equals(langCode)) { //$NON-NLS-1$
			Locale l = null;
			l = Locale.forLanguageTag(langCode);
			if (l != null) {
				Locale.setDefault(l);
				BSAppMessages.reload();
				log.info(BSAppMessages.getString("App.locale_set"), l.toLanguageTag()); //$NON-NLS-1$
			}
		}

		log.log(Level.INFO, BSAppMessages.getString("App.starting") //$NON-NLS-1$
				+ "  vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv", //$NON-NLS-1$
				getAppName()); // $NON-NLS-3$

		// přidání konfigurované lib složky do classpath
		String libDir = getGlobalProperties().getProperty(PREF_LIBDIR);
		if (StringUtils.hasText(libDir)) {
			File libsDir = new File(libDir); // $NON-NLS-1$ //$NON-NLS-2$

			if (libsDir.isDirectory()) {
				ClasspathUtils.addDirectory(libsDir);
				log.log(Level.DEBUG, BSAppMessages.getString("App.libDirAddedToCp"), libsDir); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				log.log(Level.WARN, BSAppMessages.getString("App.LibDirNotFound"), libsDir); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			log.debug("No \"lib\" directory configured.");
		}

		if (sessionManager == null) {
			setSessionManager(new DefaultUnrestrictedSessionManager());
			log.debug("Session manager wasn't set, setting to default: {}", String.valueOf(sessionManager));
		} else {
			log.debug("Session manager was set to {}", String.valueOf(sessionManager));
		}

		ImageLoader.setDefault(new AnyImageLoader());

		// načtení modulů a import jejich definic
		Modules.loadModules();

		// inicializace modulů
		Modules.initModules();

		// instalace modulů
		Modules.installModules();

		log.log(Level.INFO, BSAppMessages.getString("App.InitializationCompleted") //$NON-NLS-1$
				+ " ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^"); //$NON-NLS-1$ //$NON-NLS-3$

		Context.getRoot().addEventListener(new EventListener<AppClosedEvent>(AppClosedEvent.class, "BSApp") {
			@Override
			public void fired(AppClosedEvent event) {
				Platform.exit();
				close();
			}
		});
	}

	/**
	 * vyvolá pokus o ukončení aplikace
	 * 
	 * @return úspěšnost
	 */
	public static void tryClose() {
		TryCloseEvent.fire("Requested by framework close()");
	}

	/**
	 * úklid systému po dokončení práce, dostupnost frameworku už není zaručená
	 */
	private static void close() {
		log.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"); //$NON-NLS-1$
		log.info("Starting modules cleanup before closing."); //$NON-NLS-1$
		Singletons.cleanup();
		Modules.cleanup();
	}

	private static SessionManager sessionManager = null;

	public static SessionManager getSessionManager() {
		return sessionManager;
	}

	public static void setSessionManager(SessionManager sessionManager) {
		if (BSApp.sessionManager != null) {
			throw new SecurityException(
					"Session manager already set, that can be done only once, before \"load\" is called.");
		}
		BSApp.sessionManager = sessionManager;
	}

}
