# ImageUtils

`cz.bliksoft.javautils.fx.tools.ImageUtils`

Central utility for loading, compositing, and caching JavaFX `Image` objects and icon nodes. All results are cached by the resolved spec string. Images are described by a compact string format called an *icon spec*.

---

## Quick start

```java
// Image with fallback to error icon if loading fails
Image img = ImageUtils.getImage("save_16.png");

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
- **stroke** — sets the SVG `color` CSS property on the root element; all `currentColor` references throughout the SVG (typically `stroke="currentColor"`) resolve to this value via CSS inheritance
- **fill** — overrides the `fill` attribute on the root SVG element (replaces any existing value, e.g. `none`)

Because `#` is the overlay-chain separator, hex colors in **stroke**/**fill** are written without it:

| Notation | Example | Resolved to |
|---|---|---|
| 3 or 6 hex chars | `333` / `4A90D9` | `#333` / `#4A90D9` |
| `0xRRGGBB` | `0xFFFFFF` | `#FFFFFF` |
| CSS named color | `white`, `none` | used as-is |

```
search.svg|16|16|||333333        # 16 px, stroke #333333
arrow.svg|24|24|||FFFFFF         # white stroke
tag.svg|16|16|||4A90D9|none      # blue stroke, fill none
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

Multiple specs separated by `#` are alpha-composited into a single image. An optional alignment token as the **first** `#`-separated element controls where each subsequent image lands on the base:

```
base_16.png#overlay/lock_9.png           # lock badge at bottom-right (default)
TL#base_16.png#overlay/badge_9.png       # badge at top-left
```

Alignment tokens: `TL`, `TR`, `BL`, `BR`, `C`. If the first element is not a token it is treated as the first image.

### Processing chain — `##`

Specs separated by `##` are each resolved independently (via `createImage`, so each segment may itself be an overlay chain), then composited in one pass. An optional alignment token as the very first `##`-element sets the composite alignment:

```
base_16.png##overlay/Warning_9.png##overlay/lock_9.png
BR##step1.png#badge.png##step2.png
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
ImageView ImageUtils.getIconView(String spec, String style)  // inline CSS override
ImageView ImageUtils.getIconView(String spec, double size)   // fitWidth/fitHeight, preserveRatio
ImageView ImageUtils.getIconView(String spec, double size, String style)
ImageView ImageUtils.getIconView(Object input)               // polymorphic (Image or String)

// Best-fit node: SVGPath/Shape for [P]:/[PS]: specs, ImageView otherwise
Node ImageUtils.getIconNode(String spec)
```

### SVG helpers

```java
// SVG at current UI scale (natural size)
Image ImageUtils.getScaledSvgIcon(String iconName, null)

// SVG at current UI scale, constrained to `size` pixels
Image ImageUtils.getScaledSvgIcon(String iconName, int size)

// SVG at fixed pixel size regardless of DPI
Image ImageUtils.getSvgIcon(String iconName, int size)
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
| `ALIGN_BOTTOM_RIGHT` | 1 | Bottom-right of the canvas (default for badges) |
| `ALIGN_TOP_LEFT` | 2 | Top-left |
| `ALIGN_BOTTOM_LEFT` | 3 | Bottom-left |
| `ALIGN_TOP_RIGHT` | 4 | Top-right |
| `ALIGN_CENTER` | 0 | Centered |

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
