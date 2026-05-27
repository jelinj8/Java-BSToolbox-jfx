package cz.bliksoft.javautils.app.permissions;

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

public class Permissions {
	private static final Logger log = LogManager.getLogger();

	private static volatile boolean isLoaded = false;
	private static Map<Class<? extends Permission>, Permission> registeredPermissions = null;
	private static Map<String, List<Permission>> permissionsByCategory = null;
	private static Set<Class<? extends Permission>> fullPermissionSet = new HashSet<>();

	public static final String PERMISSIONS_FOLDER_NAME = "permissions";

	private Permissions() {
	}

	private static void registerPermission(Permission permission) {
		log.info("Permission [{}] {}", permission.getAlias(), permission.getName());
		List<Permission> category = permissionsByCategory.get(permission.getCategory());
		if (category == null) {
			category = new ArrayList<>();
			permissionsByCategory.put(permission.getCategory(), category);
		}
		registeredPermissions.put(permission.getClass(), permission);
		fullPermissionSet.add(permission.getClass());
		category.add(permission);
	}

	private static void loadPermissions() {
		if (isLoaded)
			return;

		synchronized (Permissions.class) {
			if (isLoaded)
				return;

			log.debug("Loading application permissions.");

			registeredPermissions = new HashMap<>();
			permissionsByCategory = new HashMap<>();

			FileObject permissionsFile = FileSystem.getFile(BSApp.CORE_CONFIG_FOLDER, PERMISSIONS_FOLDER_NAME);
			FileObjectClassLoader<Permission> loader = new FileObjectClassLoader<>();
			for (FileObject f : permissionsFile.getChildFiles()) {
				try {
					Permission permission = loader.loadFile(f);
					registerPermission(permission);
				} catch (Exception e) {
					log.error("Failed to register permission {} ({})", f.getName(), e.getMessage());
				}
			}

			ServiceLoader<Permission> svcLoader = ServiceLoader.load(Permission.class);
			Iterator<Permission> permissionIterator = svcLoader.iterator();
			while (permissionIterator.hasNext()) {
				try {
					Permission permission = permissionIterator.next();
					registerPermission(permission);
				} catch (ServiceConfigurationError e) {
					log.error("Class doesn't seem to be a valid BSApp Permission implementation: {}", e.getMessage());
				}
			}

			isLoaded = true;
		}
	}

	public static Set<Class<? extends Permission>> getFullSet() {
		loadPermissions();

		return fullPermissionSet;
	}

	public static Permission getPermission(Class<? extends Permission> permission) {
		loadPermissions();

		if (isLoaded)
			return registeredPermissions.get(permission);
		else {
			log.error("Permissions were not yet loaded!");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends Permission> getByName(String cls) {
		loadPermissions();

		try {
			Class<Permission> c = (Class<Permission>) Class.forName(cls);
			if (fullPermissionSet.contains(c))
				return c;
			else
				return NotAllowedPermission.class;
		} catch (ClassNotFoundException e) {
			log.error("Failed to find class {} as a permission.", cls);
			return NotAllowedPermission.class;
		}
	}

	public static boolean isAllowed(Class<? extends Permission> permission) {
		return BSApp.getSessionManager().getUserInfo().isAllowed(permission);
	}

}
