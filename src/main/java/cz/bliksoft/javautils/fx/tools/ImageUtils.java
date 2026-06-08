package cz.bliksoft.javautils.fx.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.ui.UiScale;
import cz.bliksoft.javautils.images.PixelOps;
import cz.bliksoft.javautils.images.ico.IcoWriter;
import cz.bliksoft.javautils.images.iconspec.IconSpecEngine;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/**
 * Utility for loading, caching, and composing JavaFX {@link Image} objects from
 * various sources. Spec resolution itself — parsing, postfix composition,
 * pixel-level filters, SVG/QR/ICO rendering, {@code *TEXT}/{@code *DRAW}, … —
 * is delegated to the toolkit-agnostic {@link IconSpecEngine} in the base
 * library; this class adds only the genuinely JavaFX-specific layer on top:
 * {@link Image} results (converted from the engine's {@link BufferedImage}),
 * scene-graph node creation ({@link ImageView},
 * {@link javafx.scene.shape.SVGPath}/ {@link javafx.scene.shape.Shape} for
 * {@code [P]:}/{@code [PS]:} specs), UI-scale-aware token substitution, and an
 * {@code Image}-keyed result cache.
 *
 * Images are resolved via an <em>icon spec string</em> with the following
 * supported formats:
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
 * {@code *EMPTY|w|h|color}; {@code *META|key|value} — sticky metadata
 * side-channel readable via {@link IconSpecEngine#getLastMetadata()};
 * {@code *JFXSTYLE|css} — sugar for {@code *META|jfxStyle|css}, consumed by
 * {@link #getIconView} to style the resulting {@link ImageView};
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

	/**
	 * Sets the classpath root prefix used when resolving relative image names.
	 * Delegates to {@link IconSpecEngine#setBrandingImagesRoot(String)}.
	 *
	 * @param path the new root prefix (must end with {@code /})
	 */
	public static void setBrandingImagesRoot(String path) {
		IconSpecEngine.setBrandingImagesRoot(path);
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
	 * Thread-local storage for the last {@code *JFXSTYLE}/{@code *META|jfxStyle|…}
	 * value encountered during postfix evaluation (republished from
	 * {@link IconSpecEngine#getLastMetadata()} by {@link #createImage}).
	 */
	private static final ThreadLocal<String> jfxStyleTL = new ThreadLocal<>();

	/**
	 * Set when the engine reports {@code *NOCACHE} for the most recent evaluation
	 * (republished from {@link IconSpecEngine#wasNoCacheRequested()} by
	 * {@link #createImage}), to suppress storage in {@link #iconCache}.
	 */
	private static final ThreadLocal<Boolean> noCacheTL = new ThreadLocal<>();

	/** Overlay alignment: source image is centered over the base. */
	public static final int ALIGN_CENTER = IconSpecEngine.ALIGN_CENTER;
	/**
	 * Overlay alignment: source image is placed at the bottom-right corner (default
	 * for badge overlays).
	 */
	public static final int ALIGN_BOTTOM_RIGHT = IconSpecEngine.ALIGN_BOTTOM_RIGHT;
	/** Overlay alignment: source image is placed at the top-left corner. */
	public static final int ALIGN_TOP_LEFT = IconSpecEngine.ALIGN_TOP_LEFT;
	/** Overlay alignment: source image is placed at the bottom-left corner. */
	public static final int ALIGN_BOTTOM_LEFT = IconSpecEngine.ALIGN_BOTTOM_LEFT;
	/** Overlay alignment: source image is placed at the top-right corner. */
	public static final int ALIGN_TOP_RIGHT = IconSpecEngine.ALIGN_TOP_RIGHT;

	/**
	 * Alignment: forces {@code *TEXT} to push a new standalone image without
	 * compositing onto the existing stack canvas.
	 */
	public static final int ALIGN_NEW = IconSpecEngine.ALIGN_NEW;

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
	 * substitution). Spec resolution is delegated to {@link IconSpecEngine}; this
	 * method converts the resulting {@link BufferedImage} to a JavaFX {@link Image}
	 * and republishes the engine's per-evaluation thread-local state
	 * ({@code *NOCACHE}, {@code *JFXSTYLE}/{@code *META|jfxStyle|…}) through
	 * {@link #noCacheTL}/{@link #jfxStyleTL} so {@link #getImageIfPossible}'s
	 * caching keeps working unchanged.
	 *
	 * @param spec       the icon spec string; may be {@code null}
	 * @param background ignored — kept for backward compatibility; the underlying
	 *                   engine always resolves images synchronously
	 *
	 * @return the composed image, or {@code null} if the spec is {@code null} or
	 *         loading fails
	 */
	protected static Image createImage(String spec, boolean background) {
		if (spec == null) {
			log.debug("NULL image spec requested");
			return null;
		}

		BufferedImage bi = IconSpecEngine.createImage(spec);

		noCacheTL.remove();
		if (IconSpecEngine.wasNoCacheRequested())
			noCacheTL.set(Boolean.TRUE);

		jfxStyleTL.remove();
		String style = IconSpecEngine.getLastMetadata().get("jfxStyle"); //$NON-NLS-1$
		if (style != null)
			jfxStyleTL.set(style);

		return (bi != null) ? SwingFXUtils.toFXImage(bi, null) : null;
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

		int[] out = new int[maxW * maxH];

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

			int[] src = readPixels(pr, w, h);
			boolean doSubtract = i > 0 && subtracts != null && i < subtracts.length && subtracts[i];
			if (doSubtract) {
				PixelOps.alphaSubtractInto(out, maxW, maxH, src, w, h, dx, dy);
			} else {
				PixelOps.alphaCompositeInto(out, maxW, maxH, src, w, h, dx, dy);
			}
		}

		return writePixels(out, maxW, maxH);
	}

	/**
	 * Reads a row-major ARGB pixel buffer from an image region (the
	 * {@link PixelOps} representation).
	 */
	private static int[] readPixels(PixelReader pr, int w, int h) {
		int[] pixels = new int[w * h];
		pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
		return pixels;
	}

	/**
	 * Writes a row-major ARGB pixel buffer (the {@link PixelOps} representation)
	 * into a new image.
	 */
	private static WritableImage writePixels(int[] pixels, int w, int h) {
		WritableImage out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
		return out;
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

		if (spec.startsWith(IconSpecEngine.PREFIX_PATH)) {
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

		if (spec.startsWith(IconSpecEngine.PREFIX_PATH_SHAPE)) {
			IconSpecEngine.PathSpec ps = IconSpecEngine.parsePathSpec(spec, IconSpecEngine.PREFIX_PATH_SHAPE);
			return createSvgPathShape(ps);
		} else if (spec.startsWith(IconSpecEngine.PREFIX_PATH)) {
			IconSpecEngine.PathSpec ps = IconSpecEngine.parsePathSpec(spec, IconSpecEngine.PREFIX_PATH);
			return createSvgPathNode(ps);
		}

		// default: image-based => ImageView
		return getIconView(spec);
	}

	private static javafx.scene.shape.SVGPath createSvgPathNode(IconSpecEngine.PathSpec ps) {
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

		Bounds b = node.getBoundsInLocal();
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

	private static javafx.scene.shape.Shape createSvgPathShape(IconSpecEngine.PathSpec ps) {
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

}
