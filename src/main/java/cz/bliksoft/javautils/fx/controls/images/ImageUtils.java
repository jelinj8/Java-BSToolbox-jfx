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

public class ImageUtils {
	private static final Logger log = LogManager.getLogger();

	private static String brandingImagesRoot = "/cz/bliksoft/branding/images/"; //$NON-NLS-1$

	public static void setBrandingImagesRoot(String path) {
		brandingImagesRoot = path;
	}

	private static float scale = 1f;

	public static void setScale(float scale) {
		ImageUtils.scale = scale;
	}

	public static float getScale() {
		return scale;
	}

	private static final Map<String, Image> iconCache = new HashMap<>();

	public static final int ALIGN_CENTER = 0;
	public static final int ALIGN_BOTTOM_RIGHT = 1;
	public static final int ALIGN_TOP_LEFT = 2;
	public static final int ALIGN_BOTTOM_LEFT = 3;

	public static final String SCALE_PLACEHOLDER = "${scale}";

	protected static Image createImage(String spec) {
		if (spec == null) {
			log.debug("NULL image path requested");
			return null;
		}

		// Overlay chain: "a#b#c"
		if (spec.contains("#")) { //$NON-NLS-1$
			String[] images = spec.split("#"); //$NON-NLS-1$
			return overlayImages(ALIGN_BOTTOM_RIGHT, images);
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
					return new Image(url.toExternalForm(), true);
				}
			}
		} catch (Exception e) {
			log.error("Failed to load raster image: {}", spec, e);
		}

		return null;
	}

	public static Image getImageIfPossible(String spec) {
		if (spec == null)
			return null;

		String nSpec;
		if (spec.contains(SCALE_PLACEHOLDER))
			nSpec = spec.replace(SCALE_PLACEHOLDER, UiScale.bucketedScaleString());
		else
			nSpec = spec;

		Image i = iconCache.get(nSpec);
		if (i == null) {
			i = createImage(nSpec);
			if (i != null) {
				iconCache.put(nSpec, i);
			}
		}
		return i;
	}

	public static Image getImage(String spec) {
		Image i = getImageIfPossible(spec);
		if (i == null) {
			// fallback kept from your original
			i = getImageIfPossible("File_16.png#overlay/Error_9.png"); //$NON-NLS-1$
			if (i != null) {
				iconCache.put(spec, i);
			}
		}
		return i;
	}

	public static Image getEmptyImage() {
		return getImage("overlay/empty16.png");
	}

	public static Image getScaledSvgIcon(String iconName, Integer size) {
		// kept same formatting logic (even though the original looked swapped)
		if (size == null) {
			return getImage(MessageFormat.format("{0}.svg||{1}|{2}", iconName, size, scale)); // $NON-NLS-3$
		} else {
			return getImage(MessageFormat.format("{0}.svg|||{1}", iconName, scale)); // $NON-NLS-3$
		}
	}

	public static Image getSvgIcon(String iconName, Integer size) {
		return getImage(MessageFormat.format("{0}.svg||{1}", iconName, size)); // $NON-NLS-3$
	}

	public static Image getImage(String iconNameBase, int size) {
		return getImage(iconNameBase + "_" + size + ".png"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static Image getImage(Object input) {
		if (input instanceof Image img)
			return img;
		if (input instanceof ImageView iv)
			return iv.getImage();
		if (input instanceof String s)
			return getImage(s);
		return null;
	}

	// ---- Overlay ----

	public static Image overlayImages(int align, String... args) {
		Image[] icons = new Image[args.length];
		for (int i = 0; i < args.length; i++) {
			icons[i] = (args[i] != null ? getImage(args[i]) : null);
		}
		return overlayImages(align, icons);
	}

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

	public static java.net.URL getIconUrl(String iconPath) {
		return ImageUtils.class.getResource(iconPath);
	}

	public static ImageView overlayIconViews(int align, Image... icons) {
		Image img = overlayImages(align, icons);
		return (img != null ? new ImageView(img) : null);
	}

	public static ImageView overlayIconViews(int align, String... args) {
		Image img = overlayImages(align, args);
		return (img != null ? new ImageView(img) : null);
	}

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

	public static ImageView getIconView(Object input) {
		if (input instanceof Image)
			return new ImageView((Image) input);
		else if (input instanceof String) {
			return getIconView((String) input);
		}
		return null;
	}

	public static ImageView getIconView(String spec) {
		return getIconView(spec, (String) null);
	}

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

	public static ImageView getIconView(String spec, double size) {
		return getIconView(spec, size, null);
	}

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
	 * Generic icon loader: - [P]:... -> returns SVGPath Node (styled + scaled) -
	 * otherwise -> returns ImageView (optionally styled via svg style param)
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
