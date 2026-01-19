package cz.bliksoft.javautils.fx.controls.images;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import cz.bliksoft.javautils.images.ImageLoader;

public class AnyImageLoader extends ImageLoader {

	@Override
	public List<String> getSupportedExtensions() {
		List<String> res = new ArrayList<>();
		res.add("*");
		return res;
	}

	@Override
	public Image getImage(String name, String... args) throws Exception {
		Image i = ImageUtils.getImage(name);
		if (i != null)
			return i;
		else
			return null;
	}

	@Override
	public Image getImage(byte[] data, String... args) throws Exception {
		Image orginalImage;

		try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
			orginalImage = ImageIO.read(bais);
		}

		return orginalImage;
	}

}