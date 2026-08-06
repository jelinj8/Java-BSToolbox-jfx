package cz.bliksoft.javautils.fx.tools;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
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
		registerCss("/css/app-ui-common.css");
		registerCss("/css/codebook-field.css");
		registerCss("/css/icon-text-cell.css");
		registerCss("/css/validation.css");
		registerCss("/css/object-status.css");

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
		installUiScale(scene);
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
			// root.applyCss();
			// root.layout();
		}

	}

	private static volatile ThemeMode themeMode = ThemeMode.NONE;

	public static void setThemeMode(ThemeMode mode) {
		themeMode = Objects.requireNonNull(mode, "mode");
	}

	public static ThemeMode getThemeMode() {
		return themeMode;
	}

	private static final double BASE_FONT_SIZE_PX = 13.0;

	private static volatile double uiScale = 1.0;

	/**
	 * Sets the live UI zoom factor (1.0 = 100%) and immediately re-applies it to
	 * every currently open window. Cascades via {@code -fx-font-size} on each scene
	 * root, which the default stylesheets size in {@code em} units.
	 */
	public static void setUiScale(double scale) {
		uiScale = scale;
		for (Window w : Window.getWindows()) {
			Scene s = w.getScene();
			if (s != null) {
				applyUiScale(s);
			}
		}
	}

	public static double getUiScale() {
		return uiScale;
	}

	private static final String KEY_SCALE_INSTALL = Styling.class.getName() + ".scaleInstalled";
	private static final java.util.regex.Pattern FONT_SIZE_DECL = java.util.regex.Pattern
			.compile("-fx-font-size\\s*:\\s*[^;]+;?\\s*");

	private static void installUiScale(Scene scene) {
		Objects.requireNonNull(scene, "scene");

		if (Boolean.TRUE.equals(scene.getProperties().get(KEY_SCALE_INSTALL))) {
			applyUiScale(scene);
			return;
		}
		scene.getProperties().put(KEY_SCALE_INSTALL, Boolean.TRUE);

		ChangeListener<Parent> rootListener = (obs, oldRoot, newRoot) -> {
			if (newRoot != null) {
				applyUiScale(scene);
			}
		};
		scene.rootProperty().addListener(rootListener);
		scene.getProperties().put(KEY_SCALE_INSTALL + ".rootListener", rootListener);

		applyUiScale(scene);
	}

	private static void applyUiScale(Scene scene) {
		Parent root = scene.getRoot();
		if (root == null)
			return;

		String existing = root.getStyle();
		String withoutOurs = existing == null ? "" : FONT_SIZE_DECL.matcher(existing).replaceAll("");

		if (uiScale == 1.0) {
			root.setStyle(withoutOurs);
		} else {
			String ours = String.format(java.util.Locale.ROOT, "-fx-font-size: %.2fpx; ", BASE_FONT_SIZE_PX * uiScale);
			root.setStyle(ours + withoutOurs);
		}

		// Font-size changes affect content dimensions. Controls already shown when
		// the scale changes won't grow on their own — force a relayout and, for an
		// already-visible, non-resizable window, resize it to fit.
		//
		// Deferred to the next pulse: this runs from a Window.getWindows() listener,
		// which for freshly-shown windows (e.g. an Alert's own Stage) can fire before
		// that window's own internal show()/sizeToScene() sequence has finished, so
		// resizing synchronously here can be overwritten right after by JavaFX's own
		// logic. Platform.runLater lets that finish first.
		Platform.runLater(() -> {
			root.applyCss();
			scaleButtonBars(root);
			root.layout();
			Window window = scene.getWindow();
			// Only for non-resizable windows (e.g. a plain Alert/Dialog, which has no
			// user-set or persisted size to protect and normally auto-fits its content
			// anyway). A resizable window — the main window, or a dialog that installs
			// StageAutoSizer/StageStateBinder like CameraCaptureDialog/ImageCropDialog —
			// manages its own size; unconditionally calling sizeToScene() here would
			// snap it back to its bare content size, undoing a user resize or a just
			// restored saved size.
			if (window instanceof Stage stage && stage.isShowing() && !stage.isResizable()) {
				stage.sizeToScene();
			}
		});
	}

	/**
	 * {@link javafx.scene.control.ButtonBar}'s constructor hardcodes
	 * {@code setButtonMinWidth(75)} in plain Java — not CSS, so no stylesheet can
	 * touch it — and its skin uses that floor to explicitly
	 * {@code Region.setMinWidth(...)}/{@code setPrefWidth(...)} every button (see
	 * {@code ButtonBarSkin.layoutButtons()}/{@code resizeButtons()}). Once a
	 * button's prefWidth has been explicitly set that way, later
	 * {@code prefWidth(-1)} calls (including the skin's own "widest button"
	 * measurement) just echo that fixed number back instead of re-measuring content
	 * — so after a font-size change, dialog buttons stay pinned to the unscaled
	 * 75px floor and their now-much-wider text renders as a bare ellipsis. Fix:
	 * reset each button's prefWidth to computed/auto so it measures fresh, then
	 * call the real setter (not CSS) with a scaled minimum — that setter change
	 * fires {@code buttonMinWidthProperty}'s listener, which re-runs
	 * {@code resizeButtons()} using the now-correct measurements.
	 */
	private static final double DEFAULT_BUTTON_MIN_WIDTH_PX = 75.0;

	private static void scaleButtonBars(javafx.scene.Node n) {
		if (n instanceof javafx.scene.control.ButtonBar bar) {
			for (javafx.scene.Node btn : bar.getButtons()) {
				if (btn instanceof javafx.scene.layout.Region r) {
					r.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
				}
			}
			bar.setButtonMinWidth(DEFAULT_BUTTON_MIN_WIDTH_PX * uiScale);
		}
		if (n instanceof Parent p) {
			for (javafx.scene.Node c : p.getChildrenUnmodifiable()) {
				scaleButtonBars(c);
			}
		}
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
