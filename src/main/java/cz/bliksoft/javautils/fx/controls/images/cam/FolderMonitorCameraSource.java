package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;

/**
 * {@link ICameraSource} that returns the newest image file found in a monitored
 * folder (e.g. a folder synced from a phone via Syncthing, Google Drive, etc.).
 *
 * <p>
 * Registered declaratively via the {@code /singletons} XmlFilesystem registry
 * (see {@code cz.bliksoft.javautils.xmlfilesystem.singletons.Singletons}).
 * Example registration in the application's local configuration:
 *
 * <pre>{@code
 * <file name="singletons">
 *     <file name="phone-sync-folder" type=
"cz.bliksoft.javautils.fx.controls.images.cam.FolderMonitorCameraSource">
 *         <attribute name="name" value="Phone sync folder"/>
 *         <attribute name="path" value="C:\Users\jakub\SyncthingCamera"/>
 *         <attribute name="extensions" value="jpg,jpeg,png"/>
 *         <attribute name="recursive" value="true"/>
 *         <attribute name="subfolderPattern" value="Camera|DCIM.*"/>
 *     </file>
 * </file>
 * }</pre>
 */
public final class FolderMonitorCameraSource implements ICameraSource {

	private static final Logger log = LogManager.getLogger(FolderMonitorCameraSource.class);

	/** Prefix for {@link #getId()}, used to recognize folder-monitor camera ids. */
	public static final String ID_PREFIX = "folder:";

	/** Default image file extensions considered when scanning the folder. */
	private static final Set<String> DEFAULT_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "heic");

	/** Delay between size checks used to detect a still-being-written file. */
	private static final long STABLE_CHECK_DELAY_MS = 300;

	/** Poll interval between live-preview checks. */
	private static final int PREVIEW_POLL_MS = 1000;

	private final String id;
	private final String displayName;
	private final Path folder;
	private final Set<String> extensions;
	private final boolean recursive;
	private final Pattern subfolderPattern;

	public FolderMonitorCameraSource(FileObject fo) {
		this.id = ID_PREFIX + fo.getName();
		this.displayName = fo.getLocalizedAttribute("name", fo.getName());
		this.folder = Path.of(fo.getAttribute("path", null));

		String exts = fo.getAttribute("extensions", null);
		this.extensions = exts != null ? Arrays.stream(exts.toLowerCase().split("[,;]\\s*")).collect(Collectors.toSet())
				: DEFAULT_EXTENSIONS;

		this.recursive = fo.getBool("recursive", false);
		String pattern = fo.getAttribute("subfolderPattern", null);
		this.subfolderPattern = pattern != null ? Pattern.compile(pattern) : null;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public BufferedImage grabFrame(Dimension resolution) throws IOException {
		Path newest = findNewestStableImage();
		return newest == null ? null : ImageIO.read(newest.toFile());
	}

	@Override
	public ICameraPreviewSession openPreview(Dimension resolution) {
		return new PreviewSession();
	}

	/**
	 * Returns the newest image file in the monitored folder (and matching
	 * subfolders, if {@code recursive}), or {@code null} if none is found or the
	 * newest candidate's size is still changing (likely mid-sync).
	 */
	private Path findNewestStableImage() throws IOException {
		Path candidate = recursive ? findNewestRecursive(folder) : findNewestInFolder(folder);
		if (candidate == null)
			return null;

		long size1 = Files.size(candidate);
		try {
			Thread.sleep(STABLE_CHECK_DELAY_MS);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		long size2 = Files.size(candidate);
		if (size1 == 0 || size1 != size2)
			return null;

		return candidate;
	}

	private Path findNewestInFolder(Path dir) throws IOException {
		if (!Files.isDirectory(dir))
			return null;
		try (Stream<Path> files = Files.list(dir)) {
			return files.filter(Files::isRegularFile).filter(this::hasImageExtension)
					.max(Comparator.comparingLong(this::lastModifiedSafe)).orElse(null);
		}
	}

	private Path findNewestRecursive(Path dir) throws IOException {
		if (!Files.isDirectory(dir))
			return null;

		Path newest = findNewestInFolder(dir);

		try (Stream<Path> entries = Files.list(dir)) {
			for (Path sub : entries.filter(Files::isDirectory).filter(this::matchesSubfolderPattern)
					.collect(Collectors.toList())) {
				Path candidate = findNewestRecursive(sub);
				if (candidate != null && (newest == null || lastModifiedSafe(candidate) > lastModifiedSafe(newest)))
					newest = candidate;
			}
		}

		return newest;
	}

	private boolean matchesSubfolderPattern(Path dir) {
		return subfolderPattern == null || subfolderPattern.matcher(dir.getFileName().toString()).matches();
	}

	private boolean hasImageExtension(Path p) {
		String name = p.getFileName().toString().toLowerCase();
		int dot = name.lastIndexOf('.');
		return dot >= 0 && extensions.contains(name.substring(dot + 1));
	}

	private long lastModifiedSafe(Path p) {
		try {
			return Files.getLastModifiedTime(p).toMillis();
		} catch (IOException e) {
			return 0;
		}
	}

	private final class PreviewSession implements ICameraPreviewSession {

		private Path lastFile;

		@Override
		public BufferedImage readFrame() {
			BufferedImage frame = null;
			try {
				Path newest = findNewestStableImage();
				if (newest != null && !newest.equals(lastFile)) {
					frame = ImageIO.read(newest.toFile());
					lastFile = newest;
				}
			} catch (Exception ex) {
				log.debug("Folder monitor preview frame failed for {}", folder, ex);
			}
			try {
				Thread.sleep(PREVIEW_POLL_MS);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
			return frame;
		}

		@Override
		public void close() {
		}
	}
}
