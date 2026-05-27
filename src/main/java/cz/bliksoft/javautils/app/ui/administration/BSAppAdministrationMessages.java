package cz.bliksoft.javautils.app.ui.administration;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class BSAppAdministrationMessages {
	private static final String BUNDLE_NAME = "cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages";

	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private BSAppAdministrationMessages() {
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

	public static void reload() {
		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	}
}
