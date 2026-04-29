package cz.bliksoft.javautils.app;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides access to localized application messages from
 * {@code BSAppMessages.properties}. Returns {@code !key!} when a key is missing
 * so that UI problems are immediately visible without throwing exceptions.
 */
public class BSAppMessages {
	private static final String BUNDLE_NAME = "cz.bliksoft.javautils.app.BSAppMessages"; //$NON-NLS-1$

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private BSAppMessages() {
	}

	/**
	 * Returns the localized string for the given key, or {@code !key!} if the key
	 * is missing.
	 *
	 * @param key the message key
	 *
	 * @return the localized string; never {@code null}
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/**
	 * Returns the localized string for the given key, formatted with
	 * {@link MessageFormat}.
	 *
	 * @param key    the message key
	 * @param params format arguments substituted via {@link MessageFormat}
	 *
	 * @return the formatted string; never {@code null}
	 */
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
