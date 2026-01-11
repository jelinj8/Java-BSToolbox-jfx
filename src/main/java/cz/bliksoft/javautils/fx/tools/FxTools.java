package cz.bliksoft.javautils.fx.tools;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FxTools {

	private static final List<String> GLOBAL_STYLES = new ArrayList<>();
	private static boolean installed;

	private FxTools() {
	}

	/** Register a stylesheet from resources (classpath). Example: "/css/app.css" */
	public static void registerCss(String resourcePath) {
		URL url = FxTools.class.getResource(resourcePath);
		if (url == null)
			throw new IllegalStateException("Missing CSS " + resourcePath);
		GLOBAL_STYLES.add(url.toExternalForm());
	}

	/** Call once early (e.g. first thing in Application.start) */
	public static void installGlobalCss() {
		if (installed)
			return;
		installed = true;

		// register here (or do it elsewhere and call install after)
		// (keep yours)
		safeRegister("/css/app.css");
		registerCss("/css/codebook-field.css");
		registerCss("/css/icon-text-cell.css");

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

	private static void safeRegister(String resourcePath) {
		URL url = FxTools.class.getResource(resourcePath);
		if (url != null)
			GLOBAL_STYLES.add(url.toExternalForm());
	}

	private static void applyToWindow(Window w) {
		// Scene might be null at first; when it becomes available, apply
		if (w.getScene() != null) {
			applyToScene(w.getScene());
		} else {
			w.sceneProperty().addListener((obs, old, scene) -> {
				if (scene != null)
					applyToScene(scene);
			});
		}
	}

	public static void applyToScene(Scene scene) {
		Objects.requireNonNull(scene, "scene");
		var sheets = scene.getStylesheets();
		for (String css : GLOBAL_STYLES) {
			if (!sheets.contains(css))
				sheets.add(css);
		}
	}
}
