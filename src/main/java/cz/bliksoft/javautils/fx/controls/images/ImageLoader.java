package cz.bliksoft.javautils.fx.controls.images;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import javafx.scene.image.Image;

public abstract class ImageLoader {
	public abstract List<String> getSupportedExtensions();

	public abstract Image getImage(String name, String... args) throws Exception;

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
