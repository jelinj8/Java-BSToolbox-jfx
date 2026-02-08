import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit 5 test (JDK 8 compatible) that checks unused/missing keys in
 * *Messages.properties within the current Maven module.
 *
 * - Scans src/main/resources for default bundles: *Messages.properties (no
 * locale suffix) - For each bundle, collects used keys from: - Java:
 * FooMessages.getString("KEY") or FooMessages.getString("KEY", ...) - FXML:
 * %KEY - Reports: - UNUSED keys: present in bundle but not used (excluding
 * whitelist) - MISSING keys: used in code/FXML but not present in bundle -
 * STALE whitelist entries: in whitelist but not present in bundle (cleanup
 * hint)
 *
 * Optional whitelist: - Next to the bundle file in src/main/resources: e.g.
 * src/main/resources/i18n/AppMessages.properties
 * src/main/resources/i18n/AppMessages.whitelist
 *
 * Whitelist format: one key per line, # comments, empty lines ignored.
 */
public class UnusedMessagesKeysTest {

	// Only default bundle: FooMessages.properties (not FooMessages_cs.properties
	// etc.)
	private static final Pattern DEFAULT_MESSAGES_BUNDLE = Pattern.compile("(.+Messages)\\.properties$");

	// JavaFX FXML reference: %KEY (e.g. text="%SOME_KEY")
	private static final Pattern FXML_KEY = Pattern.compile("%([A-Za-z0-9_.\\-]+)");

	// Whitelist placed next to *.properties
	private static final String WHITELIST_SUFFIX = ".whitelist";

	@Test
	public void noUnusedResourceBundleKeysInThisModule() throws Exception {
		Path moduleRoot = Paths.get("").toAbsolutePath();

		Path mainJava = moduleRoot.resolve("src/main/java");
		Path mainResources = moduleRoot.resolve("src/main/resources");

		if (!Files.isDirectory(mainResources)) {
			return; // module without resources
		}

		List<Path> bundles = findDefaultMessagesBundles(mainResources);
		if (bundles.isEmpty()) {
			return;
		}

		StringBuilder report = new StringBuilder();

		for (Path bundlePath : bundles) {
			String fileName = bundlePath.getFileName().toString();
			String className = DEFAULT_MESSAGES_BUNDLE.matcher(fileName).replaceAll("$1"); // FooMessages

			Set<String> declaredKeys = loadPropertiesKeys(bundlePath);
			Set<String> usedKeys = new TreeSet<String>();

			// Whitelist next to the properties file
			Set<String> whitelist = loadWhitelistNextToProperties(bundlePath);

			// 1) Java: FooMessages.getString("KEY"...)
			if (Files.isDirectory(mainJava)) {
				final Pattern javaPattern = getStringCallPattern(className);
				scanFiles(mainJava, Collections.singletonList(".java"), new FileConsumer() {
					@Override
					public void accept(String content) {
						extractGroup1Matches(javaPattern, content, usedKeys);
					}
				});
			}

			// 2) FXML: %KEY
			scanFiles(mainResources, Collections.singletonList(".fxml"), new FileConsumer() {
				@Override
				public void accept(String content) {
					extractGroup1Matches(FXML_KEY, content, usedKeys);
				}
			});

			// 3) Diff
			Set<String> unused = new TreeSet<String>(declaredKeys);
			unused.removeAll(usedKeys);
			unused.removeAll(whitelist);

			Set<String> missing = new TreeSet<String>(usedKeys);
			missing.removeAll(declaredKeys);

			// 4) Whitelist cleanup hint
			Set<String> staleWhitelist = new TreeSet<String>(whitelist);
			staleWhitelist.removeAll(declaredKeys);

			if (!unused.isEmpty() || !missing.isEmpty() || !staleWhitelist.isEmpty()) {
				report.append("\n=== ").append(className).append(" (").append(bundlePath).append(") ===\n");

				if (!unused.isEmpty()) {
					report.append("UNUSED keys (existují v bundle, ale nenašly se v kódu/FXML; mimo whitelist):\n");
					for (String k : unused) {
						report.append("  - ").append(k).append("\n");
					}
				}

				if (!missing.isEmpty()) {
					report.append("MISSING keys (použité v kódu/FXML, ale chybí v bundle):\n");
					for (String k : missing) {
						report.append("  + ").append(k).append("\n");
					}
				}

				if (!staleWhitelist.isEmpty()) {
					report.append("STALE whitelist entries (ve whitelistu, ale už nejsou v bundle – můžeš uklidit):\n");
					for (String k : staleWhitelist) {
						report.append("  * ").append(k).append("\n");
					}
				}
			}
		}

		if (report.length() > 0) {
			fail(report.toString());
		}
	}

	/**
	 * Regex for: FooMessages.getString("KEY") and FooMessages.getString("KEY", ...)
	 */
	private static Pattern getStringCallPattern(String className) {
		// \bFooMessages\s*\.\s*getString\s*\(\s*"([^"]+)"
		return Pattern.compile("\\b" + Pattern.quote(className) + "\\s*\\.\\s*getString\\s*\\(\\s*\"([^\"]+)\"");
	}

	private static List<Path> findDefaultMessagesBundles(Path resourcesRoot) throws IOException {
		List<Path> result = new ArrayList<Path>();
		try (Stream<Path> s = Files.walk(resourcesRoot)) {
			Iterator<Path> it = s.iterator();
			while (it.hasNext()) {
				Path p = it.next();
				if (!Files.isRegularFile(p))
					continue;

				String name = p.getFileName().toString();
				if (!name.endsWith("Messages.properties"))
					continue;

				if (DEFAULT_MESSAGES_BUNDLE.matcher(name).matches()) {
					result.add(p);
				}
			}
		}
		return result;
	}

	private static Set<String> loadPropertiesKeys(Path propertiesFile) throws IOException {
		Properties props = new Properties();
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(Files.newInputStream(propertiesFile), StandardCharsets.UTF_8);
			props.load(reader);
		} finally {
			if (reader != null)
				reader.close();
		}
		return props.stringPropertyNames();
	}

	/**
	 * Loads whitelist from a file placed next to the properties file:
	 * FooMessages.properties -> FooMessages.whitelist
	 *
	 * Format: one key per line, # comments, empty lines ignored.
	 */
	private static Set<String> loadWhitelistNextToProperties(Path propertiesFile) throws IOException {
		String propsPath = propertiesFile.toString();
		String wlPathStr = propsPath.replaceFirst("\\.properties$", WHITELIST_SUFFIX);

		// If file name doesn't end with ".properties" for some reason, fall back to
		// sibling logic
		Path whitelistFile = propsPath.endsWith(".properties") ? Paths.get(wlPathStr)
				: propertiesFile.resolveSibling(propertiesFile.getFileName().toString() + WHITELIST_SUFFIX);

		if (!Files.isRegularFile(whitelistFile)) {
			return Collections.emptySet();
		}

		Set<String> keys = new HashSet<String>();
		BufferedReader br = null;
		try {
			br = Files.newBufferedReader(whitelistFile, StandardCharsets.UTF_8);
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty())
					continue;
				if (line.startsWith("#"))
					continue;
				keys.add(line);
			}
		} finally {
			if (br != null)
				br.close();
		}
		return keys;
	}

	private static void scanFiles(Path root, List<String> extensions, FileConsumer consumer) throws IOException {
		try (Stream<Path> s = Files.walk(root)) {
			Iterator<Path> it = s.iterator();
			while (it.hasNext()) {
				Path p = it.next();
				if (!Files.isRegularFile(p))
					continue;

				String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
				boolean match = false;
				for (String ext : extensions) {
					if (n.endsWith(ext)) {
						match = true;
						break;
					}
				}
				if (!match)
					continue;

				consumer.accept(readAll(p));
			}
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	private static String readAll(Path p) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			br = Files.newBufferedReader(p, StandardCharsets.UTF_8);
			char[] buf = new char[8192];
			int n;
			while ((n = br.read(buf)) >= 0) {
				sb.append(buf, 0, n);
			}
		} finally {
			if (br != null)
				br.close();
		}
		return sb.toString();
	}

	private static void extractGroup1Matches(Pattern pattern, String content, Set<String> out) {
		Matcher m = pattern.matcher(content);
		while (m.find()) {
			out.add(m.group(1));
		}
	}

	private interface FileConsumer {
		void accept(String content) throws IOException;
	}
}
