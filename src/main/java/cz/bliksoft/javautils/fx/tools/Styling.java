package cz.bliksoft.javautils.fx.tools;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;

public final class Styling {

	private static final List<String> GLOBAL_STYLES = new ArrayList<>();
	private static boolean installed;

	private Styling() {
	}

	/** Register a stylesheet from resources (classpath). Example: "/css/app.css" */
	public static void registerCss(String resourcePath) {
		URL url = Styling.class.getResource(resourcePath);
		if (url == null)
			throw new IllegalStateException("Missing CSS " + resourcePath);
		GLOBAL_STYLES.add(url.toExternalForm());
	}

	/** Call once early (e.g. first thing in Application.start) */
	public static void installGlobalCss() {
		if (installed)
			return;
		installed = true;
		if (themeMode == ThemeMode.SYSTEM) {
			themeMode = isSystemDark() ? ThemeMode.DARK : ThemeMode.LIGHT;
		}

		if (themeMode == ThemeMode.NONE) {
			Styling.registerCss("/css/app-ui-simple.css");
		} else {
			Styling.registerCss("/css/app-ui-themed.css");
		}
		
		// register here (or do it elsewhere and call install after)
		// (keep yours)
		registerCss("/css/codebook-field.css");
		registerCss("/css/icon-text-cell.css");
		registerCss("/css/validation.css");

		// Apply to already existing windows
		for (Window w : Window.getWindows()) {
			applyToWindow(w);
		}

		// Apply to any new windows (Stages, Dialogs, PopupControls, ContextMenus, etc.)
		Window.getWindows().addListener((ListChangeListener<Window>) c -> {
			while (c.next()) {
				if (c.wasAdded()) {
					for (Window w : c.getAddedSubList()) {
						applyToWindow(w);
					}
				}
			}
		});
	}

	public static void safeRegister(String resourcePath) {
		URL url = Styling.class.getResource(resourcePath);
		if (url != null)
			GLOBAL_STYLES.add(url.toExternalForm());
	}

	private static void applyToWindow(Window w) {
		// Scene might be null at first; when it becomes available, apply
		Scene s = w.getScene();
		if (s != null) {
			applyToScene(w.getScene());
		} else {
			w.sceneProperty().addListener((obs, old, scene) -> {
				if (scene != null) {
					applyToScene(scene);
				}
			});
		}
	}

	private static void applyToScene(Scene scene) {
		Objects.requireNonNull(scene, "scene");
		var sheets = scene.getStylesheets();
		for (String css : GLOBAL_STYLES) {
			if (!sheets.contains(css))
				sheets.add(css);
		}
		installThemePseudoClass(scene);
	}

	private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");
	private static final String KEY_THEME_INSTALL = Styling.class.getName() + ".themeInstallerInstalled";

	private static boolean isSystemDarkWindows() {
		try {
			Process process = new ProcessBuilder("reg", "query",
					"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v",
					"AppsUseLightTheme").start();

			try (java.util.Scanner s = new java.util.Scanner(process.getInputStream())) {
				while (s.hasNextLine()) {
					String line = s.nextLine();
					if (line.contains("AppsUseLightTheme")) {
						return line.toLowerCase().contains("0x0");
					}
				}
			}
		} catch (Exception ignored) {
		}

		return false; // fallback light
	}

	private static boolean isSystemDarkMac() {
		try {
			Process process = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").start();

			try (java.util.Scanner s = new java.util.Scanner(process.getInputStream())) {
				if (s.hasNext()) {
					return "Dark".equalsIgnoreCase(s.next().trim());
				}
			}
		} catch (Exception ignored) {
		}

		return false; // fallback light
	}

	private static boolean isSystemDark() {
		String os = System.getProperty("os.name").toLowerCase();

		if (os.contains("win")) {
			return isSystemDarkWindows();
		}
		if (os.contains("mac")) {
			return isSystemDarkMac();
		}

		// Linux – usually not reliable
		return false;
	}

	public enum ThemeMode {
		LIGHT, DARK, SYSTEM, NONE
	}

	private static void applyTheme(Scene scene, ThemeMode mode) {
		if (mode == null || mode == ThemeMode.NONE)
			return;
		boolean dark = switch (mode) {
		case DARK -> true;
		case LIGHT -> false;
		case SYSTEM -> isSystemDark();
		case NONE -> false;
		};

		Parent root = scene.getRoot();
		if (root != null) {
			root.pseudoClassStateChanged(DARK, dark);
//			root.applyCss();
//			root.layout();
		}

	}

	private static volatile ThemeMode themeMode = ThemeMode.NONE;

	public static void setThemeMode(ThemeMode mode) {
		themeMode = Objects.requireNonNull(mode, "mode");
	}

	private static void installThemePseudoClass(Scene scene) {
		Objects.requireNonNull(scene, "scene");

		if (themeMode == ThemeMode.NONE)
			return;

		if (Boolean.TRUE.equals(scene.getProperties().get(KEY_THEME_INSTALL))) {
			applyTheme(scene, themeMode);
			return;
		}
		scene.getProperties().put(KEY_THEME_INSTALL, Boolean.TRUE);

		ChangeListener<Parent> rootListener = (obs, oldRoot, newRoot) -> {
			if (newRoot != null) {
				applyTheme(scene, themeMode);
			}
		};
		scene.rootProperty().addListener(rootListener);

		// for potential uninstallation
		scene.getProperties().put(KEY_THEME_INSTALL + ".rootListener", rootListener);

		applyTheme(scene, themeMode);
	}

}
