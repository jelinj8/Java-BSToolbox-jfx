package cz.bliksoft.javautils.fx.controls.images;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;

public final class FxIconConverters {

	private FxIconConverters() {
	}

	// Cache by spec string (safe if icons are immutable / you don't restyle
	// per-control)
	private static final Map<String, Node> NODE_CACHE = new ConcurrentHashMap<>();

	/**
	 * Convert your ImageUtils icon spec into a JavaFX Node.
	 *
	 * @param spec icon spec understood by ImageUtils (e.g. "base/OK",
	 *             "x.png#overlay/y.png", "...svg;w;h;scale;css") OR raw SVGPath
	 *             content (e.g. "M10 10 L20 20 ...")
	 * @param fit  if > 0, ImageView will be sized to fit (preserve ratio).
	 */
	public static Node toNode(String spec, double fit) {
		if (spec == null || spec.isBlank())
			return null;

		// optional cache
		String cacheKey = spec + "|fit=" + fit;
		return NODE_CACHE.computeIfAbsent(cacheKey, k -> createNode(spec, fit));
	}

	private static Node createNode(String spec, double fit) {
		// Heuristic: if it looks like SVG path data, create SVGPath directly.
		// (Your ImageUtils also supports .svg files, but those become raster images via
		// SvgConverter.)
		if (looksLikeSvgPath(spec)) {
			SVGPath p = new SVGPath();
			p.setContent(spec);
			p.getStyleClass().add("icon-svg");
			return p;
		}

		Image awt = ImageUtils.getImageIfPossible(spec);
		if (awt == null)
			return null;

		BufferedImage bi = toBufferedImage(awt);
		javafx.scene.image.Image fxImg = SwingFXUtils.toFXImage(bi, null);

		ImageView iv = new ImageView(fxImg);
		iv.setPreserveRatio(true);
		iv.setSmooth(true);

		if (fit > 0) {
			iv.setFitWidth(fit);
			iv.setFitHeight(fit);
		}

		iv.getStyleClass().add("icon-image");
		return iv;
	}

	private static boolean looksLikeSvgPath(String s) {
		// Common SVGPath starts: M/m (move), sometimes begins with whitespace
		String t = s.stripLeading();
		return !t.isEmpty() && (t.charAt(0) == 'M' || t.charAt(0) == 'm');
	}

	private static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage bi)
			return bi;

		int w = Math.max(1, img.getWidth(null));
		int h = Math.max(1, img.getHeight(null));

		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		try {
			g.drawImage(img, 0, 0, null);
		} finally {
			g.dispose();
		}
		return bi;
	}
}
