# ImageUtils

`cz.bliksoft.javautils.fx.tools.ImageUtils`

Central utility for loading, compositing, and caching JavaFX `Image` objects and icon nodes. All results are cached by the resolved spec string. Images are described by a compact string format called an *icon spec*.

---

## Quick start

```java
// Image; falls back to an error-indicator icon on failure
Image img = ImageUtils.getImage("save_16.png", false);

// ImageView, possibly sized
ImageView iv = ImageUtils.getIconView("save.svg");
ImageView iv = ImageUtils.getIconView("save.svg", 24.0);

// Polymorphic: accepts Image, ImageView, or String spec
Node icon = ImageUtils.getIconNode("save.svg|24");       // ImageView
Node icon = ImageUtils.getIconNode("[P]:M0 0 L10 10|16"); // SVGPath node

// Composed image: cube icon with a green plus badge at bottom-right
Image badge = ImageUtils.getImage("cube.svg|24#plus.svg|12|||2b8a3e#*+", false);
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
filename.svg|w|h|scale|stroke|fill
```

Parameters after `|` are all optional (leave blank to skip):
- **w** — render width in pixels
- **h** — render height in pixels
- **scale** — extra scale multiplier applied after w/h
- **stroke** — replaces every `currentColor` keyword in the SVG source and overrides all explicit `stroke="…"` attributes (except `stroke="none"`) with this color
- **fill** — overrides all explicit `fill="…"` attributes (except `fill="none"`) and injects `fill` onto shape elements that carry no inline fill attribute

> **Note:** the `viewStyle` parameter that formerly occupied slot 4 has been removed. Use the [`*JFXSTYLE`](#jfxstyle) postfix command to apply CSS to the wrapping `ImageView`.

Because `#` is the postfix token separator, hex colors in **stroke**/**fill** are written without it:

| Notation | Example | Resolved to |
|---|---|---|
| 3 hex chars (`RGB`) | `333` | `#333` |
| 4 hex chars (`RGBA`) | `333F` | `#333F` |
| 6 hex chars (`RRGGBB`) | `4A90D9` | `#4A90D9` |
| 8 hex chars (`RRGGBBAA`) | `4A90D980` | `#4A90D980` |
| `0xRRGGBB` / `0xRRGGBBAA` | `0xFFFFFF` / `0xFFFFFF80` | `#FFFFFF` / `#FFFFFF80` |
| CSS named color / `rgba(...)` | `white`, `rgba(0,0,0,0.5)` | used as-is |

```
search.svg|16|16||333333        # 16 px, stroke #333333
arrow.svg|24|24||FFFFFF         # white stroke
tag.svg|16|16||4A90D9|none      # blue stroke, fill none
tag.svg|16|16||4A90D980         # semi-transparent blue stroke (50 % alpha)
home.svg|24|24                  # no colour injection
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

---

## Postfix composition

Specs that contain `#` are evaluated as a **postfix (RPN) expression**. The spec is split on every `#`; each token is either:

- A **file spec** (does not start with `*`) — loaded as an image and pushed onto the stack.
- A **command** (starts with `*`) — operates on the stack or sets a mode value.

When processing is complete, the **top of the stack** is returned as the result.

### Anchor — `*ANCHOR`

```
*ANCHOR|position
*ANCHOR|position|offsetX|offsetY
*ANCHOR|BR|5|-3
*ANCHOR|C
*ANCHOR|N
```

Sets the current anchor position (and optional pixel offsets) used by `*+`, `*-`, `*CROP`, `*DRAW`, and `*TEXT`. Offsets default to `0, 0` and persist until overridden or `*RESET`. Default at startup is `BR, 0, 0`.

| Position | Meaning |
|---|---|
| `TL` | Top-left corner |
| `TR` | Top-right corner |
| `BL` | Bottom-left corner |
| `BR` | Bottom-right corner (default) |
| `C` | Centred |
| `N` | New — forces `*TEXT` to push a standalone image without compositing onto the existing canvas |

### Combine — `*+` and `*-`

```
base.svg|24#badge.svg|12#*+             # overlay badge at BR (default)
*ANCHOR|C#base.svg|24#mask.svg|24#*-           # subtract mask centred on base
*ANCHOR|BR|2|2#base.svg|24#badge.svg|12#*+     # overlay badge, shifted 2 px right and down
```

Pops the top two images from the stack — **top** is the overlay/mask, **second** is the base — composites them, and pushes the result.

- **`*+`** — SRC_OVER (normal alpha compositing): overlay paints over the base.
- **`*-`** — DST_OUT (alpha subtract): overlay cuts its opaque pixels out of the base.

The overlay is positioned relative to the base using the current alignment and offsets. The result canvas is the **union bounding box** of both positioned images, so a non-zero offset that pushes the overlay beyond the base edges will grow the canvas accordingly.

For a chain of more than two images, repeat `*+` (or `*-`):

```
bottom.svg|32#mid.svg|16#*+#top.svg|9#*ANCHOR|BR#*+
```

### Canvas — `*EMPTY`

```
*EMPTY|size
*EMPTY|w|h
*EMPTY|w|h|color
```

Creates a synthetic canvas and pushes it. `h` may be blank to default to `w`. `color` uses the same notation as SVG stroke/fill. Equivalent to the bare `EMPTY|…` file-spec keyword, but explicit as a command.

```
*EMPTY|24                  # 24×24 transparent canvas
*EMPTY|32|16               # 32×16 transparent canvas
*EMPTY|24|24|4A90D9        # 24×24 solid blue canvas
```

### Text rendering — `*TEXT`

```
*TEXT|value|color|size|font
```

Sets mode values (`color`, `size`, `font`) for subsequent TEXT commands. If `value` is non-empty, renders it to a canvas and pushes the result. Any mode values already set are reused when parameters are omitted.

```
*TEXT|Hello|333333|14|Arial     # render "Hello" in dark grey, 14 px Arial
*TEXT|16                        # set size only (colour/font unchanged)
```

### Filters — `*FILTER`

```
*FILTER|name|p1|p2|...
```

Pops the top image, applies the named filter, and pushes the result.

| Filter | Parameters | Description |
|---|---|---|
| `shadow` | `color`, `width`, `fill` | Diffuse fade-out shadow/glow. `fill`: `F`=filled interior (default), `T`=holes preserved |
| `outline` | `color`, `width`, `fill` | Sharp silhouette expansion (no blur). Same `fill` flag as shadow |
| `rotate` | `angle` | Clockwise rotation in degrees. Multiples of 90° resize the canvas losslessly; other angles keep canvas size using bilinear interpolation |
| `shift` | `angle`, `pixels` | Translate in direction `angle` (0°=right, 90°=down); canvas size unchanged |
| `scale` | `w`, `h`, `mode` | Scale image. `mode`: `F`=fit (letterbox), `C`=crop/fill, `%`=percent |
| `resize` | `w`, `h` | Change canvas size without scaling, using current anchor |
| `mask` | `color`, `invert` | Replace all pixel colours with `color` keeping original alpha. `invert`: `N`=normal, `Y`=invert alpha |
| `monochrome` | `color` | Convert to monochrome weighted by pixel luminance |
| `keymask` | `color` | Replace `color` (or auto-detected corner colour) with full transparency |
| `mirror` | `direction` | Flip image. `direction`: `H`=horizontal/left-right (default), `V`=vertical/top-bottom |

```
icon.svg|24#*FILTER|rotate|90        # 90° clockwise, canvas resized to fit
icon.svg|24#*FILTER|rotate|45        # 45° rotation, canvas size unchanged
icon.svg|24#*FILTER|mirror           # flip left-right
icon.svg|24#*FILTER|mirror|V         # flip top-bottom
icon.svg|24#*FILTER|resize|32|32     # crop/pad to 32×32 using current anchor
icon.svg|24#*ANCHOR|TL#*FILTER|resize|16|16   # take top-left 16×16 region
```

### Stack manipulation

| Command | Effect |
|---|---|
| `*DUPLICATE` | Push a copy of the top image (top remains) |
| `*COPY` | Store a copy of the top image in a clipboard slot (stack unchanged) |
| `*PASTE` | Push a copy of the clipboard image (clipboard preserved) |
| `*POP` | Discard the top image |
| `*RESET` | Clear all mode values (alignment reverts to `BR, 0, 0`; clipboard cleared) |

### JFXSTYLE

```
*JFXSTYLE|-fx-opacity: 0.5;
```

Sets a CSS style string that `getIconView` will apply to the wrapping `ImageView` after loading the image. This replaces the former `viewStyle` parameter (slot 4 of the SVG spec). The value is not baked into the image — it is only applied when the result is retrieved through `getIconView`.

```java
// Spec with embedded JFXSTYLE
ImageView iv = ImageUtils.getIconView("icon.svg|24#*JFXSTYLE|-fx-opacity: 0.5;");
// → iv has style "-fx-opacity: 0.5;" applied
```

### Explicit cache — `*GET_CACHE` and `*PUT_CACHE`

```
*GET_CACHE|key
*PUT_CACHE|key
```

Allows a composed sub-image to be reused across multiple spec evaluations without rebuilding it each time. The key is a plain string; it addresses a separate user-controlled cache slot (not the automatic spec-string cache).

**Semantics:**

- **`*GET_CACHE|key`** — if `key` is present in cache: push the cached image and **skip** all following tokens up to and including the matching `*PUT_CACHE|key`, then continue. If not present: do nothing (fall through).
- **`*PUT_CACHE|key`** — if not currently being skipped: store a copy of the stack top under `key`. Acts as the skip target for a matching `*GET_CACHE`.

**Pattern — shared base image:**

```
*GET_CACHE|sharedBase
  ... expensive commands to build base ...
*PUT_CACHE|sharedBase
... overlay commands on top of base ...
#*+
```

First call: cache miss → base is built, stored under `sharedBase`, overlay is applied.  
Subsequent calls: cache hit → base is fetched from cache, expensive commands are skipped, overlay is applied.

> Pairs must use identical keys. Nesting with different keys is supported; pairs with the same key must not be interleaved.

---

## Postfix examples

```
# Simple badge overlay (badge at BR, default):
cube.svg|24#search.svg|16#*+

# Badge with colour:
cube.svg|24#plus.svg|16|||2b8a3e#*+

# Badge at top-left:
cube.svg|24#plus.svg|16#*ANCHOR|TL#*+

# Circular cutout in a solid blue square, then a badge:
*EMPTY|32|32|4A90D9#*ANCHOR|C#svg/mask/circle.svg|32#*-#badge.svg|12#*ANCHOR|BR#*+

# Green save icon (TL) and red save icon (BR) on a shared 24-px canvas:
*EMPTY|24#save.svg|16||||00ff00#*ANCHOR|TL#*+#save.svg|16||||ff0000#*ANCHOR|BR#*+

# Centre a square with an X overlaid on it:
*ANCHOR|C#square.svg|24#x.svg|24|||c0392b#*+

# Shared expensive base used by two different final specs:
*GET_CACHE|myBase#...#*PUT_CACHE|myBase#badge_a.svg|12#*+
*GET_CACHE|myBase#...#*PUT_CACHE|myBase#badge_b.svg|12#*+
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

`background = true` loads raster images asynchronously (non-blocking); SVG and postfix evaluation are always synchronous.

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

`getIconView` reads the `*JFXSTYLE` value set during postfix evaluation and applies it to the returned `ImageView` automatically.

### SVG helpers

```java
// SVG at current UI scale, optionally constrained to size pixels (null = natural size)
Image ImageUtils.getScaledSvgIcon(String iconName, Integer size)

// SVG at a fixed pixel size regardless of DPI
Image ImageUtils.getSvgIcon(String iconName, Integer size)
```

`iconName` is without the `.svg` extension; resolved via the branding root.

### Compositing (Java API)

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

// Save a composed image
ImageUtils.saveImage(new File("composed.png"), "base.svg|24#badge.svg|12#*+");

// Batch-create a multi-size icon set
ImageUtils.saveImage(new File("app.ico"),
    "16/APP.png",
    "32/APP.png",
    "48/APP.png",
    "256/APP.png");
```

Throws `IOException` if any spec cannot be resolved or if writing fails. Throws `IllegalArgumentException` for unsupported extensions or an empty spec list.

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

Transparency mask files are provided under `svg/mask/` relative to the branding images root. They are designed for use with `*-` (subtract mode) so that their alpha channel cuts away the corresponding part of a base image.

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
# Circular cutout in a solid blue square, then a badge at bottom-right:
*EMPTY|32|32|4A90D9#*ANCHOR|C#svg/mask/circle.svg|32#*-#badge.svg|12#*ANCHOR|BR#*+

# Photo with the top edge faded out:
photo.png#*-#svg/mask/fade_up.svg|64#*+

# Photo with the top-left corner cut away, badge at bottom-right:
photo.png#*ANCHOR|TL#svg/mask/fade_tl.svg|64#*-#badge.svg|12#*ANCHOR|BR#*+

# Two masks both subtracted from a photo, centred:
photo.png#*ANCHOR|C#svg/mask/circle.svg|32#*-#svg/mask/circle_in.svg|32#*-
```

---

## AnyImageLoader

`cz.bliksoft.javautils.fx.controls.images.AnyImageLoader`

Adapts `ImageUtils` to the BSToolbox `ImageLoader` SPI so that any code using the generic `ImageLoader.getDefault().getImage(spec)` API benefits from the full spec system:

```java
// Installed automatically during BSApp.init()
ImageLoader.setDefault(new AnyImageLoader());
```

After installation, relative names, postfix composition, SVG, and scale substitution all work through the standard `ImageLoader` API.

---

## Caching behaviour

- The cache is keyed by the **resolved** spec string (after all token substitutions).
- Cache is never invalidated at runtime — suitable for static icons; not for dynamically generated specs that encode runtime state.
- `createImage` is not cached; `getImageIfPossible` / `getImage` are.
- `null` results are not cached — a failed load is retried on the next call.
- The `*GET_CACHE` / `*PUT_CACHE` commands provide an explicit secondary cache keyed by a user-chosen string, independent of the automatic spec-string cache.
