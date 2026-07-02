package cz.bliksoft.javautils.fx.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSAppJFX;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

/**
 * Registry-backed icon specification resolver.
 *
 * <p>
 * Iconspec entries live in the XML filesystem under {@code /core/iconspec/}:
 * each file carries an {@code iconspec} attribute (toolbar) and an optional
 * {@code menu-iconspec} attribute (menu variant; falls back to {@code iconspec}
 * when absent).
 *
 * <p>
 * Substitution variables ({@code ${name}}) are collected once from
 * {@code /core/iconspec}, {@code /core/ui/themes}, and
 * {@code /core/ui/themes/<current>}, in that order, then cached. Resolved
 * iconspec strings are cached by key+variant, so the filesystem is only
 * traversed once per key.
 */
public final class IconspecUtils {

	private static final Logger log = LogManager.getLogger();

	private static final String ICONSPEC_FOLDER = "iconspec"; //$NON-NLS-1$
	private static final String ATTR_ICONSPEC = "iconspec"; //$NON-NLS-1$
	private static final String ATTR_MENU = "menu-iconspec"; //$NON-NLS-1$

	/**
	 * Single resolved-spec cache. Key = {@code "<key>|<attrName>"}. The {@code |}
	 * separator cannot appear in XML filesystem path segments.
	 */
	private static final Map<String, String> cache = new HashMap<>();

	/** Substitution variable map — built once on first use, then reused. */
	private static volatile Map<String, String> vars = null;

	private IconspecUtils() {
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Returns the resolved toolbar iconspec for {@code key}, or {@code null} if the
	 * key is registered but carries no {@code iconspec} attribute. Logs ERROR and a
	 * DEBUG caller trace when the key is entirely absent.
	 */
	public static String getIconspec(String key) {
		return resolve(key, ATTR_ICONSPEC);
	}

	/**
	 * Returns the resolved menu iconspec for {@code key}. Falls back to the
	 * {@code iconspec} attribute when no {@code menu-iconspec} is defined. Same
	 * null/error semantics as {@link #getIconspec}.
	 */
	public static String getMenuIconspec(String key) {
		String cacheKey = key + "|" + ATTR_MENU; //$NON-NLS-1$
		if (cache.containsKey(cacheKey))
			return cache.get(cacheKey);

		FileObject f = resolveFile(key);
		if (f == null) {
			cache.put(cacheKey, null);
			return null;
		}

		String raw = f.getAttribute(ATTR_MENU, null);
		if (raw == null || raw.isBlank()) {
			String fallback = getIconspec(key);
			cache.put(cacheKey, fallback);
			return fallback;
		}

		String resolved = substitute(raw.trim());
		cache.put(cacheKey, resolved);
		return resolved;
	}

	/**
	 * Reads a numeric size variable from the substitution variable map (e.g.
	 * {@code "toolbar-size"} → {@code 24.0}). Returns {@code defaultVal} when the
	 * variable is absent or cannot be parsed.
	 */
	public static double getIconspecSize(String varName, double defaultVal) {
		String val = vars().get(varName);
		if (val == null || val.isBlank())
			return defaultVal;
		try {
			return Double.parseDouble(val.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private static String resolve(String key, String attrName) {
		String cacheKey = key + "|" + attrName; //$NON-NLS-1$
		if (cache.containsKey(cacheKey))
			return cache.get(cacheKey);

		FileObject f = resolveFile(key);
		if (f == null) {
			cache.put(cacheKey, null);
			return null;
		}

		String raw = f.getAttribute(attrName, null);
		if (raw == null || raw.isBlank()) {
			cache.put(cacheKey, null);
			return null;
		}

		String resolved = substitute(raw.trim());
		cache.put(cacheKey, resolved);
		return resolved;
	}

	private static FileObject resolveFile(String key) {
		FileObject folder = FileSystem.getFile(BSAppJFX.CORE_CONFIG_FOLDER, ICONSPEC_FOLDER);
		if (folder == null) {
			log.error("Iconspec folder /core/{} not found — key: {}", ICONSPEC_FOLDER, key); //$NON-NLS-1$
			log.debug("IconspecUtils — caller trace for key {}:", key, new Exception("caller trace")); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		FileObject f = folder.getFile(key);
		if (f == null) {
			log.error("No iconspec registered for key: {}", key); //$NON-NLS-1$
			log.debug("IconspecUtils — caller trace for key {}:", key, new Exception("caller trace")); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		return f;
	}

	private static String substitute(String raw) {
		String resolved = raw;
		boolean changed;
		do {
			if (!resolved.contains("${")) //$NON-NLS-1$
				break;
			changed = false;
			for (Map.Entry<String, String> e : vars().entrySet()) {
				String next = resolved.replace("${" + e.getKey() + "}", e.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
				if (next != resolved)
					changed = true;
				resolved = next;
			}
		} while (changed);
		return resolved;
	}

	/**
	 * Applies the same {@code ${token}} substitution used by {@link #getIconspec}
	 * to an arbitrary raw iconspec string. Use this to resolve tokens in iconspec
	 * strings that come from external sources (user input, persisted configuration)
	 * rather than from the iconspec registry.
	 *
	 * @param rawSpec a raw iconspec string that may contain {@code ${token}}
	 *                placeholders; {@code null} is returned unchanged
	 * @return the spec with all known variables substituted
	 */
	public static String substituteSpec(String rawSpec) {
		if (rawSpec == null)
			return null;
		return substitute(rawSpec);
	}

	/**
	 * Returns the substitution variable map built from the XmlFilesystem
	 * ({@code /core/iconspec}, {@code /core/ui/themes}, current theme folder). The
	 * map is unmodifiable and built lazily on first access.
	 */
	public static Map<String, String> getVars() {
		return vars();
	}

	/**
	 * Returns the substitution variable map, building it on first call. Variables
	 * are collected from (in order, later values overwrite earlier ones):
	 * {@code /core/iconspec}, {@code /core/ui/themes},
	 * {@code /core/ui/themes/<current-theme>}.
	 */
	private static Map<String, String> vars() {
		if (vars != null)
			return vars;
		Map<String, String> map = new LinkedHashMap<>();
		collectAttrs(FileSystem.getFile(BSAppJFX.CORE_CONFIG_FOLDER, ICONSPEC_FOLDER), map);
		FileObject themes = FileSystem.getFile(BSAppJFX.CORE_CONFIG_FOLDER, "ui", "themes"); //$NON-NLS-1$ //$NON-NLS-2$
		collectAttrs(themes, map);
		if (themes != null) {
			String themeName = Styling.getThemeMode() == Styling.ThemeMode.DARK ? "dark" : "light"; //$NON-NLS-1$ //$NON-NLS-2$
			collectAttrs(themes.getFile(themeName), map);
		}
		vars = Collections.unmodifiableMap(map);
		return vars;
	}

	private static void collectAttrs(FileObject f, Map<String, String> target) {
		if (f == null)
			return;
		var attrMap = f.getAttributes();
		if (attrMap == null)
			return;
		for (String k : attrMap.keySet()) {
			String v = f.getAttribute(k);
			if (v != null)
				target.put(k, v);
		}
	}
}
