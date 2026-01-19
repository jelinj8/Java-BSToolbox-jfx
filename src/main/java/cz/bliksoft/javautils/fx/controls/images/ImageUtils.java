package cz.bliksoft.javautils.fx.controls.images;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.fx.controls.images.svg.SvgConverter;

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

	protected static Image createImage(String path) {
		if (path == null) {
			log.debug("NULL image path requested");
			return null;
		}
		if (path.contains("#")) { //$NON-NLS-1$
			String[] images = path.split("#"); //$NON-NLS-1$
			return overlayImages(ALIGN_BOTTOM_RIGHT, images);
		} else {
			String[] params = path.split(";");
			String filePath = params[0];
			if (filePath.toLowerCase().endsWith(".svg")) {
				Float w = null;
				Float h = null;
				Float scale = null;
				String css = null;
				if (params.length > 1) {
					if (StringUtils.hasLength(params[1])) {
						w = Float.valueOf(params[1]);
					}
					if (params.length > 2) {
						if (StringUtils.hasLength(params[2])) {
							h = Float.valueOf(params[2]);
						}
					}
					if (params.length > 3) {
						if (StringUtils.hasLength(params[3])) {
							scale = Float.valueOf(params[3]);
						}
						if (params.length > 4) {
							if (StringUtils.hasLength(params[4])) {
								css = params[4];
							}
						}
					}
				}
				if (filePath.startsWith("[F]:")) {
					File f = new File(filePath.substring(4));
					if (f.exists()) {
						try {
							return SvgConverter.createImageFromSVG(f, w, h, scale, css);
						} catch (Exception e) {
							log.error("Failed to load SVG image.", e);
						}
					}
				} else {
					String URI = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath); //$NON-NLS-1$
					try {
						return SvgConverter.createImageFromSVGResource(URI, w, h, scale, css);
					} catch (Exception e) {
						log.error("Failed to load SVG image.", e);
					}
				}
			} else {
				if (filePath.startsWith("[F]:")) {
					File f = new File(filePath.substring(4));
					if (f.exists() && f.isFile())
						return new ImageIcon(filePath.substring(4), null).getImage();
				} else {
					java.net.URL imgURL = ImageUtils.class
							.getResource(filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath)); //$NON-NLS-1$
					if (imgURL != null)
						return new ImageIcon(imgURL, null).getImage();
				}
			}

//			if (log.isTraceEnabled())
//				log.warn(StackUtils.getStackTrace((Messages.getString("ImageUtils.0", path)), //$NON-NLS-1$
//						"cz.bliksoft.framework.core.branding")); //$NON-NLS-1$
//			else
//				log.warn(StackUtils.getStackTrace(Messages.getString("ImageUtils.0", path), //$NON-NLS-1$
//						1, 1, "cz.bliksoft.framework.core.branding")); //$NON-NLS-1$

			return null;
		}
	}

	public static Image getImageIfPossible(String path) {
		if (path == null)
			return null;

		Image i = iconCache.get(path);
		if (i == null) {
			i = createImage(path);
			if (i != null)
				iconCache.put(path, i);
		}
		return i;
	}

	public static Image getImage(String path) {
		Image i = getImageIfPossible(path);
		if (i == null) {
			i = getImage("File_16.png#overlay/Error_9.png"); //$NON-NLS-1$
			iconCache.put(path, i);
		}
		return i;
	}

	public static Image getEmptyImage() {
		return getImage("overlay/empty16.png");
	}

	public static Image getScaledSvgIcon(String iconName, Integer size) {
		if (size == null)
			return getImage(MessageFormat.format("{0}.svg;;{1};{2}", iconName, size, scale)); // $NON-NLS-3$
		else
			return getImage(MessageFormat.format("{0}.svg;;;{1}", iconName, scale)); // $NON-NLS-3$
	}

	public static Image getSvgIcon(String iconName, Integer size) {
		return getImage(MessageFormat.format("{0}.svg;;{1}", iconName, size)); // $NON-NLS-3$
	}

	public static Image getImage(String iconNameBase, int size) {
		return getImage(iconNameBase + "_" + size + ".png"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static final Map<String, Image> iconCache = new HashMap<>();

	public static Image getImage(Object input) {
		if (input instanceof ImageIcon)
			return ((ImageIcon) input).getImage();
		if (input instanceof Image)
			return (Image) input;
		else if (input instanceof String) {
			return getImage((String) input);
		}
		return null;
	}

	public static final int ALIGN_CENTER = 0;
	public static final int ALIGN_BOTTOM_RIGHT = 1;
	public static final int ALIGN_TOP_LEFT = 2;
	public static final int ALIGN_BOTTOM_LEFT = 3;

	/**
	 * překryje ikony v zadaném pořadí, výsledkem je obdélník opsaný, ikony
	 * zarovnány podle parametru
	 * 
	 * @param align
	 * @param args
	 * @return
	 */
	public static Image overlayImages(int align, String... args) {
		Image[] icons = new Image[args.length];
		for (int i = 0; i < args.length; i++) {
			Image icon = (args[i] != null ? getImage(args[i]) : null);
			icons[i] = icon;
		}
		return overlayImages(align, icons);
	}

	/**
	 * překryje ikony v zadaném pořadí, výsledkem je obdélník opsaný, ikony
	 * zarovnány podle parametru
	 * 
	 * @param align
	 * @param icons
	 * @return
	 */
	public static Image overlayImages(int align, Image... icons) {
		int maxW = 0;
		int maxH = 0;
		int w;
		int h;
		for (Image icon : icons) {
			if (icon != null) {
				w = icon.getWidth(null);
				h = icon.getHeight(null);
				if (w > maxW) {
					maxW = w;
				}
				if (h > maxH) {
					maxH = h;
				}
			}
		}
		BufferedImage img = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = img.createGraphics();
		for (Image icon : icons) {
			if (icon == null) {
				continue;
			}
			int dx = 0;
			int dy = 0;
			w = icon.getWidth(null);
			h = icon.getHeight(null);

			switch (align) {
			case ALIGN_CENTER:
				dx = (maxW - w) / 2;
				dy = (maxH - h) / 2;
				break;
			case ALIGN_BOTTOM_RIGHT:
				dx = (maxW - w);
				dy = (maxH - h);
				break;
			case ALIGN_BOTTOM_LEFT:
				dx = 0;
				dy = (maxH - h);
				break;
			case ALIGN_TOP_LEFT:
				dx = 0;
				dy = 0;
				break;
			}
			graphics.drawImage(icon, dx, dy, null);
		}
//		ImageIcon result = new ImageIcon(img, null);
		return img;
	}

	public static java.net.URL getIconUrl(String iconPath) {
		return ImageUtils.class.getResource(iconPath);
	}

	/*-------------------- FIXME temporarily uncached! --------------------------*/

	public static ImageIcon getIcon(String iconName) {
		return new ImageIcon(getImage(iconName));
	}

	public static ImageIcon getIcon(String iconName, int iconSize) {
		return new ImageIcon(getImage(iconName, iconSize));
	}

	public static ImageIcon overlayIcons(int align, Image... icons) {
		return new ImageIcon(overlayImages(align, icons));
	}

	public static ImageIcon overlayIcons(int align, ImageIcon... icons) {
		Image[] imgs = new Image[icons.length];
		for (int i = 0; i < icons.length; i++)
			imgs[i] = icons[i].getImage();
		return new ImageIcon(overlayImages(align, imgs));
	}

	/**
	 * překryje ikony v zadaném pořadí, výsledkem je obdélník opsaný, ikony
	 * zarovnány podle parametru
	 * 
	 * @param align
	 * @param args
	 * @return
	 */
	public static ImageIcon overlayIcons(int align, String... args) {
		return new ImageIcon(overlayImages(align, args));
	}

	public static ImageIcon getEmptyImageIcon() {
		return new ImageIcon(getImage("overlay/empty16.png"));
	}

	public static ImageIcon getImageIconIfPossible(String path) {
		return new ImageIcon(getImageIfPossible(path));
	}

	public static ImageIcon getIcon(Object input) {
		if (input instanceof ImageIcon)
			return ((ImageIcon) input);
		if (input instanceof Image)
			return new ImageIcon((Image) input);
		else if (input instanceof String) {
			return getIcon((String) input);
		}
		return null;
	}
}
