# ImageUtils

`cz.bliksoft.javautils.fx.tools.ImageUtils`

Central utility for loading, compositing, and caching JavaFX `Image` objects and icon nodes. All results are cached by the resolved spec string. Images are described by a compact string format called an *icon spec*.

---

## Quick start

```java
// Image; falls back to an error-indicator icon on failure
Image img = ImageUtils.getImage("save_16.png", false);

// ImageView, possibly sized
ImageView iv = ImageUtils.getIconView("save_16.png");
ImageView iv = ImageUtils.getIconView("save.svg", 24.0);

// Polymorphic: accepts Image, ImageView, or String spec
Node icon = ImageUtils.getIconNode("save.svg|24|24|1.0|");   // ImageView
Node icon = ImageUtils.getIconNode("[P]:M0 0 L10 10|16|16"); // SVGPath node
```

---

## Icon spec formats

All methods that accept a `String spec` understand the following formats.

### Raster images

| Format | Example |
|---|---|
| Relative name | `save_16.png` — resolved under the branding images root |
| Absolute classpath | `/com/example/icons/save_16.png` |
| Filesystem path | `[F]:/absolute/path/to/icon.png` |

### SVG files

```
filename.svg
filename.svg|w|h|scale|style|stroke|fill
```

Parameters after `|` are all optional (leave blank to skip):
- **w** — render width in pixels
- **h** — render height in pixels
- **scale** — extra scale multiplier applied after w/h
- **style** — inline CSS applied to the wrapping `ImageView` (not the SVG itself)
- **stroke** — replaces every `currentColor` keyword in the SVG source and overrides all explicit `stroke="…"` attributes (except `stroke="none"`) with this color
- **fill** — overrides all explicit `fill="…"` attributes (except `fill="none"`) and injects `fill` onto shape elements that carry no inline fill attribute

Because `#` is the overlay-chain separator, hex colors in **stroke**/**fill** are written without it:

| Notation | Example | Resolved to |
|---|---|---|
| 3 hex chars (`RGB`) | `333` | `#333` |
| 4 hex chars (`RGBA`) | `333F` | `#333F` |
| 6 hex chars (`RRGGBB`) | `4A90D9` | `#4A90D9` |
| 8 hex chars (`RRGGBBAA`) | `4A90D980` | `#4A90D980` |
| `0xRRGGBB` / `0xRRGGBBAA` | `0xFFFFFF` / `0xFFFFFF80` | `#FFFFFF` / `#FFFFFF80` |
| CSS named color / `rgba(...)` | `white`, `rgba(0,0,0,0.5)` | used as-is |

```
search.svg|16|16|||333333        # 16 px, stroke #333333
arrow.svg|24|24|||FFFFFF         # white stroke
tag.svg|16|16|||4A90D9|none      # blue stroke, fill none
tag.svg|16|16|||4A90D980         # semi-transparent blue stroke (50 % alpha)
home.svg|24|24                   # no colour injection
```

Paths follow the same relative / absolute / `[F]:` rules as raster images.

### ICO files

```
filename.ico
filename.ico|size
filename.ico|w|h
```

Selects the best-matching frame from a multi-image ICO file:
- **no size** — returns the largest available frame
- **`|size`** — picks the frame closest to `size × size`; prefers exact match, then next-larger, then largest
- **`|w|h`** — same logic using `min(w, h)` as the target

Paths follow the same relative / absolute / `[F]:` rules as raster images. Supported frame formats: PNG-in-ICO, 32-bpp BGRA DIB, 24-bpp BGR DIB, and indexed (≤8-bpp) DIB.

### Solid-colour canvas

```
EMPTY|size
EMPTY|w|h
EMPTY|w|h|color
```

Creates a synthetic transparent canvas useful as a base layer. `h` may be left blank to default to `w`. `color` uses the same notation as SVG stroke/fill.

```
EMPTY|24               # 24×24 transparent canvas
EMPTY|32|16            # 32×16 transparent canvas
EMPTY|24|24|4A90D9     # 24×24 solid blue canvas
```

### Inline SVG path data

| Prefix | Result | Retrieve via |
|---|---|---|
| `[PI]:pathData\|w\|h\|scale\|style` | Rasterised `Image` | `getImage` / `getIconView` |
| `[P]:pathData\|w\|h\|scale\|style` | `SVGPath` node (unmanaged) | `getIconNode` |
| `[PS]:pathData\|w\|h\|scale\|style` | `SVGPath` shape (managed, layoutable) | `getIconNode` |

`[P]:` and `[PS]:` return a `Node`, not an `Image`, so `getIconView` will throw for `[P]:` specs — use `getIconNode` instead.

### Token substitution

Before lookup, every spec is passed through a two-step token replacement:

1. **`${scale}`** — built-in, dynamic. Replaced with the current DPI bucket string (e.g. `1`, `2`, `1.5`) computed by `UiScale.bucketedScaleString()`.

   ```
   icon_${scale}x.png   →   icon_2x.png   (on a 200 % DPI screen)
   ```

2. **Custom tokens** — registered via `registerToken` / `registerTokens` at startup. Any `${key}` in the spec is replaced with the corresponding value.

   ```java
   ImageUtils.registerTokens(Map.of(
       "smallIcon",  "16",
       "normalIcon", "24",
       "largeIcon",  "32"
   ));
   // spec "${normalIcon}/SAVE.png"  →  "24/SAVE.png"
   ```

   Tokens are applied in insertion order. Passing `null` as value to `registerToken` removes that token.

### Overlay chain — `#`

Multiple specs separated by `#` are alpha-composited left-to-right into a single image. The canvas is sized to fit the largest image; the first image effectively becomes the background.

An optional **alignment token** as the first `#`-separated element controls where every image in the chain is positioned on that canvas:

```
base_16.png#overlay/lock_9.png           # lock badge at bottom-right (default)
TL#base_16.png#overlay/badge_9.png       # badge at top-left
BR#EMPTY|24#save.svg|16|||||00ff00       # green icon inside 24-px canvas, at bottom-right
```

Alignment tokens: `TL`, `TR`, `BL`, `BR`, `C`. If the first element is not a recognised token it is treated as the first image.

For the alignment to have a visible effect on an overlay, the overlay image must be smaller than the largest image (which defines the canvas). All images — including the base — are positioned with the same alignment, but the largest image lands at (0, 0) regardless of alignment.

#### Subtract mode (`-`)

Append `-` to the alignment token to switch all overlay images to **subtract mode** (DST_OUT compositing): each image after the first cuts its opaque pixels out of the accumulated result instead of painting over it. The first image is always the additive base.

```
C-#base_32.png#svg/mask/circle.svg|32     # mask subtracts a centred circular hole from base
TL-#base_16.png#shadow_mask.png           # mask subtracts from base, placed at top-left
-#base_16.png#mask.png                    # subtract at default BR alignment
```

Subtraction formula per pixel: `outA = dstA × (1 − srcA)` — fully opaque mask pixels produce fully transparent output; fully transparent mask pixels leave the destination unchanged.

### Processing chain — `##`

Specs separated by `##` are each resolved independently via `createImage` (so each segment may itself be a full overlay chain), then alpha-composited in one pass. This lets you chain independently-built images together.

An optional **global alignment token** as the very first element sets the default placement and subtract mode for all segments:

```
base_16.png##overlay/Warning_9.png##overlay/lock_9.png
BR##step1.png##step2.png
```

#### Per-segment alignment and subtract

A `##` segment that starts with an alignment token (with or without `-`) **before the first `#`** overrides both the placement and the subtract mode for that segment only, while the rest of the segment is still resolved normally.

This token is parsed twice — once by the `##` compositor for outer placement, and once by `createImage` for inner `#` chain compositing — so it controls the full chain at both levels.

```
# Green 16-px icon at top-left inside a 24-px canvas, then a red badge at bottom-right:
TL#EMPTY|24#save.svg|16|||||00ff00##BR#badge.svg|9|||||ff0000

# Solid blue square, circular cutout at centre, badge at bottom-right:
EMPTY|32|32|blue##C-#svg/mask/circle.svg|32##badge.svg

# Photo with top-left corner faded, badge added:
photo.png##TL-#svg/mask/fade_tl.svg|64##badge.svg
```

To subtract a segment that is a plain image (not a `#` chain), wrap it with a bare `-`:

```
base.png##-#mask.svg        # subtracts mask.svg at default BR alignment
base.png##C-#mask.svg       # subtracts mask.svg at centre
```

A global alignment token with `-` makes **all** subsequent segments subtract:

```
C-##photo.png##mask1.svg##mask2.svg     # both masks subtract from photo, all centred
```

---

## Main API

### Loading images

```java
// Returns Image; falls back to an error-indicator icon on failure
Image ImageUtils.getImage(String spec, boolean background)

// Returns Image or null — no fallback
Image ImageUtils.getImageIfPossible(String spec, boolean background)

// Polymorphic: Image → returned as-is; ImageView → its image; String → getImage(s, true)
Image ImageUtils.getImage(Object input)

// Builds spec "baseName_size.png" and returns getImage(spec, false)
Image ImageUtils.getImage(String iconNameBase, int size)
```

`background = true` loads raster images asynchronously (non-blocking); SVG and overlays are always synchronous.

### Loading nodes / views

```java
// ImageView; throws for [P]: specs (use getIconNode instead)
ImageView ImageUtils.getIconView(String spec)
ImageView ImageUtils.getIconView(String spec, String style)       // inline CSS override
ImageView ImageUtils.getIconView(String spec, double size)        // fitWidth/fitHeight, preserveRatio
ImageView ImageUtils.getIconView(String spec, double size, String style)
ImageView ImageUtils.getIconView(Object input)                    // polymorphic (Image or String)

// Best-fit node: SVGPath/Shape for [P]:/[PS]: specs, ImageView otherwise
Node ImageUtils.getIconNode(String spec)
```

### SVG helpers

```java
// SVG at current UI scale, optionally constrained to size pixels (null = natural size)
Image ImageUtils.getScaledSvgIcon(String iconName, Integer size)

// SVG at a fixed pixel size regardless of DPI
Image ImageUtils.getSvgIcon(String iconName, Integer size)
```

`iconName` is without the `.svg` extension; resolved via the branding root.

### Compositing

```java
// Composite spec strings → Image
Image ImageUtils.overlayImages(int align, String... specs)

// Composite Image objects → Image
Image ImageUtils.overlayImages(int align, Image... images)

// Composite and wrap in ImageView
ImageView ImageUtils.overlayIconViews(int align, String... specs)
ImageView ImageUtils.overlayIconViews(int align, Image... images)
```

Alignment constants:

| Constant | Value | Meaning |
|---|---|---|
| `ALIGN_CENTER` | 0 | Centred |
| `ALIGN_BOTTOM_RIGHT` | 1 | Bottom-right of the canvas (default for badges) |
| `ALIGN_TOP_LEFT` | 2 | Top-left |
| `ALIGN_BOTTOM_LEFT` | 3 | Bottom-left |
| `ALIGN_TOP_RIGHT` | 4 | Top-right |

### Saving images

```java
// Render iconspec(s) and write to a file — format determined by extension
void ImageUtils.saveImage(File target, String... iconspecs) throws IOException
```

- **`.png`** target — all specs are resolved and alpha-composited (bottom-right alignment) into a single image, then written as PNG. A single spec is saved as-is without compositing.
- **`.ico`** target — each spec is rendered independently and stored as a separate frame in a multi-frame Windows ICO file (PNG-in-ICO encoding, Vista+ compatible). Typical use: pass the same icon at 16, 32, 48, and 256 px to produce a standard icon set.

```java
// Save a single image
ImageUtils.saveImage(new File("out.png"), "16/SAVE.png");

// Save a composed image (specs overlaid at bottom-right)
ImageUtils.saveImage(new File("composed.png"), "24/FOLDER.png", "9/LOCK.png");

// Batch-create a multi-size icon set
ImageUtils.saveImage(new File("app.ico"),
    "16/APP.png",
    "32/APP.png",
    "48/APP.png",
    "256/APP.png");
```

Throws `IOException` if any spec cannot be resolved or if writing fails. Throws `IllegalArgumentException` for unsupported extensions or an empty spec list. ICO writing is handled by `IcoWriter`.

### Utilities

```java
// Transparent 16×16 placeholder
Image     ImageUtils.getEmptyImage()
ImageView ImageUtils.getEmptyImageView()

// Classpath URL for a given resource path (for passing to CSS / FXML)
URL ImageUtils.getIconUrl(String iconPath)

// Render any JavaFX node to a WritableImage (thread-safe: posts to FX thread if needed)
Image ImageUtils.snapshotToImage(Node node, int w, int h)
```

---

## Configuration

```java
// Root prefix for relative icon names (must end with "/")
// Default: "/cz/bliksoft/branding/images/"
ImageUtils.setBrandingImagesRoot("/com/example/myapp/images/");

// Scale factor used by getScaledSvgIcon and ${scale} substitution
// Default: 1.0 — set this once at startup from UiScale.outputScale()
ImageUtils.setScale((float) UiScale.outputScale());

// Register custom tokens for spec substitution (call at app init, before any image loading)
ImageUtils.registerToken("normalIcon", "24");   // ${normalIcon} → "24"
ImageUtils.registerToken("normalIcon", null);   // removes the token
ImageUtils.registerTokens(Map.of("smallIcon", "16", "largeIcon", "32"));
```

All values are global and shared across all callers.

---

## Built-in mask SVGs

Transparency mask files are provided under `svg/mask/` relative to the branding images root. They are designed for use with subtract mode so that their alpha channel cuts away the corresponding part of a base image.

When subtracted, pixels where the mask is fully opaque become fully transparent in the result; pixels where the mask is transparent are unchanged.

### Radial masks

All radial masks are square-viewbox SVGs. `circle.*` uses a radial gradient that reaches full transparency at the midpoint of each side (so the corners are already transparent at that point). `square.*` uses a gradient radius scaled by √2, which reaches full transparency at the corners — the sides retain more opacity, giving more rectangular coverage.

| File | Center | Edge | When subtracted |
|---|---|---|---|
| `svg/mask/circle.svg` | opaque | transparent | soft circular cutout |
| `svg/mask/circle_steep.svg` | opaque | transparent, steep falloff | sharp circular cutout |
| `svg/mask/circle_in.svg` | transparent | opaque | preserves centre, cuts border ring |
| `svg/mask/circle_in_steep.svg` | transparent | opaque, steep onset | preserves centre with sharp ring |
| `svg/mask/square.svg` | opaque | transparent | soft square/vignette cutout |
| `svg/mask/square_steep.svg` | opaque | transparent, steep falloff | sharp square cutout |
| `svg/mask/square_in.svg` | transparent | opaque | preserves centre, cuts square border |
| `svg/mask/square_in_steep.svg` | transparent | opaque, steep onset | preserves centre, sharp square border |

### Linear edge fades

Each fade runs from one edge or corner (opaque) to the opposite (transparent). When subtracted, the opaque side is cut away and the transparent side is preserved.

| File | Opaque end | Transparent end | When subtracted |
|---|---|---|---|
| `svg/mask/fade_up.svg` | top | bottom | removes top edge |
| `svg/mask/fade_down.svg` | bottom | top | removes bottom edge |
| `svg/mask/fade_left.svg` | left | right | removes left edge |
| `svg/mask/fade_right.svg` | right | left | removes right edge |
| `svg/mask/fade_tl.svg` | top-left | bottom-right | removes top-left corner |
| `svg/mask/fade_tr.svg` | top-right | bottom-left | removes top-right corner |
| `svg/mask/fade_bl.svg` | bottom-left | top-right | removes bottom-left corner |
| `svg/mask/fade_br.svg` | bottom-right | top-left | removes bottom-right corner |

### Usage examples

```
# Circular cutout in a solid square, then a badge at bottom-right:
EMPTY|32|32|blue##C-#svg/mask/circle.svg|32##badge.svg

# Photo with the top edge faded out:
photo.png##-#svg/mask/fade_up.svg|64

# Photo with the top-left corner cut away, badge at bottom-right:
photo.png##TL-#svg/mask/fade_tl.svg|64##badge.svg

# Two masks both subtracted from a photo (global subtract mode):
C-##photo.png##svg/mask/circle.svg|32##svg/mask/circle_in.svg|32
```

---

## AnyImageLoader

`cz.bliksoft.javautils.fx.controls.images.AnyImageLoader`

Adapts `ImageUtils` to the BSToolbox `ImageLoader` SPI so that any code using the generic `ImageLoader.getDefault().getImage(spec)` API benefits from the full spec system:

```java
// Installed automatically during BSApp.init()
ImageLoader.setDefault(new AnyImageLoader());
```

After installation, relative names, overlays, SVG, and scale substitution all work through the standard `ImageLoader` API.

---

## Caching behaviour

- The cache is keyed by the **resolved** spec string (after all token substitutions).
- Cache is never invalidated at runtime — suitable for static icons; not for dynamically generated specs that encode runtime state.
- `createImage` is not cached; `getImageIfPossible` / `getImage` are.
- `null` results are not cached — a failed load is retried on the next call.
