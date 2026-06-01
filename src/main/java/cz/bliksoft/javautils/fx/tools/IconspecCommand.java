package cz.bliksoft.javautils.fx.tools;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration of all postfix commands supported in ImageUtils icon spec
 * strings. Each constant carries the literal command string (as it appears
 * after the leading {@code *} in the spec) and the names of its positional
 * parameters.
 *
 * <p>
 * Parameters are positional: only slots up to the last specified one need to be
 * present in the spec. An empty slot is represented by an empty string between
 * {@code |} separators, e.g. {@code *TEXT||red} sets color without a value.
 *
 * <p>
 * Neither {@code |} nor {@code #} characters are valid inside parameter values.
 */
public enum IconspecCommand {

	// ── Composition ──────────────────────────────────────────────────────────

	/**
	 * Pops two images off the stack and composes them (SRC_OVER blending). Optional
	 * {@code canvasMode}: {@code E} = extend canvas to union bounding box,
	 * {@code C} = crop result to base image size (default).
	 */
	COMPOSE_OVER("+", "canvasMode"),

	/**
	 * Pops two images off the stack and composes them (DST_OUT blending). Optional
	 * {@code canvasMode}: {@code E} = extend canvas to union bounding box,
	 * {@code C} = crop result to base image size (default).
	 */
	COMPOSE_OUT("-", "canvasMode"),

	// ── Anchor / alignment ───────────────────────────────────────────────────

	/**
	 * Sets the anchor position (and optional pixel offsets) used by {@code *+},
	 * {@code *-}, {@code *CROP}, {@code *DRAW}, and {@code *TEXT}. Replaces the
	 * former individual {@code *TL} / {@code *TR} / {@code *BL} / {@code *BR} /
	 * {@code *C} commands.
	 *
	 * <p>
	 * {@code position} values: {@code TL} top-left, {@code TR} top-right,
	 * {@code BL} bottom-left, {@code BR} bottom-right (default), {@code C} centre,
	 * {@code N} new — forces {@code *TEXT} to push a standalone image without
	 * compositing onto the existing stack canvas.
	 */
	ANCHOR("ANCHOR", "position", "offsetX", "offsetY"),

	// ── Canvas / stack ops ───────────────────────────────────────────────────

	/**
	 * Creates an empty (transparent or colored) canvas and pushes it onto the
	 * stack.
	 */
	EMPTY("EMPTY", "width", "height", "color"),

	/** Pushes a copy of the top-of-stack image (top remains). */
	PUSH("PUSH"),

	/** Copies the top-of-stack image to a temporary buffer. */
	COPY("COPY"),

	/** Pastes the buffered image back onto the stack. */
	PASTE("PASTE"),

	/** Removes (discards) the top-of-stack image. */
	POP("POP"),

	/** Swaps the two topmost images on the stack. */
	SWAP("SWAP"),

	/** Resets the mode map (alignment, offsets, text settings). */
	RESET("RESET"),

	// ── Transforms ───────────────────────────────────────────────────────────

	// ── Filters ──────────────────────────────────────────────────────────────

	/**
	 * Applies a pixel-level filter to the top-of-stack image and replaces it with
	 * the result. The second token is the filter name; subsequent tokens are
	 * filter-specific parameters. See {@link ImageFilter} for the full list.
	 * Example: {@code *FILTER|shadow|000000|6}
	 */
	FILTER("FILTER", "filter", "p1", "p2", "p3"),

	// ── Color / drawing ──────────────────────────────────────────────────────

	/**
	 * Sets sticky drawing colors and stroke width. All parameters are optional
	 * (blank = no change). Use {@code none} to disable a paint (no fill or no
	 * stroke). Colors are in iconspec format: bare hex ({@code ff0000}),
	 * {@code 0xRRGGBB}, or CSS named colors.
	 */
	COLOR("COLOR", "stroke", "fill", "width"),

	/**
	 * Draws a primitive shape onto the top-of-stack canvas using the current COLOR
	 * fill and stroke. The optional last parameter {@code t} overrides stroke width
	 * for this call only. Supported shapes:
	 * <ul>
	 * <li>{@code line|x1|y1|x2|y2|t} — stroke only</li>
	 * <li>{@code circle|cx|cy|r|t} — center + radius</li>
	 * <li>{@code square|x|y|side|t} — top-left + side length</li>
	 * <li>{@code rectangle|x|y|w|h|t} — top-left + width + height</li>
	 * </ul>
	 */
	DRAW("DRAW", "shape", "p1", "p2", "p3", "p4", "t"),

	// ── Text ─────────────────────────────────────────────────────────────────

	/**
	 * Renders text using the current COLOR fill (text color) and stroke (outline
	 * when {@code O} flag is active). Text parameters are sticky. {@code style} is
	 * a set of flag letters: {@code B}=bold, {@code I}=italic, {@code U}=underline,
	 * {@code S}=strikethrough, {@code O}=outline (stroke from COLOR outside fill).
	 * Flags are merged into the current set: a {@code -} prefix removes subsequent
	 * flags ({@code -B} removes bold); {@code +} restores add mode; bare {@code -}
	 * clears all flags.
	 */
	TEXT("TEXT", "value", "size", "font", "style"),

	// ── Style ────────────────────────────────────────────────────────────────

	/**
	 * Sets an inline CSS style string applied to the
	 * {@link javafx.scene.image.ImageView} returned by
	 * {@link ImageUtils#getIconView}. This is <em>not</em> a CSS class name — it is
	 * passed directly to {@link javafx.scene.Node#setStyle(String)}.
	 */
	JFXSTYLE("JFXSTYLE", "cssStyle"),

	// ── Cache ─────────────────────────────────────────────────────────────────

	/**
	 * Stores a copy of the top-of-stack image under {@code key} in the icon cache.
	 * Has no effect on the stack or the rendered image.
	 */
	PUT_CACHE("PUT_CACHE", "key"),

	/**
	 * Retrieves a previously stored image from the cache by {@code key} and pushes
	 * it onto the stack. Handled inline in {@link ImageUtils#createImage} rather
	 * than in {@code executeCommand}. Has no effect on the preview in the composer.
	 */
	GET_CACHE("GET_CACHE", "key"),

	/**
	 * Removes the image stored under {@code key} from the icon cache. Has no effect
	 * on the stack or the rendered image.
	 */
	CLEAR_CACHE("CLEAR_CACHE", "key"),

	/**
	 * Suppresses writing the final composed image into the icon cache. Useful for
	 * one-off compositions or previews.
	 */
	NOCACHE("NOCACHE");

	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * The literal command string as it appears after {@code *} in the icon spec.
	 */
	public final String cmdString;

	/**
	 * Positional parameter names, in order. May be empty for zero-parameter
	 * commands.
	 */
	public final String[] paramNames;

	IconspecCommand(String cmdString, String... paramNames) {
		this.cmdString = cmdString;
		this.paramNames = paramNames;
	}

	// ── Lookup ───────────────────────────────────────────────────────────────

	private static final Map<String, IconspecCommand> BY_CMD = Arrays.stream(values())
			.collect(Collectors.toMap(c -> c.cmdString, Function.identity()));

	/**
	 * Returns the command for the given literal command string (the part after
	 * {@code *}), or {@code null} if not found.
	 */
	public static IconspecCommand fromCmdString(String cmd) {
		return BY_CMD.get(cmd);
	}
}
