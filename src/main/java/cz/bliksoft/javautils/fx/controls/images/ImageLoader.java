package cz.bliksoft.javautils.fx.controls.images;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import javafx.scene.image.Image;

/**
 * Abstract strategy for loading JavaFX {@link Image} objects. Concrete loaders
 * register themselves for specific file extensions via
 * {@link #addLoader(String, ImageLoader)}; a default loader handles everything
 * else.
 */
public abstract class ImageLoader {

	/**
	 * Returns the file extensions (lower-case, without dot) handled by this loader.
	 * Use {@code "*"} to indicate a wildcard / default loader.
	 *
	 * @return non-null list of supported extensions
	 */
	public abstract List<String> getSupportedExtensions();

	/**
	 * Loads an image from a named resource or URL.
	 *
	 * @param name resource path, classpath location, or URL
	 * @param args optional loader-specific arguments
	 *
	 * @return the loaded image, or {@code null} if not found
	 *
	 * @throws Exception if loading fails
	 */
	public abstract Image getImage(String name, String... args) throws Exception;

	/**
	 * Decodes an image from raw byte data.
	 *
	 * @param data the image bytes
	 * @param args optional loader-specific arguments
	 *
	 * @return the decoded image
	 *
	 * @throws Exception if decoding fails
	 */
	public abstract Image getImage(byte[] data, String... args) throws Exception;

	private static Map<String, ImageLoader> imageLoaders = new HashMap<>();
	private static ImageLoader defaultLoader = null;

	public static void addLoader(String extension, ImageLoader loader) {
		imageLoaders.putIfAbsent(extension, loader);
	}

	public static void setDefault(ImageLoader loader) {
		defaultLoader = loader;
	}

	public static ImageLoader getLoader(String fileName) {
		ImageLoader res = imageLoaders.get(FilenameUtils.getExtension(fileName).toLowerCase());
		if (res == null)
			res = defaultLoader;
		return res;
	}

}
