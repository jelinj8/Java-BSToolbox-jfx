package cz.bliksoft.javautils.app.ui.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileObjectClassLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

public class UIActions {
	private static final Logger log = LogManager.getLogger();
	private static boolean isLoaded = false;

	public static final String ACTIONS_FOLDER_NAME = "actions";

	private UIActions() {
	}

	private static Map<String, IUIAction> registeredActions = new HashMap<>();

	public static void registerAction(String key, IUIAction action, String source) {
		IUIAction currentValue = registeredActions.get(key);
		if (currentValue != null)
			log.log(Level.INFO,
					"Re-registering an action with key {} from {}. Previous implementation: {} New implementation: {}",
					key, source, currentValue.getClass().getName(), action.getClass().getName());
		registeredActions.put(key, action);
	}

	public static IUIAction getAction(String key) {
		loadActions();
		return registeredActions.get(key);
	}

	private static void loadActions() {
		if (isLoaded)
			return;

		log.debug("Loading UI actions.");

		isLoaded = true;

		FileObject actionsFile = FileSystem.getFile(BSApp.CORE_CONFIG_FOLDER, ACTIONS_FOLDER_NAME);
		FileObjectClassLoader<IUIAction> loader = new FileObjectClassLoader<>();
		for (FileObject f : actionsFile.getChildFiles()) {
			try {
				IUIAction action = loader.loadFile(f);
				registerAction(action.getKey(), action, f.getResourceId());
			} catch (Exception e) {
				log.error("Failed to register IUIAction {} ({})", f.getName(), e.getMessage());
			}
		}

		ServiceLoader<IUIAction> svcLoader = ServiceLoader.load(IUIAction.class);
		Iterator<IUIAction> actionIterator = svcLoader.iterator();
		while (actionIterator.hasNext()) {
			try {
				IUIAction action = actionIterator.next();
				registerAction(action.getKey(), action, "classpath");
			} catch (ServiceConfigurationError e) {
				log.error("Class doesn't seem to be a valid BSApp IUIAction implementation: {}", e.getMessage());
			}
		}

	}

	public static String dumpActions() {
		loadActions();
		return registeredActions.keySet().stream().sorted().collect(Collectors.joining("\n"));
	}
}
