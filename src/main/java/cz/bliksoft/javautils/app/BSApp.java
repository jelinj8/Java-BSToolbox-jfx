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
import cz.bliksoft.javautils.xmlfilesystem.singletons.Services;
import cz.bliksoft.javautils.xmlfilesystem.singletons.Singletons;
import javafx.application.Application;
import javafx.application.Platform;

/**
 * Static hub providing access to application framework services: properties,
 * session management, the JavaFX {@link Application} instance, and lifecycle
 * methods.
 */
public class BSApp {

	static Logger log = null;

	/** Folder name for core framework configuration resources. */
	public static final String CORE_CONFIG_FOLDER = "core";

	/**
	 * Property key for the directory whose JARs are added to the classpath on
	 * startup.
	 */
	public static final String PREF_LIBDIR = "libDir"; //$NON-NLS-1$
	/** Default value for {@link #PREF_LIBDIR} when the property is absent. */
	public static final String PREF_LIBDIR_DEFAULT = "lib"; //$NON-NLS-1$
	/**
	 * Property key for the directory whose JARs are loaded as modules on startup.
	 */
	public static final String PREF_MODULEDIR = "moduleDir"; //$NON-NLS-1$
	/** Property key for a {@code ;}-separated list of module names to disable. */
	public static final String PREF_DISABLED_MODULES = "DisabledModules"; //$NON-NLS-1$
	/**
	 * Property key for a {@code ;}-separated list of module names to enable
	 * (default: {@code "*"}).
	 */
	public static final String PREF_ENABLED_MODULES = "EnabledModules"; //$NON-NLS-1$

	private static Application jFXApp = null;

	/**
	 * Returns the running JavaFX application instance.
	 *
	 * @return the application instance, or {@code null} before {@link #init} is
	 *         called
	 */
	public static Application getApplication() {
		return jFXApp;
	}

	/**
	 * Returns {@code true} if the global configuration file is writable.
	 *
	 * @return {@code true} if the global properties file can be saved
	 */
	public static boolean isGlobalPropertiesWritable() {
		return getGlobalProperties().isWritable();
	}

	private static XmlProperties defaultGlobalProperties = null;

	/**
	 * Returns the global (application-level) properties, creating the backing file
	 * lazily.
	 *
	 * @return the global {@link XmlProperties} instance; never {@code null}
	 */
	public static XmlProperties getGlobalProperties() {
		if (defaultGlobalProperties == null) {
			defaultGlobalProperties = new XmlProperties(
					new File(BSApp.getUserAppDir(), Constants.DEFAULT_PROPERTIES_FILENAME));
		}
		return defaultGlobalProperties;
	}

	/**
	 * Discards the cached global properties so they will be re-read from disk on
	 * the next access.
	 */
	public static void reloadGlobal() {
		defaultGlobalProperties = null;
		getGlobalProperties();
	}

	private static XmlProperties defaultLocalProperties = null;

	/**
	 * Returns the local (user-level) properties, creating the backing file lazily.
	 *
	 * @return the local {@link XmlProperties} instance; never {@code null}
	 */
	public static XmlProperties getLocalProperties() {
		if (defaultLocalProperties == null) {
			defaultLocalProperties = new XmlProperties(
					new File(BSApp.getUserHomeDir(), Constants.DEFAULT_PROPERTIES_FILENAME));
		}
		return defaultLocalProperties;
	}

	/**
	 * Discards the cached local properties so they will be re-read from disk on the
	 * next access.
	 */
	public static void reloadLocal() {
		defaultLocalProperties = null;
		getLocalProperties();
	}

	private static final WeakHashMap<Object, HashMap<String, Object>> objectAttributes = new WeakHashMap<>();

	/**
	 * Returns an ad-hoc property attached to an arbitrary object.
	 *
	 * @param obj          the owner object
	 * @param propertyName the property name
	 *
	 * @return the stored value, or {@code null} if not set
	 */
	public static Object getObjectProperty(Object obj, String propertyName) {
		HashMap<String, Object> props = objectAttributes.get(obj);
		if (props == null)
			return null;
		return props.get(propertyName);
	}

	/**
	 * Attaches an ad-hoc property to an arbitrary object. Stored in a weak map so
	 * the entry is automatically removed when {@code obj} becomes unreachable.
	 *
	 * @param obj          the owner object
	 * @param propertyName the property name
	 * @param value        the value to store
	 */
	public static void setObjectProperty(Object obj, String propertyName, Object value) {
		HashMap<String, Object> props = objectAttributes.get(obj);
		if (props == null) {
			props = new HashMap<String, Object>();
			objectAttributes.put(obj, props);
		}
		props.put(propertyName, value);
	}

	/**
	 * Returns a property value, preferring local properties over global ones.
	 *
	 * @param key the property key
	 *
	 * @return the value from local properties, or the global value if absent;
	 *         {@code null} if not found in either
	 */
	public static Object getProperty(String key) {
		return getLocalProperties().getOrDefault(key, getGlobalProperties().get(key));
	}

	/**
	 * Returns a property value, preferring local over global, falling back to
	 * {@code defaultValue}.
	 *
	 * @param key          the property key
	 * @param defaultValue fallback value when neither local nor global has the key
	 *
	 * @return the resolved value; never {@code null} if {@code defaultValue} is not
	 *         {@code null}
	 */
	public static Object getProperty(String key, String defaultValue) {
		return getLocalProperties().getOrDefault(key, getGlobalProperties().getOrDefault(key, defaultValue));
	}

	/**
	 * Stores a value in the local (user-level) properties.
	 *
	 * @param key   the property key
	 * @param value the value to store
	 *
	 * @return the previous value for {@code key}, or {@code null}
	 */
	public static Object setLocalProperty(String key, String value) {
		return getLocalProperties().put(key, value);
	}

	/**
	 * Removes a key from the local (user-level) properties.
	 *
	 * @param key the property key to remove
	 *
	 * @return the previous value, or {@code null}
	 */
	public static Object removeLocalProperty(String key) {
		return getLocalProperties().remove(key);
	}

	/**
	 * Stores a value in the global (application-level) properties.
	 *
	 * @param key   the property key
	 * @param value the value to store
	 *
	 * @return the previous value for {@code key}, or {@code null}
	 */
	public static Object setGlobalProperty(String key, String value) {
		return getGlobalProperties().put(key, value);
	}

	/**
	 * Removes a key from the global (application-level) properties.
	 *
	 * @param key the property key to remove
	 *
	 * @return the previous value, or {@code null}
	 */
	public static Object removeGlobalProperty(String key) {
		return getGlobalProperties().remove(key);
	}

	/**
	 * Stores a value in the local properties under the environment-qualified key
	 * ({@code environmentName.key}).
	 *
	 * @param key   the base property key (without environment prefix)
	 * @param value the value to store
	 *
	 * @return the previous value for the qualified key, or {@code null}
	 */
	public static Object setLocalEnvironmentProperty(String key, String value) {
		return getLocalProperties().put(getEnvironmentName() + "." + key, value); //$NON-NLS-1$
	}

	/**
	 * Removes the environment-qualified key ({@code environmentName.key}) from the
	 * local properties.
	 *
	 * @param key the base property key (without environment prefix)
	 *
	 * @return the previous value, or {@code null}
	 */
	public static Object removeLocalEnvironmentProperty(String key) {
		return getLocalProperties().remove(getEnvironmentName() + "." + key); //$NON-NLS-1$
	}

	/**
	 * Stores a value in the global properties under the environment-qualified key
	 * ({@code environmentName.key}).
	 *
	 * @param key   the base property key (without environment prefix)
	 * @param value the value to store
	 *
	 * @return the previous value for the qualified key, or {@code null}
	 */
	public static Object setGlobalEnvironmentProperty(String key, String value) {
		return getGlobalProperties().put(getEnvironmentName() + "." + key, value); //$NON-NLS-1$
	}

	/**
	 * Removes the environment-qualified key ({@code environmentName.key}) from the
	 * global properties.
	 *
	 * @param key the base property key (without environment prefix)
	 *
	 * @return the previous value, or {@code null}
	 */
	public static Object removeGlobalEnvironmentProperty(String key) {
		return getGlobalProperties().remove(getEnvironmentName() + "." + key); //$NON-NLS-1$
	}

	/**
	 * Returns an environment-qualified property value
	 * ({@code environmentName.key}), preferring local properties over global ones.
	 *
	 * @param key the base property key (without environment prefix)
	 *
	 * @return the resolved value, or {@code null} if not found
	 */
	public static Object getEnvironmentProperty(String key) {
		return getLocalProperties().getOrDefault(getEnvironmentName() + "." + key, //$NON-NLS-1$
				getGlobalProperties().get(getEnvironmentName() + "." + key)); //$NON-NLS-1$
	}

	/**
	 * Returns an environment-qualified property value
	 * ({@code environmentName.key}), preferring local over global, falling back to
	 * {@code defaultValue}.
	 *
	 * @param key          the base property key (without environment prefix)
	 * @param defaultValue fallback value when neither source has the key
	 *
	 * @return the resolved value; never {@code null} if {@code defaultValue} is not
	 *         {@code null}
	 */
	public static Object getEnvironmentProperty(String key, String defaultValue) {
		return getLocalProperties().getOrDefault(getEnvironmentName() + "." + key, //$NON-NLS-1$
				getGlobalProperties().getOrDefault(getEnvironmentName() + "." + key, defaultValue)); //$NON-NLS-1$
	}

	/**
	 * Saves the local properties to disk.
	 *
	 * @throws ViewableException if saving fails
	 */
	public static void saveLocalProperties() throws ViewableException {
		getLocalProperties().save();
	}

	/**
	 * Saves the global properties to disk if the file is writable; logs an error
	 * otherwise.
	 *
	 * @throws ViewableException if saving fails
	 */
	public static void saveGlobalProperties() throws ViewableException {
		if (getGlobalProperties().isWritable())
			getGlobalProperties().save();
		else {
			log.error("Can't write global properties to " + getGlobalProperties().getPath());
			// throw new ViewableException();
		}
	}

	private static String environmentName = "default"; //$NON-NLS-1$

	/**
	 * Returns the active configuration environment name (default:
	 * {@code "default"}). The value is read from the {@code app.configname} global
	 * property.
	 *
	 * @return the environment name; never {@code null}
	 */
	public static String getEnvironmentName() {
		return environmentName;
	}

	// private static String appName = null;

	/**
	 * Sets the application name used to resolve the user settings directory.
	 *
	 * @param name the application name
	 *
	 * @throws Exception if setting the name fails
	 */
	public static void setAppName(String name) throws Exception {
		EnvironmentUtils.setAppName(name);
		System.setProperty("appName", name);
	}

	/**
	 * Returns the application name.
	 *
	 * @return the application name; never {@code null}
	 */
	public static String getAppName() {
		return EnvironmentUtils.getAppName();
	}

	private static File userAppdir = null;

	/**
	 * Returns the application's settings directory ({@code APPDATA/.{appName}}),
	 * creating it if necessary.
	 *
	 * @return the settings directory; never {@code null}
	 */
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

	/**
	 * Returns the user's home directory for this application
	 * ({@code ~/.{appName}}), creating it if necessary.
	 *
	 * @return the user home directory; never {@code null}
	 */
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
	 * Initializes the application framework.
	 *
	 * <p>
	 * Loads and activates all framework modules via {@link Modules}, configures
	 * classpath extensions, sets the locale, and installs the session manager. Must
	 * be called once from the JavaFX {@link Application#start} method before any UI
	 * is shown.
	 *
	 * @param app the running JavaFX application instance
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

		try {
			// načtení modulů a import jejich definic
			Modules.loadModules();

			// inicializace modulů
			Modules.initModules();

			// instalace modulů
			Modules.installModules();
		} catch (Exception e) {
			Platform.exit();
			close();
			return;
		}

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
	 * Requests application close by firing a {@code TryCloseEvent}. Listeners may
	 * veto the close (e.g. to prompt for unsaved changes).
	 */
	public static void tryClose() {
		TryCloseEvent.fire("Requested by framework close()");
	}

	/** Cleans up framework resources after the application has finished. */
	private static void close() {
		log.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"); //$NON-NLS-1$
		log.info("Starting modules cleanup before closing."); //$NON-NLS-1$
		Singletons.cleanup();
		Services.cleanup();
		Modules.cleanup();
	}

	private static SessionManager sessionManager = null;

	/**
	 * Returns the active session manager.
	 *
	 * @return the current {@link SessionManager}; may be {@code null} before
	 *         {@link #init}
	 */
	public static SessionManager getSessionManager() {
		return sessionManager;
	}

	/**
	 * Sets the session manager. Can only be called once, before
	 * {@link #init(Application)}.
	 *
	 * @param sessionManager the session manager to install
	 *
	 * @throws SecurityException if a session manager is already set
	 */
	public static void setSessionManager(SessionManager sessionManager) {
		if (BSApp.sessionManager != null) {
			throw new SecurityException(
					"Session manager already set, that can be done only once, before \"load\" is called.");
		}
		BSApp.sessionManager = sessionManager;
	}

}
