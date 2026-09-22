package cz.bliksoft.javautils.app.iconspec;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides access to localized UI messages for the iconspec composer from
 * {@code IconspecMessages.properties}.
 */
public class IconspecMessages {
	private static final String BUNDLE_NAME = "cz.bliksoft.javautils.app.iconspec.IconspecMessages";

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private IconspecMessages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static String getString(String key, Object... params) {
		try {
			return MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/** Reloads the resource bundle (e.g. after changing the default locale). */
	public static void reload() {
		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	}
}
