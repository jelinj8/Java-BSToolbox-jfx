package cz.bliksoft.javautils.fx.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.math.polynomial.PolynomialEvaluator;
import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.app.ui.UiScale;
import cz.bliksoft.javautils.fx.controls.images.ico.IcoReader;
import cz.bliksoft.javautils.fx.controls.images.ico.IcoWriter;
import cz.bliksoft.javautils.fx.controls.images.qr.QrCodeRenderer;
import cz.bliksoft.javautils.fx.controls.images.svg.SvgConverter;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.SVGPath;

/**
 * Utility for loading, caching, and composing JavaFX {@link Image} objects from
 * various sources. Images are resolved via an <em>icon spec string</em> with
 * the following supported formats:
 *
 * <ul>
 * <li>{@code name.png} / {@code name.svg} — resolved relative to the branding
 * images root (default: {@code /cz/bliksoft/branding/images/})</li>
 * <li>{@code /absolute/path.png} — absolute classpath resource</li>
 * <li>{@code [F]:/filesystem/path.png} — explicit file-system path (also
 * supported for SVG)</li>
 * <li>{@code name.svg|w|h|scale|stroke|fill} — SVG with optional parameters
 * (all may be left blank):
 * <ul>
 * <li>{@code w}, {@code h} — pixel width/height; omit either to derive from the
 * SVG's natural aspect ratio</li>
 * <li>{@code scale} — extra scale multiplier applied on top of w/h</li>
 * <li>{@code stroke} — color that replaces every {@code currentColor}
 * occurrence in the SVG source and overrides explicit {@code stroke}
 * attributes; accepts bare hex ({@code ff0000}, {@code ff000080} with alpha),
 * {@code 0xRRGGBB} / {@code 0xRRGGBBAA}, or CSS named colors and
 * {@code rgba(...)} expressions</li>
 * <li>{@code fill} — color injected as {@code fill} on all SVG shape elements
 * (except {@code fill="none"}); same color syntax as stroke</li>
 * </ul>
 * </li>
 * <li>{@code EMPTY|size} — synthetic transparent canvas ({@code size×size}
 * pixels); useful as the base layer in overlay chains</li>
 * <li>{@code EMPTY|w|h} — transparent canvas with explicit width and height
 * ({@code h} may be left blank to default to {@code w})</li>
 * <li>{@code EMPTY|w|h|color} — solid-color canvas; color uses the same syntax
 * as the SVG stroke/fill slots ({@code h} may be left blank)</li>
 * <li>{@code [P]:svgPathData|w|h|scale|style} — inline SVG path data rendered
 * as a styled {@link javafx.scene.shape.SVGPath} node; use {@link #getIconNode}
 * to retrieve it</li>
 * <li>{@code [PI]:svgPathData|w|h|scale|style} — inline SVG path rasterized to
 * an {@link Image}</li>
 * <li>{@code [PS]:svgPathData|w|h|scale|style} — inline SVG path as a
 * layoutable {@link javafx.scene.shape.Shape} node; use {@link #getIconNode} to
 * retrieve it</li>
 * <li>Postfix composition — {@code #}-separated tokens where non-{@code *}
 * tokens are file specs pushed onto a stack and {@code *}-prefixed tokens are
 * commands operating on the stack. The result is the top of the stack. Example
 * — badge overlay at BR: {@code base.svg|24#badge.svg|12#*+}. Example —
 * green/red dual badges:
 * {@code *EMPTY|24#save.svg|16|||00ff00#*ANCHOR|TL#*+#save.svg|16|||ff0000#*ANCHOR|BR#*+}.
 * Commands: {@code *+|canvasMode} / {@code *-|canvasMode} (SRC_OVER / DST_OUT
 * compose; {@code canvasMode}: {@code C}=crop result to base size (default),
 * {@code E}=extend canvas to union bounding box);
 * {@code *ANCHOR|position|offsetX|offsetY} (positions: {@code TL}, {@code TR},
 * {@code BL}, {@code BR} (default), {@code C}, {@code N});
 * {@code *EMPTY|w|h|color}; {@code *JFXSTYLE|css};
 * {@code *COLOR|stroke|fill|width} — sticky drawing colors (use {@code none}
 * for no paint); {@code *TEXT|value|size|font|style} — renders text using COLOR
 * fill/stroke (style flags: {@code B}old, {@code I}talic, {@code U}nderline,
 * {@code S}trikethrough, {@code O}utline; prefix with {@code -} to remove, bare
 * {@code -} clears all); {@code *DRAW|shape|...params...|t} — draws onto the
 * top canvas using COLOR fill/stroke ({@code t} overrides stroke width for this
 * call); shapes: {@code line|x1|y1|x2|y2}, {@code circle|cx|cy|r},
 * {@code square|x|y|side}, {@code rectangle|x|y|w|h}; {@code *PUSH},
 * {@code *SWAP}, {@code *COPY}, {@code *PASTE}, {@code *POP}, {@code *RESET};
 * {@code *GET_CACHE|key} / {@code *PUT_CACHE|key} — explicit cache: GET pushes
 * the cached image and skips to the matching PUT_CACHE (inclusive) when the key
 * is present, otherwise does nothing; PUT stores stack top under key;
 * {@code *NOCACHE} — suppresses storing the final result in the outer
 * {@link #getImageIfPossible} cache (useful when generating large numbers of
 * distinct one-time specs); {@code *FILTER|name|p1|p2|p3} — apply a pixel-level
 * filter; supported filters: {@code shadow|color|width} (diffuse shadow/glow),
 * {@code outline|color|width} (sharp silhouette expansion),
 * {@code rotate|angle} (CW rotation, keep canvas size),
 * {@code shift|angle|pixels} (translation in direction), {@code scale|w|h|mode}
 * (scale image; mode: {@code F}=fit, {@code C}=crop, {@code %}=percent),
 * {@code resize|w|h} (canvas resize using current alignment/offset — replaces
 * {@code *CROP}).</li>
 * <li>The token {@value #SCALE_PLACEHOLDER} in any spec is replaced with the
 * current UI-scale bucket string before lookup</li>
 * </ul>
 *
 * <p>
 * Loaded images are cached by their resolved spec string.
 */
public class ImageUtils {
	private static final Logger log = LogManager.getLogger();

	private static String brandingImagesRoot = "/cz/bliksoft/branding/images/"; //$NON-NLS-1$

	/**
	 * Sets the classpath root prefix used when resolving relative image names.
	 *
	 * @param path the new root prefix (must end with {@code /})
	 */
	public static void setBrandingImagesRoot(String path) {
		brandingImagesRoot = path;
	}

	private static float scale = 1f;

	/**
	 * Sets the global UI scale factor applied when loading scale-aware images.
	 *
	 * @param scale the scale factor (e.g. {@code 1.5f} for 150 % DPI)
	 */
	public static void setScale(float scale) {
		ImageUtils.scale = scale;
	}

	/**
	 * Returns the current global UI scale factor.
	 *
	 * @return the scale factor
	 */
	public static float getScale() {
		return scale;
	}

	private static final Map<String, Image> iconCache = new HashMap<>();
	private static final Map<String, String> tokenMap = new LinkedHashMap<>();
	private static final Map<String, String> jfxStyleCache = new HashMap<>();

	/**
	 * Thread-local storage for the last {@code *JFXSTYLE} value encountered during
	 * postfix evaluation.
	 */
	private static final ThreadLocal<String> jfxStyleTL = new ThreadLocal<>();

	/**
	 * Set by {@code *NOCACHE} during postfix evaluation to suppress cache storage.
	 */
	private static final ThreadLocal<Boolean> noCacheTL = new ThreadLocal<>();
	private static final ThreadLocal<PolynomialEvaluator> compositionEvaluatorTL = new ThreadLocal<>();

	/** Overlay alignment: source image is centered over the base. */
	public static final int ALIGN_CENTER = 0;
	/**
	 * Overlay alignment: source image is placed at the bottom-right corner (default
	 * for badge overlays).
	 */
	public static final int ALIGN_BOTTOM_RIGHT = 1;
	/** Overlay alignment: source image is placed at the top-left corner. */
	public static final int ALIGN_TOP_LEFT = 2;
	/** Overlay alignment: source image is placed at the bottom-left corner. */
	public static final int ALIGN_BOTTOM_LEFT = 3;
	/** Overlay alignment: source image is placed at the top-right corner. */
	public static final int ALIGN_TOP_RIGHT = 4;

	/**
	 * Alignment: forces {@code *TEXT} to push a new standalone image without
	 * compositing onto the existing stack canvas.
	 */
	public static final int ALIGN_NEW = 5;

	/**
	 * Placeholder token in icon spec strings that is replaced with the current
	 * UI-scale bucket string before image lookup.
	 */
	public static final String SCALE_PLACEHOLDER = "${scale}";

	/**
	 * Registers a custom token for substitution in icon spec strings. The token
	 * {@code ${key}} will be replaced with {@code value} in every spec passed to
	 * {@link #getImageIfPossible}. Passing {@code null} as value removes a
	 * previously registered token.
	 */
	public static void registerToken(String key, String value) {
		if (value == null)
			tokenMap.remove(key);
		else
			tokenMap.put(key, value);
	}

	/** Bulk-registers tokens; see {@link #registerToken}. */
	public static void registerTokens(Map<String, String> tokens) {
		tokenMap.putAll(tokens);
	}

	/**
	 * Returns a snapshot of all currently registered tokens as an unmodifiable map.
	 * The synthetic {@code scale} key is included first with its current resolved
	 * value from
	 * {@link cz.bliksoft.javautils.app.ui.UiScale#bucketedScaleString()}, followed
	 * by all entries in insertion order from the token registry.
	 */
	public static Map<String, String> getRegisteredTokens() {
		Map<String, String> result = new LinkedHashMap<>();
		result.put("scale", UiScale.bucketedScaleString()); //$NON-NLS-1$
		result.putAll(tokenMap);
		return java.util.Collections.unmodifiableMap(result);
	}

	/**
	 * Creates an image from a raw spec string (no cache look-up; no token
	 * substitution). If the spec contains {@code #}, it is evaluated as a postfix
	 * expression; otherwise it is treated as a single file spec.
	 *
	 * @param spec       the icon spec string; may be {@code null}
	 * @param background if {@code true}, raster images are loaded asynchronously
	 *
	 * @return the composed image, or {@code null} if the spec is {@code null} or
	 *         loading fails
	 */
	protected static Image createImage(String spec, boolean background) {
		if (spec == null) {
			log.debug("NULL image spec requested");
			return null;
		}
		if (!spec.contains("#")) //$NON-NLS-1$
			return createSingleImage(spec, background);

		Deque<Image> stack = new ArrayDeque<>();
		Map<String, Object> mode = new HashMap<>();
		String skipUntilPutCacheKey = null; // non-null = skip tokens until matching *PUT_CACHE

		try {
			for (String token : spec.split("#", -1)) { //$NON-NLS-1$
				// Skip mode: bypass all tokens until the matching *PUT_CACHE|key is seen
				if (skipUntilPutCacheKey != null) {
					if (token.startsWith("*PUT_CACHE")) { //$NON-NLS-1$
						String[] p = token.split("\\|", -1); //$NON-NLS-1$
						if (skipUntilPutCacheKey.equals(p.length > 1 ? p[1] : "")) //$NON-NLS-1$
							skipUntilPutCacheKey = null;
					}
					continue;
				}

				if (token.startsWith("*GET_CACHE")) { //$NON-NLS-1$
					String[] p = token.split("\\|", -1); //$NON-NLS-1$
					String key = p.length > 1 ? p[1] : ""; //$NON-NLS-1$
					Image cached = iconCache.get(key);
					if (cached != null) {
						stack.push(cached);
						skipUntilPutCacheKey = key; // activate skip
					}
					// else: key not cached — fall through, let subsequent tokens build the image
				} else if (token.startsWith("*")) { //$NON-NLS-1$
					executeCommand(token, stack, mode, background, spec);
				} else if (!token.isBlank()) {
					Image img = createSingleImage(token, background);
					stack.push(img != null ? img : new WritableImage(1, 1));
				}
			}
			return stack.isEmpty() ? null : stack.peek();
		} finally {
			compositionEvaluatorTL.remove();
		}
	}

	/**
	 * Loads a single non-composite image from a spec string. Handles {@code EMPTY},
	 * {@code [PI]:} inline paths, SVG, ICO, and raster images. The SVG spec format
	 * is {@code file.svg|w|h|scale|stroke|fill}
	 */
	private static Image createSingleImage(String spec, boolean background) {
		// Inline SVGPath rasterized into Image
		if (spec.startsWith(PREFIX_PATH_IMAGE)) {
			PathSpec ps = parsePathSpec(spec, PREFIX_PATH_IMAGE);
			SVGPath node = createSvgPathNode(ps);

			int outW, outH;
			if (ps.w != null && ps.h != null) {
				outW = (int) Math.ceil(ps.w);
				outH = (int) Math.ceil(ps.h);
			} else {
				Bounds b = node.getBoundsInLocal();
				outW = (int) Math.ceil(b.getWidth());
				outH = (int) Math.ceil(b.getHeight());
				if (outW <= 0)
					outW = 16;
				if (outH <= 0)
					outH = 16;
			}
			return snapshotToImage(node, outW, outH);
		}

		String[] params = spec.split("\\|", -1);
		String filePath = params[0];

		// Synthetic canvas: EMPTY|size EMPTY|w|h EMPTY|w|h|color
		if (filePath.equals("EMPTY")) { //$NON-NLS-1$
			try {
				int w = Math.max(1, (int) Math.round(evalNum(params[1])));
				int h = (params.length > 2 && StringUtils.hasLength(params[2]))
						? Math.max(1, (int) Math.round(evalNum(params[2])))
						: w;
				int argb = 0;
				if (params.length > 3 && StringUtils.hasLength(params[3])) {
					String css = resolveSpecColor(params[3]);
					javafx.scene.paint.Color c = javafx.scene.paint.Color.web(css);
					int a = (int) Math.round(c.getOpacity() * 255);
					int r = (int) Math.round(c.getRed() * 255);
					int g = (int) Math.round(c.getGreen() * 255);
					int b = (int) Math.round(c.getBlue() * 255);
					argb = (a << 24) | (r << 16) | (g << 8) | b;
				}
				WritableImage img = new WritableImage(w, h);
				if (argb != 0) {
					PixelWriter pw = img.getPixelWriter();
					int[] row = new int[w];
					java.util.Arrays.fill(row, argb);
					for (int y = 0; y < h; y++)
						pw.setPixels(0, y, w, 1, PixelFormat.getIntArgbInstance(), row, 0, w);
				}
				return img;
			} catch (Exception e) {
				log.warn("Invalid EMPTY canvas spec '{}': {}", spec, e.getMessage());
				return null;
			}
		}

		// QR code: QR|ec|moduleSize|targetSize|data
		if (filePath.equals("QR")) { //$NON-NLS-1$
			String data = params.length > 4 ? params[4] : ""; //$NON-NLS-1$
			if (!StringUtils.hasLength(data)) {
				log.warn("Invalid QR spec '{}': missing data", spec);
				return null;
			}
			try {
				String ec = params.length > 1 ? params[1] : null;
				Integer moduleSize = (params.length > 2 && StringUtils.hasLength(params[2]))
						? (int) Math.round(evalNum(params[2]))
						: null;
				Integer targetSize = (params.length > 3 && StringUtils.hasLength(params[3]))
						? (int) Math.round(evalNum(params[3]))
						: null;
				return QrCodeRenderer.render(data, ec, moduleSize, targetSize);
			} catch (LinkageError | Exception e) {
				log.warn("Failed to render QR code for spec '{}': {}", spec, e.getMessage());
				return null;
			}
		}

		// SVG: file.svg|w|h|scale|stroke|fill
		if (filePath.toLowerCase().endsWith(".svg")) { //$NON-NLS-1$
			Float w = null;
			Float h = null;
			Float svgScale = null;

			try {
				if (params.length > 1 && StringUtils.hasLength(params[1]))
					w = (float) evalNum(params[1]);
				if (params.length > 2 && StringUtils.hasLength(params[2]))
					h = (float) evalNum(params[2]);
				if (params.length > 3 && StringUtils.hasLength(params[3]))
					svgScale = (float) evalNum(params[3]);
			} catch (Exception e) {
				log.error("Failed to evaluate SVG size params for spec '{}': {}", spec, e.getMessage());
				return null;
			}

			String strokeColor = null;
			String fillColor = null;
			if (params.length > 4 && StringUtils.hasLength(params[4]))
				strokeColor = resolveSpecColor(params[4]);
			if (params.length > 5 && StringUtils.hasLength(params[5]))
				fillColor = resolveSpecColor(params[5]);

			try {
				if (filePath.startsWith(PREFIX_FILE)) {
					File f = new File(filePath.substring(4));
					if (f.exists() && f.isFile())
						return SvgConverter.createImageFromSVG(f, w, h, svgScale, strokeColor, fillColor);
					return null;
				}
				String res = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath); //$NON-NLS-1$
				return SvgConverter.createImageFromSVGResource(res, w, h, svgScale, strokeColor, fillColor);
			} catch (IllegalArgumentException e) {
				log.error("Failed to load SVG image: {} - {}", spec, e.getMessage());
				return null;
			} catch (Exception e) {
				log.error("Failed to load SVG image: {}", spec, e);
				return null;
			}
		}

		// ICO
		if (filePath.toLowerCase().endsWith(".ico")) { //$NON-NLS-1$
			Integer icoW = null;
			Integer icoH = null;
			try {
				if (params.length > 1 && StringUtils.hasLength(params[1]))
					icoW = (int) Math.round(evalNum(params[1]));
				if (params.length > 2 && StringUtils.hasLength(params[2]))
					icoH = (int) Math.round(evalNum(params[2]));
			} catch (Exception e) {
				log.error("Failed to evaluate ICO size params for spec '{}': {}", spec, e.getMessage());
				return null;
			}
			if (icoW != null && icoH == null)
				icoH = icoW;
			if (icoH != null && icoW == null)
				icoW = icoH;
			try {
				if (filePath.startsWith(PREFIX_FILE)) {
					File f = new File(filePath.substring(4));
					if (f.exists() && f.isFile())
						return IcoReader.loadFromFile(f, icoW, icoH);
					return null;
				}
				String res = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath); //$NON-NLS-1$
				return IcoReader.loadFromResource(res, icoW, icoH);
			} catch (Exception e) {
				log.error("Failed to load ICO image: {}", spec, e);
				return null;
			}
		}

		// Raster images
		try {
			if (filePath.startsWith(PREFIX_FILE)) {
				File f = new File(filePath.substring(4));
				if (f.exists() && f.isFile()) {
					try (InputStream in = new FileInputStream(f)) {
						return new Image(in);
					}
				}
				log.error("Image file not found: {} (path: {})", spec, f.getAbsolutePath());
				return null;
			}
			String res = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath); //$NON-NLS-1$
			var url = ImageUtils.class.getResource(res);
			if (url != null)
				return new Image(url.toExternalForm(), background);
			log.error("Image resource not found: {} (resolved: {})", spec, res);
		} catch (Exception e) {
			log.error("Failed to load raster image: {}", spec, e);
		}
		return null;
	}

	/** Dispatches a {@code *CMD} postfix token against the stack and mode map. */
	private static void executeCommand(String token, Deque<Image> stack, Map<String, Object> mode, boolean background,
			String spec) {
		String[] parts = token.split("\\|", -1); //$NON-NLS-1$
		String cmd = parts[0].substring(1); // strip leading '*'

		switch (cmd) {
		case "+" -> { // IconspecCommand.COMPOSE_OVER //$NON-NLS-1$
			boolean extend = parts.length > 1 && "E".equalsIgnoreCase(parts[1].trim()); //$NON-NLS-1$
			Image overlay = stack.isEmpty() ? null : stack.pop();
			Image base = stack.isEmpty() ? null : stack.pop();
			Image result = postfixCompose(base, overlay, modeInt(mode, "align", ALIGN_BOTTOM_RIGHT), //$NON-NLS-1$
					modeInt(mode, "offsetX", 0), modeInt(mode, "offsetY", 0), false, extend); //$NON-NLS-1$ //$NON-NLS-2$
			if (result != null)
				stack.push(result);
		}
		case "-" -> { // IconspecCommand.COMPOSE_OUT //$NON-NLS-1$
			boolean extend = parts.length > 1 && "E".equalsIgnoreCase(parts[1].trim()); //$NON-NLS-1$
			Image overlay = stack.isEmpty() ? null : stack.pop();
			Image base = stack.isEmpty() ? null : stack.pop();
			Image result = postfixCompose(base, overlay, modeInt(mode, "align", ALIGN_BOTTOM_RIGHT), //$NON-NLS-1$
					modeInt(mode, "offsetX", 0), modeInt(mode, "offsetY", 0), true, extend); //$NON-NLS-1$ //$NON-NLS-2$
			if (result != null)
				stack.push(result);
		}
		case "ANCHOR" -> { // IconspecCommand.ANCHOR //$NON-NLS-1$
			String pos = (parts.length > 1 && StringUtils.hasLength(parts[1])) ? parts[1].trim().toUpperCase() : "BR"; //$NON-NLS-1$
			int align = switch (pos) {
			case "TL" -> ALIGN_TOP_LEFT; //$NON-NLS-1$
			case "TR" -> ALIGN_TOP_RIGHT; //$NON-NLS-1$
			case "BL" -> ALIGN_BOTTOM_LEFT; //$NON-NLS-1$
			case "C" -> ALIGN_CENTER; //$NON-NLS-1$
			case "N" -> ALIGN_NEW; //$NON-NLS-1$
			default -> ALIGN_BOTTOM_RIGHT;
			};
			mode.put("align", align); //$NON-NLS-1$
			if (parts.length > 2 && StringUtils.hasLength(parts[2])) {
				try {
					mode.put("offsetX", Integer.parseInt(parts[2].trim())); //$NON-NLS-1$
				} catch (NumberFormatException ignored) {
				}
			}
			if (parts.length > 3 && StringUtils.hasLength(parts[3])) {
				try {
					mode.put("offsetY", Integer.parseInt(parts[3].trim())); //$NON-NLS-1$
				} catch (NumberFormatException ignored) {
				}
			}
		}
		case "EMPTY" -> { // IconspecCommand.EMPTY //$NON-NLS-1$
			StringBuilder sb = new StringBuilder("EMPTY"); //$NON-NLS-1$
			for (int i = 1; i < parts.length; i++)
				sb.append("|").append(parts[i]); //$NON-NLS-1$
			Image img = createSingleImage(sb.toString(), background);
			if (img != null)
				stack.push(img);
		}
		case "QR" -> { // IconspecCommand.QR //$NON-NLS-1$
			StringBuilder sb = new StringBuilder("QR"); //$NON-NLS-1$
			for (int i = 1; i < parts.length; i++)
				sb.append("|").append(parts[i]); //$NON-NLS-1$
			Image img = createSingleImage(sb.toString(), background);
			if (img != null)
				stack.push(img);
		}
		case "JFXSTYLE" -> jfxStyleTL.set(parts.length > 1 ? parts[1] : ""); // IconspecCommand.JFXSTYLE //$NON-NLS-1$ //$NON-NLS-2$
																				// — sets inline CSS via setStyle()
		case "RESET" -> mode.clear(); // IconspecCommand.RESET //$NON-NLS-1$
		case "PUSH" -> { // IconspecCommand.PUSH //$NON-NLS-1$
			if (!stack.isEmpty())
				stack.push(stack.peek());
		}
		case "COPY" -> { // IconspecCommand.COPY //$NON-NLS-1$
			if (!stack.isEmpty())
				mode.put("_temp", stack.peek()); //$NON-NLS-1$
		}
		case "PASTE" -> { // IconspecCommand.PASTE //$NON-NLS-1$
			Image temp = (Image) mode.get("_temp"); //$NON-NLS-1$
			if (temp != null)
				stack.push(copyImage(temp));
		}
		case "POP" -> { // IconspecCommand.POP //$NON-NLS-1$
			if (!stack.isEmpty())
				stack.pop();
		}
		case "SWAP" -> { // IconspecCommand.SWAP //$NON-NLS-1$
			if (stack.size() >= 2) {
				Image top = stack.pop();
				Image second = stack.pop();
				stack.push(top);
				stack.push(second);
			}
		}
		case "FILTER" -> { // IconspecCommand.FILTER //$NON-NLS-1$
			if (!stack.isEmpty() && parts.length > 1) {
				ImageFilter filter = ImageFilter.fromName(parts[1]);
				if (filter == null) {
					log.warn("Unknown filter name in: {}", token); //$NON-NLS-1$
				} else {
					Image top = stack.pop();
					Image result = applyFilter(filter, top, parts, 2, mode);
					if (result != null)
						stack.push(result);
				}
			}
		}
		case "*" -> { // IconspecCommand.COMPOSE_FILTER — **|filtername|p1|p2|p3 //$NON-NLS-1$
			// Equivalent to: *COPY # *FILTER|… # *- # *PASTE # *+
			if (stack.size() >= 2 && parts.length > 1) {
				ImageFilter filter = ImageFilter.fromName(parts[1]);
				if (filter == null) {
					log.warn("Unknown filter name in ** command: {}", token); //$NON-NLS-1$
				} else {
					int align = modeInt(mode, "align", ALIGN_BOTTOM_RIGHT); //$NON-NLS-1$
					int offsetX = modeInt(mode, "offsetX", 0); //$NON-NLS-1$
					int offsetY = modeInt(mode, "offsetY", 0); //$NON-NLS-1$
					Image top = stack.pop();
					Image base = stack.pop();
					Image topFiltered = applyFilter(filter, top, parts, 2, mode);
					if (topFiltered != null) {
						Image baseCutout = postfixCompose(base, topFiltered, align, offsetX, offsetY, true, false);
						Image result = postfixCompose(baseCutout, top, align, offsetX, offsetY, false, false);
						if (result != null)
							stack.push(result);
					}
				}
			}
		}
		case "TEXT" -> { // IconspecCommand.TEXT //$NON-NLS-1$
			String value = parts.length > 1 ? parts[1] : ""; //$NON-NLS-1$
			if (parts.length > 2 && StringUtils.hasLength(parts[2])) {
				try {
					mode.put("textSize", Double.parseDouble(parts[2].trim())); //$NON-NLS-1$
				} catch (NumberFormatException ignored) {
				}
			}
			if (parts.length > 3 && StringUtils.hasLength(parts[3]))
				mode.put("textFont", parts[3]); //$NON-NLS-1$
			if (parts.length > 4 && StringUtils.hasLength(parts[4]))
				mode.put("textStyle", mergeStyleFlags((String) mode.get("textStyle"), parts[4])); //$NON-NLS-1$
			if (StringUtils.hasLength(value)) {
				javafx.scene.paint.Paint fill = resolveModePaint(mode.get("fillColor")); //$NON-NLS-1$
				if (fill == null)
					fill = javafx.scene.paint.Color.BLACK;
				javafx.scene.paint.Paint stroke = resolveModePaint(mode.get("strokeColor")); //$NON-NLS-1$
				double sw = ((Number) mode.getOrDefault("strokeWidth", 1.0)).doubleValue(); //$NON-NLS-1$
				Image img = textToImage(value, fill, stroke, sw,
						((Number) mode.getOrDefault("textSize", 12.0)).doubleValue(), //$NON-NLS-1$
						(String) mode.get("textFont"), //$NON-NLS-1$
						(String) mode.get("textStyle")); //$NON-NLS-1$
				if (img != null) {
					int align = modeInt(mode, "align", ALIGN_BOTTOM_RIGHT); //$NON-NLS-1$
					if (!stack.isEmpty() && align != ALIGN_NEW) {
						Image base = stack.pop();
						int cw = (int) Math.ceil(base.getWidth());
						int ch = (int) Math.ceil(base.getHeight());
						int tw = (int) Math.ceil(img.getWidth());
						int th = (int) Math.ceil(img.getHeight());
						int ox = modeInt(mode, "offsetX", 0); //$NON-NLS-1$
						int oy = modeInt(mode, "offsetY", 0); //$NON-NLS-1$
						double dx, dy;
						switch (align) {
						case ALIGN_TOP_LEFT -> {
							dx = ox;
							dy = oy;
						}
						case ALIGN_TOP_RIGHT -> {
							dx = cw - tw + ox;
							dy = oy;
						}
						case ALIGN_BOTTOM_LEFT -> {
							dx = ox;
							dy = ch - th + oy;
						}
						case ALIGN_CENTER -> {
							dx = (cw - tw) / 2.0 + ox;
							dy = (ch - th) / 2.0 + oy;
						}
						default -> { // ALIGN_BOTTOM_RIGHT
							dx = cw - tw + ox;
							dy = ch - th + oy;
						}
						}
						javafx.scene.canvas.Canvas cv = new javafx.scene.canvas.Canvas(cw, ch);
						javafx.scene.canvas.GraphicsContext gc = cv.getGraphicsContext2D();
						gc.drawImage(base, 0, 0);
						gc.drawImage(img, dx, dy);
						img = snapshotToImage(cv, cw, ch);
					}
					stack.push(img);
				}
			}
		}
		case "COLOR" -> { // IconspecCommand.COLOR //$NON-NLS-1$
			if (parts.length > 1 && StringUtils.hasLength(parts[1]))
				mode.put("strokeColor", parts[1]); //$NON-NLS-1$
			if (parts.length > 2 && StringUtils.hasLength(parts[2]))
				mode.put("fillColor", parts[2]); //$NON-NLS-1$
			if (parts.length > 3 && StringUtils.hasLength(parts[3])) {
				try {
					mode.put("strokeWidth", Double.parseDouble(parts[3].trim())); //$NON-NLS-1$
				} catch (NumberFormatException ignored) {
				}
			}
		}
		case "DRAW" -> { // IconspecCommand.DRAW //$NON-NLS-1$
			if (stack.isEmpty() || parts.length < 2) {
				log.warn("DRAW requires a canvas on the stack and a shape name: {}", token); //$NON-NLS-1$
			} else {
				javafx.scene.paint.Paint fill = resolveModePaint(mode.get("fillColor")); //$NON-NLS-1$
				javafx.scene.paint.Paint stroke = resolveModePaint(mode.get("strokeColor")); //$NON-NLS-1$
				double sw = ((Number) mode.getOrDefault("strokeWidth", 1.0)).doubleValue(); //$NON-NLS-1$
				String shape = parts[1];
				// Fixed geometry param counts per shape; optional t follows
				int geomCount = switch (shape) {
				case "line" -> 4; // x1 y1 x2 y2
				case "circle" -> 3; // cx cy r
				case "square" -> 3; // x y side
				case "rectangle" -> 4; // x y w h
				default -> -1;
				};
				if (geomCount < 0) {
					log.warn("Unknown DRAW shape '{}': {}", shape, token); //$NON-NLS-1$
				} else {
					// t override: parts[2+geomCount] if present
					if (parts.length > 2 + geomCount && StringUtils.hasLength(parts[2 + geomCount]))
						sw = parseDouble(parts[2 + geomCount], sw);
					double[] p = new double[geomCount];
					for (int i = 0; i < geomCount; i++)
						p[i] = parseDouble(get(parts, 2 + i), 0.0);
					Image base = stack.pop();
					int cw = (int) Math.ceil(base.getWidth());
					int ch = (int) Math.ceil(base.getHeight());
					int drawAlign = modeInt(mode, "align", ALIGN_BOTTOM_RIGHT); //$NON-NLS-1$
					int ox = modeInt(mode, "offsetX", 0); //$NON-NLS-1$
					int oy = modeInt(mode, "offsetY", 0); //$NON-NLS-1$
					double refX = switch (drawAlign) {
					case ALIGN_TOP_LEFT, ALIGN_BOTTOM_LEFT -> ox;
					case ALIGN_TOP_RIGHT, ALIGN_BOTTOM_RIGHT -> cw + ox;
					case ALIGN_NEW -> 0;
					default -> cw / 2.0 + ox; // ALIGN_CENTER
					};
					double refY = switch (drawAlign) {
					case ALIGN_TOP_LEFT, ALIGN_TOP_RIGHT -> oy;
					case ALIGN_BOTTOM_LEFT, ALIGN_BOTTOM_RIGHT -> ch + oy;
					case ALIGN_NEW -> 0;
					default -> ch / 2.0 + oy; // ALIGN_CENTER
					};
					Image result = drawOnImage(base, shape, p, fill, stroke, sw, refX, refY);
					if (result != null)
						stack.push(result);
				}
			}
		}
		case "PUT_CACHE" -> { // IconspecCommand.PUT_CACHE //$NON-NLS-1$
			if (!stack.isEmpty() && parts.length > 1 && StringUtils.hasLength(parts[1]))
				iconCache.put(parts[1], copyImage(stack.peek()));
		}
		case "CLEAR_CACHE" -> { // IconspecCommand.CLEAR_CACHE //$NON-NLS-1$
			if (parts.length > 1 && StringUtils.hasLength(parts[1]))
				iconCache.remove(parts[1]);
		}
		case "SET" -> { //$NON-NLS-1$
			if (parts.length > 2 && StringUtils.hasLength(parts[1])) {
				double val = parseDouble(parts[2], 0.0);
				PolynomialEvaluator ev = compositionEvaluatorTL.get();
				if (ev == null) {
					ev = new PolynomialEvaluator();
					compositionEvaluatorTL.set(ev);
				}
				ev.registerVariable(parts[1], val);
			}
		}
		case "NOCACHE" -> noCacheTL.set(Boolean.TRUE); // IconspecCommand.NOCACHE //$NON-NLS-1$
		default -> log.warn("Unknown postfix command: {} in spec: {}", token, spec); //$NON-NLS-1$
		}
	}

	private static Image applyFilter(ImageFilter filter, Image img, String[] parts, int offset,
			Map<String, Object> mode) {
		return switch (filter) {
		case SHADOW -> filterShadow(img, parseArgbColor(get(parts, offset), 0xFF000000),
				parseInt(get(parts, offset + 1), 5), !"T".equalsIgnoreCase(get(parts, offset + 2))); //$NON-NLS-1$
		case OUTLINE -> filterOutline(img, parseArgbColor(get(parts, offset), 0xFF000000),
				parseInt(get(parts, offset + 1), 1), !"T".equalsIgnoreCase(get(parts, offset + 2))); //$NON-NLS-1$
		case ROTATE -> filterRotate(img, parseDouble(get(parts, offset), 0.0));
		case SHIFT -> filterShift(img, parseDouble(get(parts, offset), 0.0), parseDouble(get(parts, offset + 1), 0.0));
		case SCALE -> filterScale(img, get(parts, offset), get(parts, offset + 1), get(parts, offset + 2));
		case RESIZE -> filterResize(img, parseInt(get(parts, offset), (int) img.getWidth()),
				parseInt(get(parts, offset + 1), (int) img.getHeight()), modeInt(mode, "align", ALIGN_BOTTOM_RIGHT), //$NON-NLS-1$
				modeInt(mode, "offsetX", 0), modeInt(mode, "offsetY", 0)); //$NON-NLS-1$ //$NON-NLS-2$
		case MASK -> filterMask(img, parseArgbColor(get(parts, offset), 0xFFFFFFFF),
				"Y".equalsIgnoreCase(get(parts, offset + 1))); //$NON-NLS-1$
		case MONOCHROME -> filterMonochrome(img, parseArgbColor(get(parts, offset), 0xFFFFFFFF));
		case KEYMASK -> filterKeymask(img, get(parts, offset));
		case MIRROR -> mirrorImage(img, !"V".equalsIgnoreCase(get(parts, offset))); //$NON-NLS-1$
		};
	}

	private static int modeInt(Map<String, Object> mode, String key, int defaultVal) {
		Object v = mode.get(key);
		return (v instanceof Number n) ? n.intValue() : defaultVal;
	}

	private static double evalNum(String s) {
		PolynomialEvaluator ev = compositionEvaluatorTL.get();
		return ev != null ? ev.evaluate(s) : PolynomialEvaluator.eval(s);
	}

	private static int parseInt(String s, int defaultVal) {
		if (s == null || s.isBlank())
			return defaultVal;
		try {
			return (int) Math.round(evalNum(s.trim()));
		} catch (Exception e) {
			log.warn("parseInt: failed to evaluate '{}': {}", s, e.getMessage());
			return defaultVal;
		}
	}

	/**
	 * Composes {@code overlay} onto {@code base} using alignment, optional pixel
	 * offsets, and normal (SRC_OVER) or subtract (DST_OUT) mode.
	 *
	 * <p>
	 * When {@code extend} is {@code true} the result canvas is the union bounding
	 * box of both images (the overlay may grow the canvas). When {@code false} the
	 * result is clipped to the base image dimensions (default for {@code *+} /
	 * {@code *-}).
	 */
	private static Image postfixCompose(Image base, Image overlay, int align, int offsetX, int offsetY,
			boolean subtract, boolean extend) {
		if (base == null)
			return overlay;
		if (overlay == null)
			return base;

		int bW = (int) Math.ceil(base.getWidth());
		int bH = (int) Math.ceil(base.getHeight());
		int oW = (int) Math.ceil(overlay.getWidth());
		int oH = (int) Math.ceil(overlay.getHeight());

		int ox, oy;
		switch (align) {
		case ALIGN_TOP_LEFT -> {
			ox = offsetX;
			oy = offsetY;
		}
		case ALIGN_TOP_RIGHT -> {
			ox = bW - oW + offsetX;
			oy = offsetY;
		}
		case ALIGN_BOTTOM_LEFT -> {
			ox = offsetX;
			oy = bH - oH + offsetY;
		}
		case ALIGN_CENTER -> {
			ox = (bW - oW) / 2 + offsetX;
			oy = (bH - oH) / 2 + offsetY;
		}
		default -> {
			ox = bW - oW + offsetX;
			oy = bH - oH + offsetY;
		} // ALIGN_BOTTOM_RIGHT
		}

		int rW, rH, baseOffX, baseOffY, overlayOffX, overlayOffY;
		if (extend) {
			int minX = Math.min(0, ox);
			int minY = Math.min(0, oy);
			int maxX = Math.max(bW, ox + oW);
			int maxY = Math.max(bH, oy + oH);
			rW = maxX - minX;
			rH = maxY - minY;
			baseOffX = -minX;
			baseOffY = -minY;
			overlayOffX = ox - minX;
			overlayOffY = oy - minY;
		} else {
			rW = bW;
			rH = bH;
			baseOffX = 0;
			baseOffY = 0;
			overlayOffX = ox;
			overlayOffY = oy;
		}

		if (rW <= 0 || rH <= 0)
			return base;

		WritableImage out = new WritableImage(rW, rH);
		PixelWriter pw = out.getPixelWriter();
		int[] clear = new int[rW];
		for (int y = 0; y < rH; y++)
			pw.setPixels(0, y, rW, 1, PixelFormat.getIntArgbInstance(), clear, 0, rW);

		PixelReader baseReader = base.getPixelReader();
		if (baseReader != null)
			alphaCompositeInto(out, baseReader, baseOffX, baseOffY, bW, bH);

		PixelReader overlayReader = overlay.getPixelReader();
		if (overlayReader != null) {
			if (subtract)
				alphaSubtractInto(out, overlayReader, overlayOffX, overlayOffY, oW, oH);
			else
				alphaCompositeInto(out, overlayReader, overlayOffX, overlayOffY, oW, oH);
		}
		return out;
	}

	private static Image copyImage(Image src) {
		int w = (int) Math.ceil(src.getWidth());
		int h = (int) Math.ceil(src.getHeight());
		PixelReader pr = src.getPixelReader();
		if (pr == null)
			return src;
		int[] pixels = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
		WritableImage copy = new WritableImage(w, h);
		copy.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
		return copy;
	}

	private static Image mirrorImage(Image img, boolean flipLeftRight) {
		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;
		WritableImage out = new WritableImage(w, h);
		PixelWriter pw = out.getPixelWriter();
		int[] row = new int[w];
		for (int y = 0; y < h; y++) {
			int srcY = flipLeftRight ? y : h - 1 - y;
			pr.getPixels(0, srcY, w, 1, PixelFormat.getIntArgbInstance(), row, 0, w);
			if (flipLeftRight) {
				for (int x = 0, x2 = w - 1; x < x2; x++, x2--) {
					int tmp = row[x];
					row[x] = row[x2];
					row[x2] = tmp;
				}
			}
			pw.setPixels(0, y, w, 1, PixelFormat.getIntArgbInstance(), row, 0, w);
		}
		return out;
	}

	private static Image rotateImage(Image img, int degrees) {
		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null || (degrees != 90 && degrees != 180 && degrees != 270))
			return img;
		int[] src = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);
		int outW = (degrees == 180) ? w : h;
		int outH = (degrees == 180) ? h : w;
		int[] dst = new int[outW * outH];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int nx, ny;
				switch (degrees) {
				case 90 -> {
					nx = h - 1 - y;
					ny = x;
				}
				case 180 -> {
					nx = w - 1 - x;
					ny = h - 1 - y;
				}
				default -> {
					nx = y;
					ny = w - 1 - x;
				} // 270
				}
				dst[ny * outW + nx] = src[y * w + x];
			}
		}
		WritableImage out = new WritableImage(outW, outH);
		out.getPixelWriter().setPixels(0, 0, outW, outH, PixelFormat.getIntArgbInstance(), dst, 0, outW);
		return out;
	}

	private static String mergeStyleFlags(String current, String update) {
		if ("-".equals(update))
			return "";
		java.util.Set<Character> flags = new java.util.LinkedHashSet<>();
		if (current != null)
			for (char c : current.toCharArray())
				flags.add(c);
		boolean remove = false;
		for (char c : update.toCharArray()) {
			if (c == '-')
				remove = true;
			else if (c == '+')
				remove = false;
			else if (remove)
				flags.remove(c);
			else
				flags.add(c);
		}
		StringBuilder sb = new StringBuilder();
		for (char c : flags)
			sb.append(c);
		return sb.toString();
	}

	private static Image textToImage(String value, javafx.scene.paint.Paint fill, javafx.scene.paint.Paint stroke,
			double strokeWidth, double size, String fontName, String style) {
		boolean bold = style != null && style.indexOf('B') >= 0;
		boolean italic = style != null && style.indexOf('I') >= 0;
		javafx.scene.text.FontWeight weight = bold ? javafx.scene.text.FontWeight.BOLD
				: javafx.scene.text.FontWeight.NORMAL;
		javafx.scene.text.FontPosture posture = italic ? javafx.scene.text.FontPosture.ITALIC
				: javafx.scene.text.FontPosture.REGULAR;
		javafx.scene.text.Font font = (fontName != null && !fontName.isBlank())
				? javafx.scene.text.Font.font(fontName, weight, posture, size)
				: javafx.scene.text.Font.font(null, weight, posture, size);
		javafx.scene.text.Text node = new javafx.scene.text.Text(value);
		node.setFont(font);
		if (style != null && style.indexOf('U') >= 0)
			node.setUnderline(true);
		if (style != null && style.indexOf('S') >= 0)
			node.setStrikethrough(true);
		if (style != null && style.indexOf('O') >= 0 && stroke != null) {
			node.setStroke(stroke);
			node.setStrokeWidth(strokeWidth);
			node.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
		}
		node.setFill(fill != null ? fill : javafx.scene.paint.Color.TRANSPARENT);
		Bounds b = node.getBoundsInLocal();
		int w = Math.max(1, (int) Math.ceil(b.getWidth()));
		int h = Math.max(1, (int) Math.ceil(b.getHeight()));
		return snapshotToImage(node, w, h);
	}

	/**
	 * Returns an image for the given spec, substituting {@link #SCALE_PLACEHOLDER}
	 * and consulting the cache. Returns {@code null} without a fallback if loading
	 * fails.
	 *
	 * @param spec       the icon spec string; may be {@code null}
	 * @param background if {@code true}, raster images are loaded asynchronously
	 *
	 * @return the image, or {@code null}
	 */
	public static Image getImageIfPossible(String spec, boolean background) {
		if (spec == null)
			return null;

		String nSpec = spec;
		boolean changed;
		do {
			if (!nSpec.contains("${")) //$NON-NLS-1$
				break;
			changed = false;
			if (nSpec.contains(SCALE_PLACEHOLDER)) {
				String next = nSpec.replace(SCALE_PLACEHOLDER, UiScale.bucketedScaleString());
				if (next != nSpec)
					changed = true;
				nSpec = next;
			}
			for (Map.Entry<String, String> e : tokenMap.entrySet()) {
				String next = nSpec.replace("${" + e.getKey() + "}", e.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
				if (next != nSpec)
					changed = true;
				nSpec = next;
			}
		} while (changed);

		jfxStyleTL.remove();
		noCacheTL.remove();
		Image i = iconCache.get(nSpec);
		if (i != null) {
			String cached = jfxStyleCache.get(nSpec);
			if (cached != null)
				jfxStyleTL.set(cached);
			return i;
		}
		i = createImage(nSpec, background);
		if (i != null && noCacheTL.get() == null) {
			iconCache.put(nSpec, i);
			String style = jfxStyleTL.get();
			if (style != null)
				jfxStyleCache.put(nSpec, style);
		}
		return i;
	}

	/**
	 * Returns an image for the given spec; falls back to an error-indicator icon if
	 * loading fails.
	 *
	 * @param spec       the icon spec string
	 * @param background if {@code true}, raster images are loaded asynchronously
	 *
	 * @return the image, or a fallback error icon if the spec cannot be resolved
	 */
	public static Image getImage(String spec, boolean background) {
		Image i = getImageIfPossible(spec, background);
		if (i == null) {
			i = getImageIfPossible("svg/file.svg|16#svg/alert-triangle.svg|8|||red#*ANCHOR|C||2#*+", false); //$NON-NLS-1$
			if (i != null) {
				iconCache.put(spec, i);
			}
		}
		return i;
	}

	/**
	 * Loads an SVG icon scaled to the current UI scale factor.
	 *
	 * @param iconName the SVG file name without extension, resolved via the
	 *                 branding root
	 * @param size     optional target pixel size; {@code null} uses the natural SVG
	 *                 size at current scale
	 *
	 * @return the loaded image, or a fallback error icon
	 */
	public static Image getScaledSvgImage(String iconName, Integer size) {
		if (size == null) {
			return getImage(MessageFormat.format("{0}.svg|||{1}", iconName, scale)); // $NON-NLS-2$
		} else {
			return getImage(MessageFormat.format("{0}.svg||{1}|{2}", iconName, size, scale)); // $NON-NLS-3$
		}
	}

	/**
	 * Loads an SVG icon at a fixed pixel size.
	 *
	 * @param iconName the SVG file name without extension, resolved via the
	 *                 branding root
	 * @param size     target pixel size (both width and height)
	 *
	 * @return the loaded image, or a fallback error icon
	 */
	public static Image getSvgImage(String iconName, Integer size) {
		return getImage(MessageFormat.format("{0}.svg||{1}", iconName, size)); // $NON-NLS-3$
	}

	/**
	 * Loads a raster PNG icon by combining the base name and pixel size into a
	 * spec. Equivalent to {@code getImage(iconNameBase + "_" + size + ".png")}.
	 *
	 * @param iconNameBase the icon name without size suffix and extension
	 * @param size         the pixel size (appended as {@code _{size}.png})
	 *
	 * @return the loaded image, or a fallback error icon
	 */
	public static Image getImage(String iconNameBase, int size) {
		return getImage(iconNameBase + "_" + size + ".png"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Loads an image from a polymorphic input:
	 * <ul>
	 * <li>{@link Image} — returned as-is</li>
	 * <li>{@link ImageView} — returns the view's image</li>
	 * <li>{@link String} — resolved as an icon spec via
	 * {@link #getImage(String, boolean)}</li>
	 * </ul>
	 *
	 * @param input an {@link Image}, {@link ImageView}, spec {@link String}, or
	 *              {@code null}
	 *
	 * @return the resolved image, or {@code null} if {@code input} is an
	 *         unsupported type
	 */
	public static Image getImage(Object input) {
		if (input instanceof Image img)
			return img;
		if (input instanceof ImageView iv)
			return iv.getImage();
		if (input instanceof String s)
			return getImage(s, true);
		return null;
	}

	// ---- Overlay ----

	/**
	 * Composes multiple images (given as spec strings) into a single image using
	 * alpha-compositing at the specified alignment.
	 *
	 * @param align one of {@link #ALIGN_CENTER}, {@link #ALIGN_BOTTOM_RIGHT},
	 *              {@link #ALIGN_TOP_LEFT}, or {@link #ALIGN_BOTTOM_LEFT}
	 * @param args  icon spec strings; {@code null} entries are treated as
	 *              transparent
	 *
	 * @return the composited image, or {@code null} if all images fail to load
	 */
	public static Image overlayImages(int align, String... args) {
		Image[] icons = new Image[args.length];
		for (int i = 0; i < args.length; i++) {
			icons[i] = (args[i] != null ? getImage(args[i], false) : null);
		}
		return overlayImages(align, icons);
	}

	/**
	 * Composes multiple {@link Image} objects into a single image using
	 * alpha-compositing at the specified alignment. The canvas is sized to fit the
	 * largest image; each subsequent image is placed according to {@code align}.
	 *
	 * @param align one of {@link #ALIGN_CENTER}, {@link #ALIGN_BOTTOM_RIGHT},
	 *              {@link #ALIGN_TOP_LEFT}, or {@link #ALIGN_BOTTOM_LEFT}
	 * @param icons images to composite; {@code null} entries are skipped
	 *
	 * @return the composited {@link WritableImage}, or {@code null} if all inputs
	 *         are empty
	 */
	public static Image overlayImages(int align, Image... icons) {
		int[] aligns = new int[icons.length];
		java.util.Arrays.fill(aligns, align);
		return overlayImagesImpl(icons, aligns, null);
	}

	/**
	 * Canonical compositing implementation. {@code aligns[i]} controls the
	 * placement of each image; {@code subtracts[i] == true} applies DST_OUT
	 * (alpha-subtract) compositing instead of the normal SRC_OVER for that image.
	 * Index 0 is always the base (composited normally regardless of
	 * {@code subtracts[0]}). Either array may be {@code null} to use defaults
	 * (global align / add-only).
	 */
	private static Image overlayImagesImpl(Image[] icons, int[] aligns, boolean[] subtracts) {
		int maxW = 0;
		int maxH = 0;

		for (Image icon : icons) {
			if (icon == null)
				continue;
			int w = (int) Math.ceil(icon.getWidth());
			int h = (int) Math.ceil(icon.getHeight());
			if (w > maxW)
				maxW = w;
			if (h > maxH)
				maxH = h;
		}

		if (maxW <= 0 || maxH <= 0) {
			return null;
		}

		WritableImage out = new WritableImage(maxW, maxH);
		PixelWriter pw = out.getPixelWriter();

		// Explicit clear to transparent
		int[] clear = new int[maxW];
		for (int y = 0; y < maxH; y++) {
			pw.setPixels(0, y, maxW, 1, PixelFormat.getIntArgbInstance(), clear, 0, maxW);
		}

		for (int i = 0; i < icons.length; i++) {
			Image icon = icons[i];
			if (icon == null)
				continue;
			PixelReader pr = icon.getPixelReader();
			if (pr == null)
				continue;

			int w = (int) Math.ceil(icon.getWidth());
			int h = (int) Math.ceil(icon.getHeight());

			int align = (aligns != null && i < aligns.length) ? aligns[i] : ALIGN_BOTTOM_RIGHT;
			int dx = 0;
			int dy = 0;

			switch (align) {
			case ALIGN_CENTER -> {
				dx = (maxW - w) / 2;
				dy = (maxH - h) / 2;
			}
			case ALIGN_BOTTOM_RIGHT -> {
				dx = (maxW - w);
				dy = (maxH - h);
			}
			case ALIGN_BOTTOM_LEFT -> {
				dx = 0;
				dy = (maxH - h);
			}
			case ALIGN_TOP_LEFT -> {
				dx = 0;
				dy = 0;
			}
			case ALIGN_TOP_RIGHT -> {
				dx = (maxW - w);
				dy = 0;
			}
			}

			boolean doSubtract = i > 0 && subtracts != null && i < subtracts.length && subtracts[i];
			if (doSubtract) {
				alphaSubtractInto(out, pr, dx, dy, w, h);
			} else {
				alphaCompositeInto(out, pr, dx, dy, w, h);
			}
		}

		return out;
	}

	private static void alphaCompositeInto(WritableImage dst, PixelReader src, int dx, int dy, int w, int h) {
		PixelReader dstReader = dst.getPixelReader();
		PixelWriter dstWriter = dst.getPixelWriter();

		int[] srcRow = new int[w];
		int[] dstRow = new int[w];

		int dstW = (int) dst.getWidth();
		int dstH = (int) dst.getHeight();

		for (int y = 0; y < h; y++) {
			int ty = dy + y;
			if (ty < 0 || ty >= dstH)
				continue;

			int sx0 = 0;
			int tx0 = dx;
			int len = w;

			if (tx0 < 0) {
				sx0 = -tx0;
				len -= sx0;
				tx0 = 0;
			}
			if (tx0 + len > dstW) {
				len = dstW - tx0;
			}
			if (len <= 0)
				continue;

			src.getPixels(sx0, y, len, 1, PixelFormat.getIntArgbInstance(), srcRow, 0, len);
			dstReader.getPixels(tx0, ty, len, 1, PixelFormat.getIntArgbInstance(), dstRow, 0, len);

			for (int x = 0; x < len; x++) {
				int s = srcRow[x];
				int d = dstRow[x];

				int sa = (s >>> 24) & 0xFF;
				if (sa == 0)
					continue;
				if (sa == 255) {
					dstRow[x] = s;
					continue;
				}

				int sr = (s >>> 16) & 0xFF;
				int sg = (s >>> 8) & 0xFF;
				int sb = s & 0xFF;

				int da = (d >>> 24) & 0xFF;
				int dr = (d >>> 16) & 0xFF;
				int dg = (d >>> 8) & 0xFF;
				int db = d & 0xFF;

				int invSa = 255 - sa;

				int outA = sa + (da * invSa + 127) / 255;
				int outR = (sr * sa + dr * da * invSa / 255 + 127) / 255;
				int outG = (sg * sa + dg * da * invSa / 255 + 127) / 255;
				int outB = (sb * sa + db * da * invSa / 255 + 127) / 255;

				dstRow[x] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
			}

			dstWriter.setPixels(tx0, ty, len, 1, PixelFormat.getIntArgbInstance(), dstRow, 0, len);
		}
	}

	/**
	 * DST_OUT compositing: source alpha subtracts from destination alpha; RGB
	 * unchanged.
	 */
	private static void alphaSubtractInto(WritableImage dst, PixelReader src, int dx, int dy, int w, int h) {
		PixelReader dstReader = dst.getPixelReader();
		PixelWriter dstWriter = dst.getPixelWriter();

		int[] srcRow = new int[w];
		int[] dstRow = new int[w];

		int dstW = (int) dst.getWidth();
		int dstH = (int) dst.getHeight();

		for (int y = 0; y < h; y++) {
			int ty = dy + y;
			if (ty < 0 || ty >= dstH)
				continue;

			int sx0 = 0;
			int tx0 = dx;
			int len = w;

			if (tx0 < 0) {
				sx0 = -tx0;
				len -= sx0;
				tx0 = 0;
			}
			if (tx0 + len > dstW) {
				len = dstW - tx0;
			}
			if (len <= 0)
				continue;

			src.getPixels(sx0, y, len, 1, PixelFormat.getIntArgbInstance(), srcRow, 0, len);
			dstReader.getPixels(tx0, ty, len, 1, PixelFormat.getIntArgbInstance(), dstRow, 0, len);

			for (int x = 0; x < len; x++) {
				int sa = (srcRow[x] >>> 24) & 0xFF;
				if (sa == 0)
					continue;
				int d = dstRow[x];
				int da = (d >>> 24) & 0xFF;
				if (da == 0)
					continue;
				int outA = sa == 255 ? 0 : (da * (255 - sa) + 127) / 255;
				dstRow[x] = (outA << 24) | (d & 0x00FFFFFF);
			}

			dstWriter.setPixels(tx0, ty, len, 1, PixelFormat.getIntArgbInstance(), dstRow, 0, len);
		}
	}

	/**
	 * Returns the classpath URL for the given icon resource path, or {@code null}
	 * if not found.
	 *
	 * @param iconPath absolute classpath resource path (e.g.
	 *                 {@code "/cz/bliksoft/branding/images/24/SAVE.png"})
	 *
	 * @return the URL, or {@code null}
	 */
	public static java.net.URL getIconUrl(String iconPath) {
		return ImageUtils.class.getResource(iconPath);
	}

	/**
	 * Composes multiple images and wraps the result in an {@link ImageView}.
	 *
	 * @param align one of the {@code ALIGN_*} constants
	 * @param icons images to composite; {@code null} entries are skipped
	 *
	 * @return an {@link ImageView} containing the composited image, or {@code null}
	 */
	public static ImageView overlayIconViews(int align, Image... icons) {
		Image img = overlayImages(align, icons);
		return (img != null ? new ImageView(img) : null);
	}

	/**
	 * Resolves spec strings to images, composites them, and wraps the result in an
	 * {@link ImageView}.
	 *
	 * @param align one of the {@code ALIGN_*} constants
	 * @param args  icon spec strings; {@code null} entries are treated as
	 *              transparent
	 *
	 * @return an {@link ImageView} containing the composited image, or {@code null}
	 */
	public static ImageView overlayIconViews(int align, String... args) {
		Image img = overlayImages(align, args);
		return (img != null ? new ImageView(img) : null);
	}

	/**
	 * Returns an {@link ImageView} wrapping the standard transparent placeholder
	 * image.
	 *
	 * @return an empty image view, or {@code null} if the placeholder is
	 *         unavailable
	 */
	public static ImageView getEmptyImageView() {
		Image img = createImage("EMPTY|16", false);
		return (img != null ? new ImageView(img) : null);
	}

	/**
	 * Returns an {@link ImageView} from a polymorphic input ({@link Image} or spec
	 * {@link String}).
	 *
	 * @param input an {@link Image} or icon spec {@link String}; {@code null}
	 *              returns {@code null}
	 *
	 * @return the image view, or {@code null}
	 */
	public static ImageView getIconView(Object input) {
		if (input instanceof Image)
			return new ImageView((Image) input);
		else if (input instanceof String) {
			return getIconView((String) input);
		}
		return null;
	}

	/**
	 * Returns an {@link ImageView} for the given icon spec, without explicit style.
	 *
	 * @param spec the icon spec string; may be {@code null}
	 *
	 * @return the image view, or {@code null} if the image cannot be loaded
	 */
	public static ImageView getIconView(String spec) {
		return getIconView(spec, (String) null);
	}

	/**
	 * Returns an {@link ImageView} for the given icon spec with an optional CSS
	 * style. For {@code [P]:} and {@code [PS]:} path specs, use
	 * {@link #getIconNode} instead.
	 *
	 * @param spec  the icon spec string; may be {@code null}
	 * @param style an optional inline CSS style applied to the view, or
	 *              {@code null}; for SVG specs the style embedded in the spec is
	 *              used when {@code style} is {@code null}
	 *
	 * @return the styled image view, or {@code null} if the image cannot be loaded
	 *
	 * @throws IllegalArgumentException if {@code spec} starts with the {@code [P]:}
	 *                                  prefix
	 */
	public static ImageView getIconView(String spec, String style) {
		if (spec == null)
			return null;

		if (spec.startsWith(PREFIX_PATH)) {
			throw new IllegalArgumentException("[P]: path specs return SVGPath Node; use getIconNode(spec) instead.");
		}

		Image img = getImage(spec, false);
		if (img == null)
			return null;

		ImageView iv = new ImageView(img);
		String s = (style != null) ? style : jfxStyleTL.get();
		if (s != null && !s.isBlank()) {
			iv.setStyle(s);
		}
		return iv;
	}

	/**
	 * Returns a size-constrained {@link ImageView} for the given icon spec.
	 *
	 * @param spec the icon spec string
	 * @param size the width and height to set on the view (preserving ratio)
	 *
	 * @return the image view, or {@code null}
	 */
	public static ImageView getIconView(String spec, double size) {
		return getIconView(spec, size, null);
	}

	/**
	 * Returns a size-constrained {@link ImageView} for the given icon spec with an
	 * optional CSS style.
	 *
	 * @param spec  the icon spec string
	 * @param size  the width and height to set on the view (preserving ratio)
	 * @param style an optional inline CSS style applied to the view, or
	 *              {@code null}
	 *
	 * @return the image view, or {@code null}
	 */
	public static ImageView getIconView(String spec, double size, String style) {
		ImageView iv = getIconView(spec, style);
		if (iv == null)
			return null;

		iv.setPreserveRatio(true);
		iv.setSmooth(true);
		iv.setFitWidth(size);
		iv.setFitHeight(size);
		return iv;
	}

	/**
	 * Converts a spec color token (which cannot contain {@code #}) to a CSS color
	 * string usable in SVG attributes.
	 * <ul>
	 * <li>3, 4, 6, or 8 hex chars (e.g. {@code 333333}, {@code ff000080}) →
	 * {@code #333333}, {@code #ff000080}; 4-char is {@code #RGBA}, 8-char is
	 * {@code #RRGGBBAA}</li>
	 * <li>{@code 0xRRGGBB} / {@code 0xRRGGBBAA} → {@code #RRGGBB} /
	 * {@code #RRGGBBAA}</li>
	 * <li>CSS named colors, {@code none}, {@code rgb(...)} etc. → returned
	 * as-is</li>
	 * </ul>
	 */
	private static String resolveSpecColor(String s) {
		if (s == null || s.isBlank())
			return null;
		if (s.startsWith("0x") || s.startsWith("0X"))
			return "#" + s.substring(2);
		if (s.matches("[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8}"))
			return "#" + s;
		return s;
	}

	private static javafx.scene.paint.Paint resolveModePaint(Object colorValue) {
		if (colorValue == null)
			return null;
		String s = colorValue.toString();
		if ("none".equalsIgnoreCase(s)) //$NON-NLS-1$
			return null;
		String css = resolveSpecColor(s);
		if (css == null)
			return null;
		try {
			return javafx.scene.paint.Color.web(css);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static Image drawOnImage(Image base, String shape, double[] p, javafx.scene.paint.Paint fill,
			javafx.scene.paint.Paint stroke, double strokeWidth, double refX, double refY) {
		int w = (int) Math.ceil(base.getWidth());
		int h = (int) Math.ceil(base.getHeight());
		javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(w, h);
		javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.drawImage(base, 0, 0);
		gc.setLineWidth(strokeWidth);
		switch (shape) {
		case "line" -> { // p: x1 y1 x2 y2 (relative to refX/refY) //$NON-NLS-1$
			if (stroke != null) {
				gc.setStroke(stroke);
				gc.strokeLine(refX + p[0], refY + p[1], refX + p[2], refY + p[3]);
			}
		}
		case "circle" -> { // p: cx cy r (cx/cy relative to refX/refY) //$NON-NLS-1$
			double cx = refX + p[0], cy = refY + p[1], r = p[2];
			if (fill != null) {
				gc.setFill(fill);
				gc.fillOval(cx - r, cy - r, r * 2, r * 2);
			}
			if (stroke != null) {
				gc.setStroke(stroke);
				gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
			}
		}
		case "square" -> { // p: x y side (top-left relative to refX/refY) //$NON-NLS-1$
			if (fill != null) {
				gc.setFill(fill);
				gc.fillRect(refX + p[0], refY + p[1], p[2], p[2]);
			}
			if (stroke != null) {
				gc.setStroke(stroke);
				gc.strokeRect(refX + p[0], refY + p[1], p[2], p[2]);
			}
		}
		case "rectangle" -> { // p: x y w h (top-left relative to refX/refY) //$NON-NLS-1$
			if (fill != null) {
				gc.setFill(fill);
				gc.fillRect(refX + p[0], refY + p[1], p[2], p[3]);
			}
			if (stroke != null) {
				gc.setStroke(stroke);
				gc.strokeRect(refX + p[0], refY + p[1], p[2], p[3]);
			}
		}
		default -> log.warn("Unknown DRAW shape: {}", shape); //$NON-NLS-1$
		}
		return snapshotToImage(canvas, w, h);
	}

	private static final String PREFIX_FILE = "[F]:";
	private static final String PREFIX_PATH = "[P]:";
	private static final String PREFIX_PATH_IMAGE = "[PI]:";
	private static final String PREFIX_PATH_SHAPE = "[PS]:";

	/**
	 * Generic icon loader that returns the most appropriate node type for the spec:
	 * <ul>
	 * <li>{@code [P]:...} — returns a styled and scaled
	 * {@link javafx.scene.shape.SVGPath} node</li>
	 * <li>{@code [PS]:...} — returns a managed layoutable
	 * {@link javafx.scene.shape.Shape} node</li>
	 * <li>anything else — returns an {@link ImageView}</li>
	 * </ul>
	 *
	 * @param spec the icon spec string; may be {@code null}
	 *
	 * @return the appropriate node, or {@code null} if {@code spec} is {@code null}
	 */
	public static javafx.scene.Node getIconNode(String spec) {
		if (spec == null)
			return null;

		if (spec.startsWith(PREFIX_PATH_SHAPE)) {
			PathSpec ps = parsePathSpec(spec, PREFIX_PATH_SHAPE);
			return createSvgPathShape(ps);
		} else if (spec.startsWith(PREFIX_PATH)) {
			PathSpec ps = parsePathSpec(spec, PREFIX_PATH);
			return createSvgPathNode(ps);
		}

		// default: image-based => ImageView
		return getIconView(spec);
	}

	private static class PathSpec {
		final String pathData;
		final Float w;
		final Float h;
		final Float s;
		final String style;

		PathSpec(String pathData, Float w, Float h, Float s, String style) {
			this.pathData = pathData;
			this.w = w;
			this.h = h;
			this.s = s;
			this.style = style;
		}
	}

	private static PathSpec parsePathSpec(String spec, String prefix) {
		// Format: [P]:<pathData>|w|h|scale|style
		String rest = spec.substring(prefix.length());
		String[] p = rest.split("\\|", -1);

		String pathData = (p.length >= 1) ? p[0] : "";
		Float w = (p.length > 1 && StringUtils.hasLength(p[1])) ? Float.valueOf(p[1]) : null;
		Float h = (p.length > 2 && StringUtils.hasLength(p[2])) ? Float.valueOf(p[2]) : null;
		Float s = (p.length > 3 && StringUtils.hasLength(p[3])) ? Float.valueOf(p[3]) : null;
		String style = (p.length > 4 && StringUtils.hasLength(p[4])) ? p[4] : null;

		return new PathSpec(pathData, w, h, s, style);
	}

	private static javafx.scene.shape.SVGPath createSvgPathNode(PathSpec ps) {
		javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
		path.setContent(ps.pathData);

		if (ps.style != null && !ps.style.isBlank()) {
			path.setStyle(ps.style);
		}

		applyBestFitScale(path, ps.w, ps.h, ps.s);
		return path;
	}

	/**
	 * Scales a node to best-fit into (w,h) with optional extra scale multiplier.
	 * Uses boundsInLocal, adds a Scale transform.
	 */
	private static void applyBestFitScale(javafx.scene.Node node, Float w, Float h, Float extraScale) {
		double mul = (extraScale != null) ? extraScale.doubleValue() : 1.0;

		// If no target size, apply only extra scale
		if (w == null && h == null) {
			if (mul != 1.0)
				node.getTransforms().add(new javafx.scene.transform.Scale(mul, mul));
			return;
		}

		javafx.geometry.Bounds b = node.getBoundsInLocal();
		double bw = b.getWidth();
		double bh = b.getHeight();

		if (bw <= 0 || bh <= 0) {
			if (mul != 1.0)
				node.getTransforms().add(new javafx.scene.transform.Scale(mul, mul));
			return;
		}

		double sx = (w != null) ? (w.doubleValue() / bw) : Double.POSITIVE_INFINITY;
		double sy = (h != null) ? (h.doubleValue() / bh) : Double.POSITIVE_INFINITY;

		double best = Math.min(sx, sy);
		if (!Double.isFinite(best) || best <= 0)
			best = 1.0;

		double finalScale = best * mul;
		node.getTransforms().add(new javafx.scene.transform.Scale(finalScale, finalScale));
	}

	/**
	 * Renders a JavaFX {@link javafx.scene.Node} into a
	 * {@link javafx.scene.image.WritableImage} of the given size. If called from
	 * the JavaFX Application Thread, the snapshot is taken synchronously; otherwise
	 * it is submitted via {@link javafx.application.Platform#runLater} and the
	 * calling thread blocks until the result is available.
	 *
	 * @param node the node to snapshot; must be fully laid-out and styled
	 * @param w    the output image width in pixels (minimum 1)
	 * @param h    the output image height in pixels (minimum 1)
	 *
	 * @return the rendered image; never {@code null}
	 *
	 * @throws RuntimeException if the snapshot task is interrupted or fails
	 */
	public static Image snapshotToImage(javafx.scene.Node node, int w, int h) {
		javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
		params.setFill(javafx.scene.paint.Color.TRANSPARENT);

		javafx.scene.image.WritableImage target = new javafx.scene.image.WritableImage(Math.max(1, w), Math.max(1, h));

		if (javafx.application.Platform.isFxApplicationThread()) {
			return node.snapshot(params, target);
		}

		final java.util.concurrent.FutureTask<Image> task = new java.util.concurrent.FutureTask<>(
				() -> node.snapshot(params, target));

		javafx.application.Platform.runLater(task);

		try {
			return task.get();
		} catch (Exception e) {
			throw new RuntimeException("Failed to snapshot node to image", e);
		}
	}

	private static javafx.scene.shape.Shape createSvgPathShape(PathSpec ps) {
		javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
		path.setContent(ps.pathData);

		if (ps.style != null && !ps.style.isBlank()) {
			path.setStyle(ps.style);
		}

		applyBestFitScale(path, ps.w, ps.h, ps.s);

		// Make it behave like a layoutable shape
		path.setManaged(true);
		path.setPickOnBounds(true);

		return path;
	}

	// ---- Save ----

	/**
	 * Saves one or more iconspec images to a file. The output format is determined
	 * by the file extension:
	 * <ul>
	 * <li>{@code .png} — all specs are composited (overlaid at bottom-right) into a
	 * single PNG; if only one spec is given it is saved as-is.</li>
	 * <li>{@code .ico} — each spec is rendered as an independent frame in a
	 * multi-frame Windows ICO file (PNG-in-ICO encoding, Vista+ compatible).</li>
	 * </ul>
	 *
	 * @param target    the destination file ({@code .png} or {@code .ico})
	 * @param iconspecs one or more icon spec strings; see class Javadoc for the
	 *                  spec format
	 *
	 * @throws IllegalArgumentException if no specs are given or the file extension
	 *                                  is not {@code .png} / {@code .ico}
	 * @throws IOException              if any spec cannot be resolved or file
	 *                                  writing fails
	 */
	public static void saveImage(File target, String... iconspecs) throws IOException {
		if (iconspecs == null || iconspecs.length == 0)
			throw new IllegalArgumentException("At least one iconspec is required");

		String name = target.getName().toLowerCase(java.util.Locale.ROOT);

		if (name.endsWith(".png")) { //$NON-NLS-1$
			Image img;
			if (iconspecs.length == 1) {
				img = getImageIfPossible(iconspecs[0], false);
			} else {
				Image[] images = new Image[iconspecs.length];
				for (int i = 0; i < iconspecs.length; i++) {
					images[i] = getImageIfPossible(iconspecs[i], false);
				}
				img = overlayImages(ALIGN_BOTTOM_RIGHT, images);
			}
			if (img == null)
				throw new IOException("Could not render iconspec: " + iconspecs[0]);
			BufferedImage bi = SwingFXUtils.fromFXImage(img, null);
			if (!ImageIO.write(bi, "png", target)) //$NON-NLS-1$
				throw new IOException("No PNG ImageIO writer available");

		} else if (name.endsWith(".ico")) { //$NON-NLS-1$
			List<BufferedImage> frames = new ArrayList<>(iconspecs.length);
			for (String spec : iconspecs) {
				Image img = getImageIfPossible(spec, false);
				if (img == null)
					throw new IOException("Could not render iconspec: " + spec);
				frames.add(SwingFXUtils.fromFXImage(img, null));
			}
			IcoWriter.write(target, frames);

		} else {
			throw new IllegalArgumentException("Unsupported format (expected .png or .ico): " + target.getName());
		}
	}

	// ---- Filters ----

	private static Image filterShadow(Image img, int argbColor, int radius, boolean filled) {
		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int[] src = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);

		int[] workAlpha = silhouetteAlpha(src, filled);
		int[] dilated = dilateAlpha(workAlpha, w, h, radius);
		int[] blurred = gaussianBlurAlpha(dilated, w, h, radius / 2.0 + 0.5);

		int r = (argbColor >>> 16) & 0xFF;
		int g = (argbColor >>> 8) & 0xFF;
		int b = argbColor & 0xFF;
		int[] dst = new int[w * h];
		for (int i = 0; i < dst.length; i++) {
			int a = Math.max(blurred[i], workAlpha[i]);
			dst[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}

		WritableImage out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), dst, 0, w);
		return out;
	}

	private static Image filterOutline(Image img, int argbColor, int radius, boolean filled) {
		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int[] src = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);

		int[] workAlpha = silhouetteAlpha(src, filled);
		int[] dilated = dilateAlpha(workAlpha, w, h, radius);

		int r = (argbColor >>> 16) & 0xFF;
		int g = (argbColor >>> 8) & 0xFF;
		int b = argbColor & 0xFF;
		int[] dst = new int[w * h];
		for (int i = 0; i < dst.length; i++) {
			int a = Math.max(dilated[i], workAlpha[i]);
			dst[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}

		WritableImage out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), dst, 0, w);
		return out;
	}

	private static Image filterRotate(Image img, double angleDeg) {
		int norm = (int) (((angleDeg % 360) + 360) % 360);
		if (angleDeg == Math.floor(angleDeg) && norm % 90 == 0) {
			if (norm == 0)
				return img;
			return rotateImage(img, norm);
		}

		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int[] src = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);

		double rad = Math.toRadians(angleDeg);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double cx = (w - 1) / 2.0;
		double cy = (h - 1) / 2.0;

		int[] dst = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double dxOut = x - cx;
				double dyOut = y - cy;
				// Inverse of CW screen-space rotation: rotate back (CCW)
				double srcX = cx + dxOut * cos + dyOut * sin;
				double srcY = cy - dxOut * sin + dyOut * cos;
				dst[y * w + x] = bilinearSample(src, w, h, srcX, srcY);
			}
		}

		WritableImage out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), dst, 0, w);
		return out;
	}

	private static Image filterShift(Image img, double angleDeg, double pixels) {
		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		double rad = Math.toRadians(angleDeg);
		double dx = pixels * Math.cos(rad);
		double dy = pixels * Math.sin(rad);

		// Lossless path: axis-aligned integer shift
		int norm = (int) (((angleDeg % 360) + 360) % 360);
		boolean isAxisAligned = (angleDeg == Math.floor(angleDeg)) && (norm % 90 == 0);
		boolean isWholePixel = (pixels == Math.floor(pixels));
		if (isAxisAligned && isWholePixel) {
			int idx = (int) dx;
			int idy = (int) dy;
			int[] src = new int[w * h];
			pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);
			int[] dst = new int[w * h];
			for (int y = 0; y < h; y++) {
				int sy = y - idy;
				if (sy < 0 || sy >= h)
					continue;
				for (int x = 0; x < w; x++) {
					int sx = x - idx;
					if (sx < 0 || sx >= w)
						continue;
					dst[y * w + x] = src[sy * w + sx];
				}
			}
			WritableImage out = new WritableImage(w, h);
			out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), dst, 0, w);
			return out;
		}

		// General bilinear path
		int[] src = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);
		int[] dst = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				dst[y * w + x] = bilinearSample(src, w, h, x - dx, y - dy);
			}
		}
		WritableImage out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), dst, 0, w);
		return out;
	}

	private static Image filterScale(Image img, String wStr, String hStr, String modeStr) {
		int srcW = (int) Math.ceil(img.getWidth());
		int srcH = (int) Math.ceil(img.getHeight());
		if (srcW <= 0 || srcH <= 0)
			return img;

		boolean hasW = StringUtils.hasLength(wStr);
		boolean hasH = StringUtils.hasLength(hStr);
		String mode = (modeStr != null) ? modeStr.trim() : ""; //$NON-NLS-1$

		if ("%".equals(mode) && hasW) { //$NON-NLS-1$
			double pct;
			try {
				pct = Double.parseDouble(wStr.trim()) / 100.0;
			} catch (NumberFormatException e) {
				return img;
			}
			int dstW = Math.max(1, (int) Math.round(srcW * pct));
			int dstH = Math.max(1, (int) Math.round(srcH * pct));
			return applyScaleToImage(img, srcW, srcH, dstW, dstH);
		}

		if ("F".equalsIgnoreCase(mode) && hasW && hasH) { //$NON-NLS-1$
			int targetW, targetH;
			try {
				targetW = Integer.parseInt(wStr.trim());
				targetH = Integer.parseInt(hStr.trim());
			} catch (NumberFormatException e) {
				return img;
			}
			double factor = Math.min((double) targetW / srcW, (double) targetH / srcH);
			int scaledW = Math.max(1, (int) Math.round(srcW * factor));
			int scaledH = Math.max(1, (int) Math.round(srcH * factor));
			int[] src = new int[srcW * srcH];
			img.getPixelReader().getPixels(0, 0, srcW, srcH, PixelFormat.getIntArgbInstance(), src, 0, srcW);
			int[] scaled = scalePixels(src, srcW, srcH, scaledW, scaledH);
			// Fit: scaled image centred on target canvas
			WritableImage canvas = new WritableImage(targetW, targetH);
			int offX = (targetW - scaledW) / 2;
			int offY = (targetH - scaledH) / 2;
			int[] row = new int[scaledW];
			PixelWriter pw = canvas.getPixelWriter();
			for (int y = 0; y < scaledH; y++) {
				System.arraycopy(scaled, y * scaledW, row, 0, scaledW);
				pw.setPixels(offX, offY + y, scaledW, 1, PixelFormat.getIntArgbInstance(), row, 0, scaledW);
			}
			return canvas;
		}

		if ("C".equalsIgnoreCase(mode) && hasW && hasH) { //$NON-NLS-1$
			int targetW, targetH;
			try {
				targetW = Integer.parseInt(wStr.trim());
				targetH = Integer.parseInt(hStr.trim());
			} catch (NumberFormatException e) {
				return img;
			}
			double factor = Math.max((double) targetW / srcW, (double) targetH / srcH);
			int scaledW = Math.max(1, (int) Math.round(srcW * factor));
			int scaledH = Math.max(1, (int) Math.round(srcH * factor));
			int[] src = new int[srcW * srcH];
			img.getPixelReader().getPixels(0, 0, srcW, srcH, PixelFormat.getIntArgbInstance(), src, 0, srcW);
			int[] scaled = scalePixels(src, srcW, srcH, scaledW, scaledH);
			// Crop to targetW×targetH centred
			int offX = (scaledW - targetW) / 2;
			int offY = (scaledH - targetH) / 2;
			WritableImage out = new WritableImage(targetW, targetH);
			PixelWriter pw = out.getPixelWriter();
			int[] row = new int[targetW];
			for (int y = 0; y < targetH; y++) {
				int sy = offY + y;
				if (sy < 0 || sy >= scaledH)
					continue;
				int copyStart = Math.max(0, offX);
				int copyEnd = Math.min(offX + targetW, scaledW);
				if (copyEnd <= copyStart)
					continue;
				int len = copyEnd - copyStart;
				System.arraycopy(scaled, sy * scaledW + copyStart, row, copyStart - offX, len);
				pw.setPixels(copyStart - offX, y, len, 1, PixelFormat.getIntArgbInstance(), row, copyStart - offX,
						targetW);
			}
			return out;
		}

		// Default: proportional (one dimension) or stretch (both)
		int dstW, dstH;
		if (hasW && hasH) {
			try {
				dstW = Math.max(1, Integer.parseInt(wStr.trim()));
				dstH = Math.max(1, Integer.parseInt(hStr.trim()));
			} catch (NumberFormatException e) {
				return img;
			}
		} else if (hasW) {
			try {
				dstW = Math.max(1, Integer.parseInt(wStr.trim()));
			} catch (NumberFormatException e) {
				return img;
			}
			dstH = Math.max(1, (int) Math.round((double) srcH * dstW / srcW));
		} else if (hasH) {
			try {
				dstH = Math.max(1, Integer.parseInt(hStr.trim()));
			} catch (NumberFormatException e) {
				return img;
			}
			dstW = Math.max(1, (int) Math.round((double) srcW * dstH / srcH));
		} else {
			return img;
		}
		return applyScaleToImage(img, srcW, srcH, dstW, dstH);
	}

	private static Image applyScaleToImage(Image img, int srcW, int srcH, int dstW, int dstH) {
		int[] src = new int[srcW * srcH];
		img.getPixelReader().getPixels(0, 0, srcW, srcH, PixelFormat.getIntArgbInstance(), src, 0, srcW);
		int[] dst = scalePixels(src, srcW, srcH, dstW, dstH);
		WritableImage out = new WritableImage(dstW, dstH);
		out.getPixelWriter().setPixels(0, 0, dstW, dstH, PixelFormat.getIntArgbInstance(), dst, 0, dstW);
		return out;
	}

	private static int[] scalePixels(int[] src, int srcW, int srcH, int dstW, int dstH) {
		int[] dst = new int[dstW * dstH];
		double sx = (double) srcW / dstW;
		double sy = (double) srcH / dstH;
		for (int y = 0; y < dstH; y++) {
			for (int x = 0; x < dstW; x++) {
				dst[y * dstW + x] = bilinearSample(src, srcW, srcH, x * sx, y * sy);
			}
		}
		return dst;
	}

	private static Image filterResize(Image img, int newW, int newH, int align, int offsetX, int offsetY) {
		int srcW = (int) Math.ceil(img.getWidth());
		int srcH = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int dstX, dstY;
		switch (align) {
		case ALIGN_TOP_LEFT -> {
			dstX = offsetX;
			dstY = offsetY;
		}
		case ALIGN_TOP_RIGHT -> {
			dstX = newW - srcW + offsetX;
			dstY = offsetY;
		}
		case ALIGN_BOTTOM_LEFT -> {
			dstX = offsetX;
			dstY = newH - srcH + offsetY;
		}
		case ALIGN_CENTER -> {
			dstX = (newW - srcW) / 2 + offsetX;
			dstY = (newH - srcH) / 2 + offsetY;
		}
		default -> {
			dstX = newW - srcW + offsetX;
			dstY = newH - srcH + offsetY;
		} // ALIGN_BOTTOM_RIGHT
		}

		WritableImage out = new WritableImage(newW, newH);
		int srcStartX = Math.max(0, -dstX);
		int srcStartY = Math.max(0, -dstY);
		int canvasStartX = Math.max(0, dstX);
		int canvasStartY = Math.max(0, dstY);
		int copyW = Math.min(srcW - srcStartX, newW - canvasStartX);
		int copyH = Math.min(srcH - srcStartY, newH - canvasStartY);
		if (copyW > 0 && copyH > 0) {
			int[] row = new int[copyW];
			PixelWriter pw = out.getPixelWriter();
			for (int y = 0; y < copyH; y++) {
				pr.getPixels(srcStartX, srcStartY + y, copyW, 1, PixelFormat.getIntArgbInstance(), row, 0, copyW);
				pw.setPixels(canvasStartX, canvasStartY + y, copyW, 1, PixelFormat.getIntArgbInstance(), row, 0, copyW);
			}
		}
		return out;
	}

	private static Image filterMask(Image img, int argbColor, boolean invert) {
		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int[] src = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);

		int r = (argbColor >>> 16) & 0xFF;
		int g = (argbColor >>> 8) & 0xFF;
		int b = argbColor & 0xFF;
		int colorAlpha = (argbColor >>> 24) & 0xFF;

		int[] dst = new int[w * h];
		for (int i = 0; i < src.length; i++) {
			int a = (src[i] >>> 24) & 0xFF;
			if (invert)
				a = 255 - a;
			int outA = colorAlpha == 255 ? a : (a * colorAlpha + 127) / 255;
			dst[i] = (outA << 24) | (r << 16) | (g << 8) | b;
		}

		WritableImage out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), dst, 0, w);
		return out;
	}

	private static Image filterMonochrome(Image img, int argbColor) {
		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int[] src = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);

		int cr = (argbColor >>> 16) & 0xFF;
		int cg = (argbColor >>> 8) & 0xFF;
		int cb = argbColor & 0xFF;

		int[] dst = new int[w * h];
		for (int i = 0; i < src.length; i++) {
			int p = src[i];
			int sa = (p >>> 24) & 0xFF;
			int sr = (p >>> 16) & 0xFF;
			int sg = (p >>> 8) & 0xFF;
			int sb = p & 0xFF;
			int L = (299 * sr + 587 * sg + 114 * sb) / 1000;
			dst[i] = (sa << 24) | ((cr * L / 255) << 16) | ((cg * L / 255) << 8) | (cb * L / 255);
		}

		WritableImage out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), dst, 0, w);
		return out;
	}

	private static Image filterKeymask(Image img, String colorSpec) {
		int w = (int) Math.ceil(img.getWidth());
		int h = (int) Math.ceil(img.getHeight());
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int[] src = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), src, 0, w);

		int keyRgb;
		if (StringUtils.hasLength(colorSpec)) {
			keyRgb = parseArgbColor(colorSpec, 0) & 0x00FFFFFF;
		} else {
			// Majority colour among the four corners (RGB only, ignore alpha)
			int[] corners = { src[0] & 0x00FFFFFF, src[w - 1] & 0x00FFFFFF, src[(h - 1) * w] & 0x00FFFFFF,
					src[(h - 1) * w + w - 1] & 0x00FFFFFF };
			keyRgb = cornerMajorityRgb(corners);
		}

		int[] dst = new int[w * h];
		for (int i = 0; i < src.length; i++)
			dst[i] = (src[i] & 0x00FFFFFF) == keyRgb ? 0 : src[i];

		WritableImage out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), dst, 0, w);
		return out;
	}

	// ---- Filter helpers ----

	/**
	 * Returns the alpha channel, binarising to 0/255 when {@code filled} is true.
	 */
	private static int[] silhouetteAlpha(int[] src, boolean filled) {
		int[] alpha = new int[src.length];
		for (int i = 0; i < src.length; i++) {
			int a = (src[i] >>> 24) & 0xFF;
			alpha[i] = (filled && a > 0) ? 255 : a;
		}
		return alpha;
	}

	private static int cornerMajorityRgb(int[] corners) {
		int best = corners[0], bestCount = 1;
		for (int i = 0; i < corners.length; i++) {
			int count = 0;
			for (int j = 0; j < corners.length; j++)
				if (corners[j] == corners[i])
					count++;
			if (count > bestCount) {
				bestCount = count;
				best = corners[i];
			}
		}
		return best;
	}

	private static int[] dilateAlpha(int[] alpha, int w, int h, int radius) {
		int[] out = new int[w * h];
		int r2 = radius * radius;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int maxA = 0;
				for (int dy = -radius; dy <= radius && maxA < 255; dy++) {
					int ny = y + dy;
					if (ny < 0 || ny >= h)
						continue;
					for (int dx = -radius; dx <= radius && maxA < 255; dx++) {
						if (dx * dx + dy * dy > r2)
							continue;
						int nx = x + dx;
						if (nx < 0 || nx >= w)
							continue;
						int a = alpha[ny * w + nx];
						if (a > maxA)
							maxA = a;
					}
				}
				out[y * w + x] = maxA;
			}
		}
		return out;
	}

	private static int[] gaussianBlurAlpha(int[] alpha, int w, int h, double sigma) {
		int radius = (int) Math.ceil(sigma * 3);
		if (radius <= 0)
			return alpha.clone();

		double[] kernel = new double[2 * radius + 1];
		double twoSigmaSq = 2 * sigma * sigma;
		for (int i = -radius; i <= radius; i++) {
			kernel[i + radius] = Math.exp(-(double) i * i / twoSigmaSq);
		}

		int[] tmp = new int[w * h];
		// Horizontal pass
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double sum = 0, wt = 0;
				for (int k = -radius; k <= radius; k++) {
					int nx = x + k;
					if (nx < 0 || nx >= w)
						continue;
					double kw = kernel[k + radius];
					sum += alpha[y * w + nx] * kw;
					wt += kw;
				}
				tmp[y * w + x] = (int) Math.round(sum / wt);
			}
		}

		int[] out = new int[w * h];
		// Vertical pass
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double sum = 0, wt = 0;
				for (int k = -radius; k <= radius; k++) {
					int ny = y + k;
					if (ny < 0 || ny >= h)
						continue;
					double kw = kernel[k + radius];
					sum += tmp[ny * w + x] * kw;
					wt += kw;
				}
				out[y * w + x] = (int) Math.round(sum / wt);
			}
		}
		return out;
	}

	private static int bilinearSample(int[] src, int srcW, int srcH, double x, double y) {
		if (x < 0 || y < 0 || x > srcW - 1 || y > srcH - 1)
			return 0;
		int x0 = (int) x, y0 = (int) y;
		int x1 = Math.min(x0 + 1, srcW - 1);
		int y1 = Math.min(y0 + 1, srcH - 1);
		double tx = x - x0, ty = y - y0;

		int p00 = src[y0 * srcW + x0], p10 = src[y0 * srcW + x1];
		int p01 = src[y1 * srcW + x0], p11 = src[y1 * srcW + x1];

		// Blend in premultiplied-alpha space to avoid colour bleed at transparent edges
		int a00 = (p00 >>> 24) & 0xFF, a10 = (p10 >>> 24) & 0xFF;
		int a01 = (p01 >>> 24) & 0xFF, a11 = (p11 >>> 24) & 0xFF;
		int outA = clamp255((int) Math.round(bilerp(a00, a10, a01, a11, tx, ty)));

		if (outA == 0)
			return 0;

		// Premultiply
		double pm00 = a00 / 255.0, pm10 = a10 / 255.0, pm01 = a01 / 255.0, pm11 = a11 / 255.0;
		int outR = clamp255((int) Math.round(bilerp((p00 >> 16 & 0xFF) * pm00, (p10 >> 16 & 0xFF) * pm10,
				(p01 >> 16 & 0xFF) * pm01, (p11 >> 16 & 0xFF) * pm11, tx, ty) / (outA / 255.0)));
		int outG = clamp255((int) Math.round(bilerp((p00 >> 8 & 0xFF) * pm00, (p10 >> 8 & 0xFF) * pm10,
				(p01 >> 8 & 0xFF) * pm01, (p11 >> 8 & 0xFF) * pm11, tx, ty) / (outA / 255.0)));
		int outB = clamp255((int) Math.round(
				bilerp((p00 & 0xFF) * pm00, (p10 & 0xFF) * pm10, (p01 & 0xFF) * pm01, (p11 & 0xFF) * pm11, tx, ty)
						/ (outA / 255.0)));
		return (outA << 24) | (outR << 16) | (outG << 8) | outB;
	}

	private static double bilerp(double v00, double v10, double v01, double v11, double tx, double ty) {
		return v00 * (1 - tx) * (1 - ty) + v10 * tx * (1 - ty) + v01 * (1 - tx) * ty + v11 * tx * ty;
	}

	private static int clamp255(int v) {
		return v < 0 ? 0 : (v > 255 ? 255 : v);
	}

	private static int parseArgbColor(String spec, int defaultArgb) {
		if (!StringUtils.hasLength(spec))
			return defaultArgb;
		try {
			String css = resolveSpecColor(spec);
			javafx.scene.paint.Color c = javafx.scene.paint.Color.web(css);
			int a = (int) Math.round(c.getOpacity() * 255);
			int r = (int) Math.round(c.getRed() * 255);
			int g = (int) Math.round(c.getGreen() * 255);
			int b = (int) Math.round(c.getBlue() * 255);
			return (a << 24) | (r << 16) | (g << 8) | b;
		} catch (Exception e) {
			log.warn("Invalid color '{}': {}", spec, e.getMessage());
			return defaultArgb;
		}
	}

	private static double parseDouble(String s, double defaultVal) {
		if (s == null || s.isBlank())
			return defaultVal;
		try {
			return evalNum(s.trim());
		} catch (Exception e) {
			log.warn("parseDouble: failed to evaluate '{}': {}", s, e.getMessage());
			return defaultVal;
		}
	}

	/** Null-safe positional access into a split-parts array. */
	private static String get(String[] parts, int index) {
		return index < parts.length ? parts[index] : null;
	}

}
