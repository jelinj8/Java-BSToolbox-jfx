package cz.bliksoft.javautils.app.modules;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.ClasspathUtils;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.logging.LogUtils;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

public class Modules {
	private Modules() {
	}

	static Logger log;// = LogManager.getLogger(); // init in loadModules (called after log4j is
						// initialized)

	private static List<String> forceEnabledModules = new ArrayList<>();
	public static final String PREF_DISABLED_MODULES = "DisabledModules"; //$NON-NLS-1$
	public static final String PREF_ENABLED_MODULES = "EnabledModules"; //$NON-NLS-1$
	public static final String PREF_MODULEDIR = "moduleDir"; //$NON-NLS-1$
	public static final String PREF_MODULEDIR_DEFAULT = "modules"; //$NON-NLS-1$

	/**
	 * registr modulů
	 */
	protected static Map<String, IModule> modules = new HashMap<>();
	protected static List<IModule> sortedModules = null;

	public static void forceEnableModule(String className) {
		forceEnabledModules.add(className);
	}

	public static boolean isForceEnabled(String className) {
		return forceEnabledModules.contains(className);
	}

	public static void loadModules() {
		log = LogManager.getLogger();
		forceEnabledModules.add("cz.bliksoft.framework.core.Core");
		forceEnabledModules.add("cz.bliksoft.framework.ui.BSFrameworkUI");
		forceEnabledModules.add("cz.bliksoft.framework.ui.administration.Administration");

		String plugDir = BSApp.getGlobalProperties().getProperty(PREF_MODULEDIR);
		if (StringUtils.hasText(plugDir)) {
			File pluginsDir = new File(plugDir); // $NON-NLS-1$ //$NON-NLS-2$
			if (pluginsDir.isDirectory()) {
				ClasspathUtils.addDirectory(pluginsDir);
				log.log(Level.DEBUG, BSAppMessages.getString("Common.pluginDirAddedToCp"), pluginsDir); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				log.log(Level.WARN, BSAppMessages.getString("Common.pluginsDirNotFound"), pluginsDir); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			log.debug("No \"modules\" directory configured.");
		}

		ServiceLoader<IModule> loader = ServiceLoader.load(IModule.class);

		List<String> disabledModules = new ArrayList<>();
		{
			String tmp_disabledModules = (String) BSApp.getEnvironmentProperty(PREF_DISABLED_MODULES, ""); //$NON-NLS-1$ //$NON-NLS-2$
			for (String s : tmp_disabledModules.split(";")) {
				if (StringUtils.hasText(s))
					disabledModules.add(s);
			}
		}
		List<String> enabledModules = new ArrayList<>();
		{
			String tmp_enabledModules = (String) BSApp.getEnvironmentProperty(PREF_ENABLED_MODULES, "*"); //$NON-NLS-1$ //$NON-NLS-2$
			for (String s : tmp_enabledModules.split(";")) {
				if (StringUtils.hasText(s))
					enabledModules.add(s);
			}
		}
		boolean allEnabled = enabledModules.contains("*");

		sortedModules = new ArrayList<>();

		Iterator<IModule> modulesIterator = loader.iterator();
		while (modulesIterator.hasNext()) {
			try {
				IModule pd = modulesIterator.next();
				boolean enabled = allEnabled;
				if (enabled && disabledModules.contains(pd.getClass().getName())) {
					enabled = false;
				}
				if (!enabled && (enabledModules.contains(pd.getClass().getName()))
						|| (forceEnabledModules.contains(pd.getClass().getName()))) {
					enabled = true;
				}

				pd.setEnabled(enabled);

				sortedModules.add(pd);
			} catch (ServiceConfigurationError e) {
				log.error(BSAppMessages.getString("Modules.ClassNotAModule"), e.getMessage()); //$NON-NLS-1$
			}
		}
		Collections.sort(sortedModules,
				(IModule m1, IModule m2) -> Integer.compare(m1.getModuleLoadingOrder(), m2.getModuleLoadingOrder()));

		for (IModule pd : sortedModules) {
			modules.put(pd.getClass().getName(), pd);

			if (pd.isEnabled()) {
				log.log(Level.INFO, BSAppMessages.getString("Common.ModuleFound"), pd.getModuleName(), //$NON-NLS-1$
						pd.getVersionInfo()); // $NON-NLS-2$
				InputStream is = pd.getRootXml();
				if (is != null) {
					try {
						FileSystem.getDefault().importXml(is, "module:" + pd.getModuleName()); //$NON-NLS-1$
					} catch (Exception e) {
						log.error(BSAppMessages.getString("Modules.FailedToLoadRootXMLForModule"), pd.getModuleName(), //$NON-NLS-1$
								LogUtils.traceToString(e));
						throw e;
					}
				} else {
					log.warn(BSAppMessages.getString("Modules.ModuleMissingRootXML"), pd.getModuleName()); //$NON-NLS-1$
				}

			} else {
				log.log(Level.INFO, BSAppMessages.getString("Common.ModuleDisabledByCfg"), pd.getClass().getName(), //$NON-NLS-1$
						pd.getModuleName()); // $NON-NLS-2$
			}
		}

		FileSystem.loadTranslations();
	}

	public static Map<String, IModule> getModules() {
		return modules;
	}

	public static void initModules() {
		for (IModule pd : sortedModules) {
			if (pd.isEnabled()) {
				try {
					if (!ModuleBase.class.equals(pd.getClass().getMethod("init").getDeclaringClass())) { //$NON-NLS-1$
						pd.init();
						log.log(Level.INFO, BSAppMessages.getString("Common.ModuleInitialized"), pd.getModuleName()); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						log.debug(BSAppMessages.getString("Modules.ModuleNotProvidingInitMethod"), pd.getModuleName()); //$NON-NLS-1$
					}
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void installModules() {
		if (modules.isEmpty()) {
			log.info(BSAppMessages.getString("Modules.NoModulesToStart")); //$NON-NLS-1$
		} else {
			log.info(BSAppMessages.getString("Modules.ModulesStartup")); //$NON-NLS-1$
			// instalace modulů
			for (IModule md : sortedModules) {
				if (md.isEnabled()) {
					try {
						if (!ModuleBase.class.equals(md.getClass().getMethod("install").getDeclaringClass())) { //$NON-NLS-1$
							log.log(Level.INFO,
									BSAppMessages.getString("Modules.ModuleInstalationStart", md.getModuleName())); //$NON-NLS-1$
							md.install();
							log.log(Level.INFO,
									BSAppMessages.getString("Modules.ModuleInstalationCompleted", md.getModuleName())); //$NON-NLS-1$
						} else {
							log.debug(BSAppMessages.getString("Modules.ModuleNotProvidingInstallMethod"), //$NON-NLS-1$
									md.getModuleName());
						}
					} catch (NoSuchMethodException | SecurityException e) {
						e.printStackTrace();
					}
				}
			}
			log.info(BSAppMessages.getString("Modules.StartupFinished")); //$NON-NLS-1$
		}
	}

	public static void cleanup() {
		for (IModule pd : modules.values()) {
			if (pd.isEnabled())
				pd.cleanup();
		}
	}
}
