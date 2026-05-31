package cz.bliksoft.javautils.fx.controls.images.svg;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SvgConverter {

	private static volatile String defaultStrokeColor = null;
	private static volatile String defaultFillColor = null;

	public static void setDefaultStrokeColor(String color) {
		defaultStrokeColor = normalizeColor(color);
	}

	public static void setDefaultFillColor(String color) {
		defaultFillColor = normalizeColor(color);
	}

	/**
	 * Normalises a color string: prefixes bare hex values (3–6 digits) with
	 * {@code #}.
	 */
	private static String normalizeColor(String s) {
		if (s == null || s.isBlank())
			return s;
		if (s.startsWith("0x") || s.startsWith("0X"))
			return "#" + s.substring(2);
		if (s.matches("[0-9a-fA-F]{3,6}"))
			return "#" + s;
		return s;
	}

	public static String getDefaultStrokeColor() {
		return defaultStrokeColor;
	}

	public static String getDefaultFillColor() {
		return defaultFillColor;
	}

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
		return createImageFromSVG(svg, width, height, scale, null, null);
	}

	public static Image createImageFromSVG(File svg, Float width, Float height, Float scale, String strokeColor,
			String fillColor) throws Exception {
		String svgContent;
		try (var is = new FileInputStream(svg)) {
			svgContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		// Only substitute when SVG uses currentColor — don't override explicit colors.
		// Falls back to "black" so SVG Salamander never receives the unresolvable
		// keyword.
		String effectiveStroke = strokeColor != null ? strokeColor
				: (svgContent.contains("currentColor") ? (defaultStrokeColor != null ? defaultStrokeColor : "black")
						: null);
		String effectiveFill = fillColor != null ? fillColor
				: (svgContent.contains("fill=\"currentColor\"") && effectiveStroke == null ? defaultFillColor : null);
		svgContent = applyColorSubstitutions(svgContent, effectiveStroke, effectiveFill);
		SVGIcon icon = loadSvgIconFromString(svgContent, svg.toURI().toString());
		return createImageFromSVG(icon, width, height, scale);
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
		return createImageFromSVGResource(path, width, height, scale, null, null);
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

	/**
	 * Loads an SVG from classpath, replaces {@code currentColor} in stroke/fill
	 * attributes with the given colors, then renders to an image.
	 *
	 * @param strokeColor CSS color string (no {@code #}) for stroke, or
	 *                    {@code null} to leave unchanged
	 * @param fillColor   CSS color string (no {@code #}) for fill, or {@code null}
	 *                    to leave unchanged
	 */
	public static Image createImageFromSVGResource(String path, Float width, Float height, Float scale,
			String strokeColor, String fillColor) throws Exception {
		if (strokeColor == null && fillColor == null && defaultStrokeColor == null && defaultFillColor == null)
			return createImageFromSVG(loadSvgIcon(path), width, height, scale);

		var url = SvgConverter.class.getResource(path);
		if (url == null)
			throw new IllegalArgumentException("SVG resource not found: " + path);

		String svgContent;
		try (InputStream is = url.openStream()) {
			svgContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}

		String effectiveStroke = strokeColor != null ? strokeColor
				: (svgContent.contains("currentColor") ? (defaultStrokeColor != null ? defaultStrokeColor : "black")
						: null);
		String effectiveFill = fillColor != null ? fillColor
				: (svgContent.contains("fill=\"currentColor\"") && effectiveStroke == null ? defaultFillColor : null);
		svgContent = applyColorSubstitutions(svgContent, effectiveStroke, effectiveFill);
		SVGIcon icon = loadSvgIconFromString(svgContent, path);
		return createImageFromSVG(icon, width, height, scale);
	}

	/**
	 * Matches shape elements that have no explicit {@code fill} attribute, used to
	 * inject fill directly so SVG Salamander doesn't need to inherit from root.
	 */
	private static final Pattern SHAPE_NO_FILL = Pattern
			.compile("<(path|rect|circle|ellipse|polygon|polyline|line)(?![^>]*\\bfill=)");

	/**
	 * Overrides stroke and fill colors in SVG source text.
	 * <ul>
	 * <li>{@code strokeColor}: replaces every {@code currentColor} occurrence (SVG
	 * Salamander does not support the keyword) and overrides all explicit
	 * {@code stroke="..."} attributes except {@code stroke="none"}.</li>
	 * <li>{@code fillColor}: overrides all {@code fill="..."} attributes except
	 * {@code fill="none"}, and injects {@code fill} directly onto shape elements
	 * that lack it — SVG Salamander does not reliably inherit fill from the root.
	 * </li>
	 * </ul>
	 */
	static String applyColorSubstitutions(String svg, String strokeColor, String fillColor) {
		if (strokeColor == null && fillColor == null)
			return svg;

		if (strokeColor != null) {
			svg = svg.replace("currentColor", strokeColor);
			svg = svg.replaceAll("stroke=\"(?!none\")[^\"]*\"",
					"stroke=\"" + Matcher.quoteReplacement(strokeColor) + "\"");
		}

		if (fillColor != null) {
			svg = svg.replaceAll("fill=\"(?!none\")[^\"]*\"", "fill=\"" + Matcher.quoteReplacement(fillColor) + "\"");
			svg = SHAPE_NO_FILL.matcher(svg).replaceAll("<$1 fill=\"" + Matcher.quoteReplacement(fillColor) + "\"");
		}

		return svg;
	}

	private static SVGIcon loadSvgIconFromString(String svgContent, String baseUri) throws IOException {
		SVGUniverse universe = new SVGUniverse();
		byte[] bytes = svgContent.getBytes(StandardCharsets.UTF_8);
		URI svgUri = universe.loadSVG(new ByteArrayInputStream(bytes), baseUri);
		SVGIcon icon = new SVGIcon();
		icon.setSvgUniverse(universe);
		icon.setSvgURI(svgUri);
		return icon;
	}
}
