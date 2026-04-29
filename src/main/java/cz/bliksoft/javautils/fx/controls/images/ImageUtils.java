package cz.bliksoft.javautils.fx.controls.images;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.app.ui.UiScale;
import cz.bliksoft.javautils.fx.controls.images.svg.SvgConverter;
import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.SVGPath;

/**
 * Utility for loading, caching, and compositing JavaFX {@link Image} objects
 * from various sources. Images are resolved via an <em>icon spec string</em>
 * with the following supported formats:
 *
 * <ul>
 * <li>{@code name.png} / {@code name.svg} — resolved relative to the branding
 * images root (default: {@code /cz/bliksoft/branding/images/})</li>
 * <li>{@code /absolute/path.png} — absolute classpath resource</li>
 * <li>{@code [F]:/filesystem/path.png} — explicit file-system path</li>
 * <li>{@code name.svg|w|h|scale|style} — SVG with optional pixel width, height,
 * extra scale multiplier, and inline CSS style</li>
 * <li>{@code [P]:svgPathData|w|h|scale|style} — inline SVG path data rendered
 * as a styled {@link javafx.scene.shape.SVGPath} node; use {@link #getIconNode}
 * to retrieve it</li>
 * <li>{@code [PI]:svgPathData|w|h|scale|style} — inline SVG path rasterized to
 * an {@link Image}</li>
 * <li>{@code [PS]:svgPathData|w|h|scale|style} — inline SVG path as a
 * layoutable {@link javafx.scene.shape.Shape} node; use {@link #getIconNode} to
 * retrieve it</li>
 * <li>{@code base#overlay#...} — overlay chain: images composited in order with
 * bottom-right alignment by default; an optional alignment token ({@code TL},
 * {@code TR}, {@code BL}, {@code BR}, {@code C}) as the first
 * {@code #}-separated element overrides the alignment for that chain</li>
 * <li>{@code step1##step2##...} — processing chain: each {@code ##}-separated
 * segment is resolved independently via {@code createImage} (so a segment may
 * itself be an overlay chain with its own alignment token), and the resulting
 * images are then composited in a single pass; an optional alignment token as
 * the very first {@code ##}-part controls the composite alignment (default
 * bottom-right)</li>
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
	 * Placeholder token in icon spec strings that is replaced with the current
	 * UI-scale bucket string before image lookup.
	 */
	public static final String SCALE_PLACEHOLDER = "${scale}";

	private static Integer parseAlignToken(String s) {
		return switch (s) {
		case "TL" -> ALIGN_TOP_LEFT; //$NON-NLS-1$
		case "TR" -> ALIGN_TOP_RIGHT; //$NON-NLS-1$
		case "BL" -> ALIGN_BOTTOM_LEFT; //$NON-NLS-1$
		case "BR" -> ALIGN_BOTTOM_RIGHT; //$NON-NLS-1$
		case "C" -> ALIGN_CENTER; //$NON-NLS-1$
		default -> null;
		};
	}

	/**
	 * Creates an image from a raw spec string (no cache look-up; no scale
	 * substitution). Handles overlay chains, inline SVG paths, SVG files, and
	 * raster images.
	 *
	 * @param spec       the icon spec string; may be {@code null}
	 * @param background if {@code true}, raster images are loaded asynchronously in
	 *                   the background
	 *
	 * @return the loaded image, or {@code null} if the spec is {@code null} or
	 *         loading fails
	 */
	protected static Image createImage(String spec, boolean background) {
		if (spec == null) {
			log.debug("NULL image spec requested");
			return null;
		}

		// Processing chain: "step1##step2##..." — each segment resolved via
		// createImage, then composited once
		if (spec.contains("##")) { //$NON-NLS-1$
			String[] parts = spec.split("##", -1); //$NON-NLS-1$
			int align = ALIGN_BOTTOM_RIGHT;
			int startIdx = 0;
			Integer tokenAlign = parseAlignToken(parts[0]);
			if (tokenAlign != null) {
				align = tokenAlign;
				startIdx = 1;
			}
			Image[] images = new Image[parts.length - startIdx];
			for (int i = 0; i < images.length; i++) {
				images[i] = createImage(parts[startIdx + i], background);
			}
			return overlayImages(align, images);
		}

		// Overlay chain: "a#b#c" — optional leading alignment token (TL/TR/BL/BR/C)
		if (spec.contains("#")) { //$NON-NLS-1$
			String[] parts = spec.split("#", -1); //$NON-NLS-1$
			int align = ALIGN_BOTTOM_RIGHT;
			int startIdx = 0;
			Integer tokenAlign = parseAlignToken(parts[0]);
			if (tokenAlign != null) {
				align = tokenAlign;
				startIdx = 1;
			}
			String[] imageParts = java.util.Arrays.copyOfRange(parts, startIdx, parts.length);
			return overlayImages(align, imageParts);
		}

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

		// Param syntax: "file.svg;w;h;scale;css"
		String[] params = spec.split("\\|", -1);
		String filePath = params[0];

		// SVG handling
		if (filePath.toLowerCase().endsWith(".svg")) {
			Float w = null;
			Float h = null;
			Float svgScale = null;

			if (params.length > 1 && StringUtils.hasLength(params[1]))
				w = Float.valueOf(params[1]);
			if (params.length > 2 && StringUtils.hasLength(params[2]))
				h = Float.valueOf(params[2]);
			if (params.length > 3 && StringUtils.hasLength(params[3]))
				svgScale = Float.valueOf(params[3]);

			try {
				if (filePath.startsWith(PREFIX_FILE)) {
					File f = new File(filePath.substring(4));
					if (f.exists() && f.isFile()) {
						return SvgConverter.createImageFromSVG(f, w, h, svgScale);
					}
					return null;
				}

				String res = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath);
				return SvgConverter.createImageFromSVGResource(res, w, h, svgScale);

			} catch (Exception e) {
				log.error("Failed to load SVG image: {}", spec, e);
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
				return null;
			} else {
				String res = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath); //$NON-NLS-1$
				var url = ImageUtils.class.getResource(res);
				if (url != null) {
					return new Image(url.toExternalForm(), background);
				}
			}
		} catch (Exception e) {
			log.error("Failed to load raster image: {}", spec, e);
		}

		return null;
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

		String nSpec;
		if (spec.contains(SCALE_PLACEHOLDER))
			nSpec = spec.replace(SCALE_PLACEHOLDER, UiScale.bucketedScaleString());
		else
			nSpec = spec;

		Image i = iconCache.get(nSpec);
		if (i == null) {
			i = createImage(nSpec, background);
			if (i != null) {
				iconCache.put(nSpec, i);
			}
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
			// fallback kept from your original
			i = getImageIfPossible("File_16.png#overlay/Error_9.png", false); //$NON-NLS-1$
			if (i != null) {
				iconCache.put(spec, i);
			}
		}
		return i;
	}

	/**
	 * Returns the standard 16×16 transparent placeholder image.
	 *
	 * @return the empty/transparent image, or {@code null} if unavailable
	 */
	public static Image getEmptyImage() {
		return getImage("overlay/empty16.png");
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
	public static Image getScaledSvgIcon(String iconName, Integer size) {
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
	public static Image getSvgIcon(String iconName, Integer size) {
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
			icons[i] = (args[i] != null ? getImage(args[i]) : null);
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

		for (Image icon : icons) {
			if (icon == null)
				continue;
			PixelReader pr = icon.getPixelReader();
			if (pr == null)
				continue;

			int w = (int) Math.ceil(icon.getWidth());
			int h = (int) Math.ceil(icon.getHeight());

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

			alphaCompositeInto(out, pr, dx, dy, w, h);
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
	 * Returns the classpath URL for the given icon resource path, or {@code null}
	 * if not found.
	 *
	 * @param iconPath absolute classpath resource path (e.g.
	 *                 {@code "/icons/save.png"})
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
		Image img = getEmptyImage();
		return (img != null ? new ImageView(img) : null);
	}

	private static String extractViewStyle(String spec) {
		if (spec == null)
			return null;

		String[] p = spec.split("\\|", -1);
		// For svg file specs only: "...svg|w|h|scale|style"
		if (p.length >= 5 && p[0].toLowerCase().endsWith(".svg")) {
			return p[4];
		}
		return null;
	}

	private static String stripViewStyleFromSpec(String spec) {
		if (spec == null)
			return null;

		String[] p = spec.split("\\|", -1);
		if (p.length >= 5 && p[0].toLowerCase().endsWith(".svg")) {
			// keep only first 4 parts for image caching/loading
			return p[0] + "|" + p[1] + "|" + p[2] + "|" + p[3];
		}
		return spec;
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
			// [P]: is not representable as ImageView without rasterizing;
			// caller should use getIconNode().
			throw new IllegalArgumentException("[P]: path specs return SVGPath Node; use getIconNode(spec) instead.");
		}

		String baseSpec = stripViewStyleFromSpec(spec);
		Image img = getImage(baseSpec);
		if (img == null)
			return null;

		ImageView iv = new ImageView(img);

		String s = (style != null) ? style : extractViewStyle(spec);
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

}
