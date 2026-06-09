package cz.bliksoft.javautils.app.ui.actions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import javafx.scene.input.KeyCodeCombination;
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
			// return KeyCombination.keyCombination(keys.trim());
			return withAltGrSupport(KeyCombination.keyCombination(keys.trim()));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * On Windows, the right Alt key (AltGr) is reported as Ctrl+Alt at the OS
	 * level. A combination with {@code ALT_DOWN} and {@code CTRL_UP} would
	 * therefore never match an AltGr keystroke. Replacing {@code CTRL_UP} with
	 * {@code CTRL_ANY} makes the shortcut fire for both left Alt and right AltGr
	 * without introducing a separate Ctrl+Alt binding.
	 */
	private static KeyCombination withAltGrSupport(KeyCombination kc) {
		if (!(kc instanceof KeyCodeCombination kcc))
			return kc;
		if (kcc.getAlt() != KeyCombination.ModifierValue.DOWN)
			return kc;
		if (kcc.getControl() != KeyCombination.ModifierValue.UP)
			return kc;
		return new KeyCodeCombination(kcc.getCode(), kcc.getShift(), KeyCombination.ModifierValue.ANY, kcc.getAlt(),
				kcc.getMeta(), kcc.getShortcut());
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
