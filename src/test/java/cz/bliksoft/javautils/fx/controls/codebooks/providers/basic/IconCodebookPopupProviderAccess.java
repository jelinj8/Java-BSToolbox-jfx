package cz.bliksoft.javautils.fx.controls.codebooks.providers.basic;

import java.io.File;

/** Test access to {@link IconCodebookPopupProvider}'s package-private helpers. */
public final class IconCodebookPopupProviderAccess {

	private IconCodebookPopupProviderAccess() {
	}

	public static boolean isIconFile(File f) {
		return IconCodebookPopupProvider.isIconFile(f);
	}
}
