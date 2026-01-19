package cz.bliksoft.javautils.fx.controls.images.svg;

import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.net.URISyntaxException;

import com.kitfox.svg.app.beans.SVGIcon;

public class SvgConverter {

	public static Image createImageFromSVG(SVGIcon icon, Float width, Float height, Float scale,
			String userSvgStylesheet) throws Exception {
		icon.setAntiAlias(true);
		icon.setInterpolation(SVGIcon.INTERP_BICUBIC);

		if ((width != null) && (height != null))
			icon.setAutosize(SVGIcon.AUTOSIZE_BESTFIT);
		else if (width != null)
			icon.setAutosize(SVGIcon.AUTOSIZE_HORIZ);
		else if (height != null)
			icon.setAutosize(SVGIcon.AUTOSIZE_VERT);
		else
			icon.setAutosize(SVGIcon.AUTOSIZE_NONE);

		if (height == null) {
			height = 0f;
		}
		if (width == null) {
			width = 0f;
		}

		if (scale != null)
			icon.setPreferredSize(new Dimension((int) (width * scale), (int) (height * scale)));
		else
			icon.setPreferredSize(new Dimension((int) Math.floor(width), (int) Math.floor(height)));

		return icon.getImage();
	}

	public static SVGIcon loadSvgIcon(File svg) {
		SVGIcon icon = new SVGIcon();
		icon.setSvgURI(svg.toURI());
		return icon;
	}

	public static SVGIcon loadSvgIcon(String svg) throws URISyntaxException {
		SVGIcon icon = new SVGIcon();
		icon.setSvgURI(SvgConverter.class.getResource(svg).toURI());
		return icon;
	}

	public static Image createImageFromSVG(File svg, Float width, Float height, Float scale,
			String userSvgStylesheet) throws Exception {
		SVGIcon icon = loadSvgIcon(svg);
		return createImageFromSVG(icon, width, height, scale, userSvgStylesheet);
	}

	public static Image createImageFromSVG(File svg, Float width, Float height) throws Exception {
		return createImageFromSVG(svg, width, height, null, null);
	}

	public static Image createImageFromSVG(File svg, Float height) throws Exception {
		return createImageFromSVG(svg, null, height, null, null);
	}

	public static Image createImageFromSVG(File svg) throws Exception {
		return createImageFromSVG(svg, null, null, null, null);
	}

	public static Image createImageFromSVGResource(String path, Float width, Float height, Float scale,
			String userSvgStylesheet) throws Exception {
		SVGIcon icon = loadSvgIcon(path);
		return createImageFromSVG(icon, width, height, scale, userSvgStylesheet);
	}

	public static Image createImageFromSVG(String path, Float width, Float height) throws Exception {
		return createImageFromSVGResource(path, width, height, null, null);
	}

	public static Image createImageFromSVG(String path, Float height) throws Exception {
		return createImageFromSVGResource(path, null, height, null, null);
	}

	public static Image createImageFromSVG(String path) throws Exception {
		return createImageFromSVGResource(path, null, null, null, null);
	}
}
