package cz.bliksoft.javautils.fx.tools;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration of filter sub-commands supported by the {@code *FILTER} postfix
 * command in {@link ImageUtils} icon spec strings.
 *
 * <p>
 * Usage in a spec string: {@code *FILTER|<name>|<p1>|<p2>|...}
 *
 * <p>
 * Filters operate on the top-of-stack image and replace it with the filtered
 * result.
 *
 * <table>
 * <caption>Filter reference</caption>
 * <tr>
 * <th>Name</th>
 * <th>Parameters</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@code shadow}</td>
 * <td>{@code color}, {@code width}, {@code fill}</td>
 * <td>Diffuse fade-out shadow in {@code color} extending {@code width} pixels
 * beyond the image silhouette. {@code fill}: {@code F} = filled interior
 * (default), {@code T} = transparent holes preserved.</td>
 * </tr>
 * <tr>
 * <td>{@code outline}</td>
 * <td>{@code color}, {@code width}, {@code fill}</td>
 * <td>Sharp (non-blurred) silhouette expansion. Same as shadow but without the
 * Gaussian falloff. {@code fill}: {@code F} = filled (default), {@code T} =
 * transparent.</td>
 * </tr>
 * <tr>
 * <td>{@code rotate}</td>
 * <td>{@code angle}</td>
 * <td>Clockwise rotation by {@code angle} degrees around canvas centre; canvas
 * size is unchanged. Multiples of 90° use lossless pixel mapping; other angles
 * use bilinear interpolation.</td>
 * </tr>
 * <tr>
 * <td>{@code shift}</td>
 * <td>{@code angle}, {@code pixels}</td>
 * <td>Translation in direction {@code angle} (0°=right, 90°=down) by
 * {@code pixels} pixels. Multiples of 90° with whole-pixel counts are lossless;
 * others use bilinear interpolation. Canvas size is unchanged.</td>
 * </tr>
 * <tr>
 * <td>{@code scale}</td>
 * <td>{@code w}, {@code h}, {@code mode}</td>
 * <td>Scale image. {@code mode} {@code F}=fit, {@code C}=crop/fill,
 * {@code %}=percent (first arg). Omit one dimension to keep aspect ratio.</td>
 * </tr>
 * <tr>
 * <td>{@code resize}</td>
 * <td>{@code w}, {@code h}</td>
 * <td>Change canvas size using current alignment/offset mode; content is not
 * scaled. Replaces {@code *CROP} (which remains for backward
 * compatibility).</td>
 * </tr>
 * <tr>
 * <td>{@code mask}</td>
 * <td>{@code color}, {@code invert}</td>
 * <td>Replace all pixel colours with {@code color} (default white) keeping the
 * original alpha. If {@code color} is semi-transparent its alpha acts as
 * maximum opacity. {@code invert}: {@code N} = normal (default), {@code Y} =
 * invert alpha.</td>
 * </tr>
 * <tr>
 * <td>{@code monochrome}</td>
 * <td>{@code color}</td>
 * <td>Convert to monochrome by replacing each pixel's colour with {@code color}
 * (default white) weighted by the pixel's luminance.</td>
 * </tr>
 * <tr>
 * <td>{@code keymask}</td>
 * <td>{@code color}</td>
 * <td>Replace a specific colour (or, if omitted, the colour that is the same in
 * the majority of canvas corners) with full transparency.</td>
 * </tr>
 * <tr>
 * <td>{@code mirror}</td>
 * <td>{@code direction}</td>
 * <td>Flip the image. {@code direction}: {@code H} = horizontal/left-right
 * (default), {@code V} = vertical/top-bottom.</td>
 * </tr>
 * </table>
 */
public enum ImageFilter {

	/** Diffuse glow/shadow: dilate + Gaussian-blur alpha, solid interior. */
	SHADOW("shadow", "color", "width", "fill"),

	/** Sharp silhouette expansion: dilate alpha only, no blur. */
	OUTLINE("outline", "color", "width", "fill"),

	/** Clockwise rotation around centre; canvas size unchanged. */
	ROTATE("rotate", "angle"),

	/** Translation in given direction; canvas size unchanged. */
	SHIFT("shift", "angle", "pixels"),

	/** Scale image; supports fit, crop, percent, and proportional modes. */
	SCALE("scale", "w", "h", "mode"),

	/** Canvas resize using current alignment; content is not scaled. */
	RESIZE("resize", "w", "h"),

	/** Flatten to a single colour using original alpha channel. */
	MASK("mask", "color", "invert"),

	/**
	 * Convert to monochrome using luminance; optionally tint with a base colour.
	 */
	MONOCHROME("monochrome", "color"),

	/** Replace a specific (or auto-detected corner) colour with transparency. */
	KEYMASK("keymask", "color"),

	/** Flip image horizontally ({@code H}, default) or vertically ({@code V}). */
	MIRROR("mirror", "direction");

	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * The literal filter name as it appears after {@code *FILTER|} in the icon
	 * spec.
	 */
	public final String filterName;

	/** Positional parameter names, in order. */
	public final String[] paramNames;

	ImageFilter(String filterName, String... paramNames) {
		this.filterName = filterName;
		this.paramNames = paramNames;
	}

	// ── Lookup ───────────────────────────────────────────────────────────────

	private static final Map<String, ImageFilter> BY_NAME = Arrays.stream(values())
			.collect(Collectors.toMap(f -> f.filterName, Function.identity()));

	/**
	 * Returns the filter for the given name string (case-sensitive), or
	 * {@code null} if unknown.
	 */
	public static ImageFilter fromName(String name) {
		return BY_NAME.get(name);
	}
}
