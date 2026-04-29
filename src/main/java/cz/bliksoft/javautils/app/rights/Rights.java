package cz.bliksoft.javautils.app.rights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileObjectClassLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

public class Rights {
	private static final Logger log = LogManager.getLogger();

	private static volatile boolean isLoaded = false;
	private static Map<Class<? extends Right>, Right> registeredRights = null;
	private static Map<String, List<Right>> rightByCategories = null;
	private static Set<Class<? extends Right>> fullRightsSet = new HashSet<>();

	public static final String RIGHTS_FOLDER_NAME = "rights";

	private Rights() {
	}

	private static void registerRight(Right right) {
		log.info("Right [{}] {}", right.getAlias(), right.getName());
		List<Right> category = rightByCategories.get(right.getCategory());
		if (category == null) {
			category = new ArrayList<>();
			rightByCategories.put(right.getCategory(), category);
		}
		registeredRights.put(right.getClass(), right);
		fullRightsSet.add(right.getClass());
		category.add(right);
	}

	private static void loadRights() {
		if (isLoaded)
			return;

		synchronized (Rights.class) {
			if (isLoaded)
				return;

			log.debug("Loading application rights.");

			registeredRights = new HashMap<>();
			rightByCategories = new HashMap<>();

			FileObject rightsFile = FileSystem.getFile(BSApp.CORE_CONFIG_FOLDER, RIGHTS_FOLDER_NAME);
			FileObjectClassLoader<Right> loader = new FileObjectClassLoader<>();
			for (FileObject f : rightsFile.getChildFiles()) {
				try {
					Right right = loader.loadFile(f);
					registerRight(right);
				} catch (Exception e) {
					log.error("Failed to register right {} ({})", f.getName(), e.getMessage());
				}
			}

			ServiceLoader<Right> svcLoader = ServiceLoader.load(Right.class);
			Iterator<Right> rightIterator = svcLoader.iterator();
			while (rightIterator.hasNext()) {
				try {
					Right right = rightIterator.next();
					registerRight(right);
				} catch (ServiceConfigurationError e) {
					log.error("Class doesn't seem to be a valid BSApp Right implementation: {}", e.getMessage());
				}
			}

			isLoaded = true;
		}
	}

	public static Set<Class<? extends Right>> getFullSet() {
		loadRights();

		return fullRightsSet;
	}

	public static Right getRight(Class<? extends Right> right) {
		loadRights();

		if (isLoaded)
			return registeredRights.get(right);
		else {
			log.error("Rights were not yet loaded!");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends Right> getByName(String cls) {
		loadRights();

		// Iterator<Class<? extends Right>> iterator = getFullSet().iterator();
		// while (iterator.hasNext()) {
		// Class<? extends Right> r = iterator.next();
		// if (r.getName().equals(cls))
		// return r;
		// }
		// return NotAllowedRight.class;

		try {
			Class<Right> c = (Class<Right>) Class.forName(cls);
			if (fullRightsSet.contains(c))
				return c;
			else
				return NotAllowedRight.class;
		} catch (ClassNotFoundException e) {
			log.error("Failed to find class {} as a right.", cls);
			return NotAllowedRight.class;
		}
	}

	public static boolean isAllowed(Class<? extends Right> right) {
		return BSApp.getSessionManager().getUserInfo().isAllowed(right);
	}

}
