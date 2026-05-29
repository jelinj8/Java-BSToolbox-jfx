package cz.bliksoft.javautils.app.ui.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import javafx.scene.input.KeyCombination;

public final class ShortcutFileLoader {

	private static final Logger log = LogManager.getLogger(ShortcutFileLoader.class);

	public static final String KEY_BINDINGS_FOLDER = "key-bindings";

	private ShortcutFileLoader() {
	}

	/**
	 * Parses the {@code keys} attribute from a {@link FileObject} into a
	 * {@link KeyCombination}.
	 *
	 * <p>
	 * The value is parsed with {@link KeyCombination#keyCombination(String)}, which
	 * accepts JavaFX shorthand such as {@code "Ctrl+S"}, {@code "Shortcut+N"},
	 * {@code "Ctrl+Shift+Delete"}.
	 *
	 * @return the parsed combination, or {@code null} if the attribute is absent or
	 *         cannot be parsed
	 */
	public static KeyCombination load(FileObject f) {
		String keys = f.getAttribute("keys", null);
		if (keys == null || keys.isBlank())
			return null;
		try {
			return KeyCombination.keyCombination(keys.trim());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Loads a {@link KeyCombination} from {@code /core/key-bindings/{subpath}}.
	 *
	 * @return the parsed combination, or {@code null} if the file is absent or has
	 *         no valid {@code keys} attribute
	 */
	public static KeyCombination loadFromKeyBindings(String subpath) {
		FileObject f = FileSystem.getFile(BSApp.CORE_CONFIG_FOLDER, KEY_BINDINGS_FOLDER, subpath);
		if (f == null) {
			log.debug("No key binding defined for {}/{}/{} — create the file to bind a shortcut",
					BSApp.CORE_CONFIG_FOLDER, KEY_BINDINGS_FOLDER, subpath);
			return null;
		}
		return load(f);
	}
}
