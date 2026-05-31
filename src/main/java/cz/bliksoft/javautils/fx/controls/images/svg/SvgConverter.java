package cz.bliksoft.javautils.fx.controls.images.svg;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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

	/**
	 * Loads an SVG from a file as a {@link SVGDocument}.
	 */
	public static SVGDocument loadSvgDocument(File svg) throws IOException {
		try (InputStream is = new FileInputStream(svg)) {
			SVGDocument doc = new SVGLoader().load(is, svg.toURI(), LoaderContext.createDefault());
			if (doc == null)
				throw new IOException("Failed to parse SVG: " + svg);
			return doc;
		}
	}

	/**
	 * Loads an SVG from a classpath resource as a {@link SVGDocument}.
	 */
	public static SVGDocument loadSvgDocument(String resourcePath) throws IOException {
		var url = SvgConverter.class.getResource(resourcePath);
		if (url == null)
			throw new IllegalArgumentException("SVG resource not found: " + resourcePath);
		SVGDocument doc = new SVGLoader().load(url, LoaderContext.createDefault());
		if (doc == null)
			throw new IOException("Failed to parse SVG resource: " + resourcePath);
		return doc;
	}

	/**
	 * Renders a pre-loaded {@link SVGDocument} to a JavaFX {@link Image}.
	 * <p>
	 * Sizing rules (before applying {@code scale}):
	 * <ul>
	 * <li>Both {@code width} and {@code height} supplied — canvas is
	 * {@code width × height}; the SVG fits within it via its own
	 * {@code preserveAspectRatio}.</li>
	 * <li>Only {@code width} — height is derived from the SVG's aspect ratio.</li>
	 * <li>Only {@code height} — width is derived from the SVG's aspect ratio.</li>
	 * <li>Neither — natural SVG size.</li>
	 * </ul>
	 */
	public static Image createImageFromSVG(SVGDocument doc, Float width, Float height, Float scale) {
		FloatSize nat = doc.size();
		float natW = nat.width;
		float natH = nat.height;

		float targetW;
		float targetH;
		if (width != null && height != null) {
			targetW = width;
			targetH = height;
		} else if (width != null) {
			targetW = width;
			targetH = (natW > 0) ? (width * natH / natW) : width;
		} else if (height != null) {
			targetH = height;
			targetW = (natH > 0) ? (height * natW / natH) : height;
		} else {
			targetW = natW;
			targetH = natH;
		}

		int w = (scale != null) ? (int) (targetW * scale) : (int) Math.floor(targetW);
		int h = (scale != null) ? (int) (targetH * scale) : (int) Math.floor(targetH);
		if (w <= 0)
			w = 1;
		if (h <= 0)
			h = 1;

		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			doc.render(null, g, new ViewBox(w, h));
		} finally {
			g.dispose();
		}

		return SwingFXUtils.toFXImage(bi, null);
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
		String effectiveStroke = strokeColor != null ? strokeColor
				: (svgContent.contains("currentColor") ? (defaultStrokeColor != null ? defaultStrokeColor : "black")
						: null);
		String effectiveFill = fillColor != null ? fillColor
				: (svgContent.contains("fill=\"currentColor\"") && effectiveStroke == null ? defaultFillColor : null);
		svgContent = applyColorSubstitutions(svgContent, effectiveStroke, effectiveFill);
		SVGDocument doc = loadSvgDocumentFromString(svgContent, svg.toURI());
		return createImageFromSVG(doc, width, height, scale);
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
	 * Loads an SVG from classpath, substitutes {@code currentColor} and explicit
	 * stroke/fill attributes with the given colors, then renders to an image.
	 *
	 * @param strokeColor CSS color string for stroke substitution, or {@code null}
	 *                    to leave unchanged
	 * @param fillColor   CSS color string injected as {@code fill} on all shape
	 *                    elements (except {@code fill="none"}), or {@code null} to
	 *                    leave unchanged
	 */
	public static Image createImageFromSVGResource(String path, Float width, Float height, Float scale,
			String strokeColor, String fillColor) throws Exception {
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
		SVGDocument doc = loadSvgDocumentFromString(svgContent, null);
		return createImageFromSVG(doc, width, height, scale);
	}

	/**
	 * Matches shape elements that have no explicit {@code fill} attribute, used to
	 * inject a theme fill color on elements that would otherwise inherit or
	 * default.
	 */
	private static final Pattern SHAPE_NO_FILL = Pattern
			.compile("<(path|rect|circle|ellipse|polygon|polyline|line)(?![^>]*\\bfill=)");

	/**
	 * Substitutes stroke and fill colors in SVG source text.
	 * <ul>
	 * <li>{@code strokeColor}: replaces every {@code currentColor} occurrence and
	 * overrides all explicit {@code stroke="..."} attributes except
	 * {@code stroke="none"}.</li>
	 * <li>{@code fillColor}: overrides all {@code fill="..."} attributes except
	 * {@code fill="none"}, and injects {@code fill} directly onto shape elements
	 * that lack it.</li>
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

	private static SVGDocument loadSvgDocumentFromString(String svgContent, URI baseUri) throws IOException {
		byte[] bytes = svgContent.getBytes(StandardCharsets.UTF_8);
		SVGDocument doc = new SVGLoader().load(new ByteArrayInputStream(bytes), baseUri, LoaderContext.createDefault());
		if (doc == null)
			throw new IOException("Failed to parse SVG" + (baseUri != null ? ": " + baseUri : ""));
		return doc;
	}
}
