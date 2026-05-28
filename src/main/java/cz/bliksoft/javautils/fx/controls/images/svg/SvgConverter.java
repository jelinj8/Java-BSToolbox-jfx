package cz.bliksoft.javautils.fx.controls.images.svg;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.app.beans.SVGIcon;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URISyntaxException;

public class SvgConverter {

	public static Image createImageFromSVG(SVGIcon icon, Float width, Float height, Float scale) throws Exception {
		icon.setAntiAlias(true);
		icon.setInterpolation(SVGIcon.INTERP_BICUBIC);

		if ((width != null) && (height != null)) {
			icon.setAutosize(SVGIcon.AUTOSIZE_BESTFIT);
		} else if (width != null) {
			icon.setAutosize(SVGIcon.AUTOSIZE_HORIZ);
			// compute height from the SVG's natural aspect ratio so the canvas is correct
			SVGDiagram diagram = icon.getSvgUniverse().getDiagram(icon.getSvgURI());
			if (diagram != null && diagram.getWidth() > 0)
				height = width * diagram.getHeight() / diagram.getWidth();
		} else if (height != null) {
			icon.setAutosize(SVGIcon.AUTOSIZE_VERT);
			// compute width from the SVG's natural aspect ratio so the canvas is correct
			SVGDiagram diagram = icon.getSvgUniverse().getDiagram(icon.getSvgURI());
			if (diagram != null && diagram.getHeight() > 0)
				width = height * diagram.getWidth() / diagram.getHeight();
		} else {
			icon.setAutosize(SVGIcon.AUTOSIZE_NONE);
		}

		if (height == null)
			height = 0f;
		if (width == null)
			width = 0f;

		int w = (scale != null) ? (int) (width * scale) : (int) Math.floor(width);
		int h = (scale != null) ? (int) (height * scale) : (int) Math.floor(height);

		// Avoid 0x0 raster targets
		if (w <= 0)
			w = 1;
		if (h <= 0)
			h = 1;

		icon.setPreferredSize(new Dimension(w, h));

		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			icon.paintIcon(null, g, 0, 0);
		} finally {
			g.dispose();
		}

		return SwingFXUtils.toFXImage(bi, null);
	}

	public static SVGIcon loadSvgIcon(File svg) {
		SVGIcon icon = new SVGIcon();
		icon.setSvgURI(svg.toURI());
		return icon;
	}

	public static SVGIcon loadSvgIcon(String svgResourcePath) throws URISyntaxException {
		var url = SvgConverter.class.getResource(svgResourcePath);
		if (url == null)
			throw new IllegalArgumentException("SVG resource not found: " + svgResourcePath);
		SVGIcon icon = new SVGIcon();
		icon.setSvgURI(url.toURI());
		return icon;
	}

	public static Image createImageFromSVG(File svg, Float width, Float height, Float scale) throws Exception {
		return createImageFromSVG(loadSvgIcon(svg), width, height, scale);
	}

	public static Image createImageFromSVG(File svg, Float width, Float height) throws Exception {
		return createImageFromSVG(svg, width, height, null);
	}

	public static Image createImageFromSVG(File svg, Float height) throws Exception {
		return createImageFromSVG(svg, null, height, null);
	}

	public static Image createImageFromSVG(File svg) throws Exception {
		return createImageFromSVG(svg, null, null, null);
	}

	public static Image createImageFromSVGResource(String path, Float width, Float height, Float scale)
			throws Exception {
		return createImageFromSVG(loadSvgIcon(path), width, height, scale);
	}

	public static Image createImageFromSVG(String path, Float width, Float height) throws Exception {
		return createImageFromSVGResource(path, width, height, null);
	}

	public static Image createImageFromSVG(String path, Float height) throws Exception {
		return createImageFromSVGResource(path, null, height, null);
	}

	public static Image createImageFromSVG(String path) throws Exception {
		return createImageFromSVGResource(path, null, null, null);
	}
}
