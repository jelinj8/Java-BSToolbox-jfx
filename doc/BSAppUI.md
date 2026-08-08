# BSAppUI — UI Framework

`BSAppUI` is a `ModuleBase` subclass (loading order `-8000`, right after
`BaseAppModule`'s `-10000`) that owns the main window and the UI context stack.
It provides the scene builder, the UI stack API, status messaging, and progress
dialogs. Loading this early means its `/core/...` filesystem definitions
(theme, iconspec variables, etc.) merge in *before* application modules —
letting a client app's own config override framework defaults, not the other
way around.

---

## Initialization

Call `BSAppUI.init(app, stage)` once from `Application.start()`. This
registers `BSAppUI` in the module system and triggers the full startup sequence
(see **BSApp.md**). After `installModules()` runs, `BSAppUI.install()` makes the
primary stage visible.

The `BSAppUI.init(app, stage, root)` overload hosts an *externally-built* root
node on the primary stage instead of composing one from the `/AppUI` scene
definition. The framework bindings still apply: the root is wrapped in a
`Scene`, registered action accelerators are bound, window state is
restored/persisted, and the close handler is wired. A non-`null` `root` takes
precedence over any scene definition; passing `null` behaves exactly like the
two-argument form. Theming and global CSS are applied in both cases.

---

## Scene builder (UIComposer)

The main window scene is described in the XmlFilesystem under `/AppUI`. The
`root` attribute on that node names the sub-entry that holds the scene
description; `UIComposer.buildUI(fileObject, stage)` assembles it.

A minimal declaration in a module's XML:

```xml
<file name="AppUI">
    <attribute name="root">mainScene</attribute>
    <file name="mainScene">
        <attribute name="theme">SYSTEM</attribute>
        <!-- loader type determines layout -->
        <file name="SceneBorder">
            <file name="region" value="top">
                <file name="MenuBar"> … </file>
            </file>
            <file name="region" value="center" id="mainBorderPane"/>
        </file>
    </file>
</file>
```

### Common XML attributes

| Attribute | Applies to | Effect |
|---|---|---|
| `FXclass` / `FXclasses` | any Node | CSS class(es) |
| `id` | any Node | CSS/lookup id |
| `visible` / `managed` | any Node | visibility |
| `disabled` | any Node | disable control |
| `prefWidth` / `prefHeight` | Region | preferred size |
| `minWidth` / `minHeight` | Region | minimum size |
| `maxWidth` / `maxHeight` | Region | maximum size |
| `text` | Labeled | label text |
| `icon` | Labeled | icon spec (→ `ImageUtils`) |
| `tooltip` | Control | tooltip text |
| `prompt` | text input | prompt text |
| `wrap` | Label/Text | text wrapping |
| `alignment` | Labeled/HBox/VBox | alignment |
| `action` | ButtonBase/MenuItem | wires `UIActions.getAction(key)` via `ActionBinder` |
| `region` | child of BorderPane | `top`/`bottom`/`left`/`right`/`center` |
| `into` | any Node | injects into a named slot published by an `ISlotPublisher` |
| `hgrow` / `vgrow` | child of HBox/VBox | `Priority.ALWAYS` / `NEVER` / `SOMETIMES` |
| `overflowFocusable` | ToolBar | `false` makes the skin's overflow button non-focus-traversable (it is focusable by default and CSS cannot override it); toolbar children are already non-focusable by default |
| `grid.row`, `grid.col` | child of GridPane | grid position |
| `grid.rowSpan`, `grid.colSpan` | child of GridPane | grid span |

### FXML support

Use the `FXML` file type to embed an `.fxml` file:

```xml
<file name="FXML">
    <attribute name="path">/com/example/MyView.fxml</attribute>
    <!-- optional: override controller class -->
    <attribute name="controller">com.example.MyViewController</attribute>
</file>
```

If the controller implements `ISlotPublisher` it can publish named slots that
sibling nodes target with `into="slotName"`.

### Available loaders

Layout panes: `BorderPane`, `HBox`, `VBox`, `GridPane`, `AnchorPane`,
`ScrollPane`, `StackPane`, `SplitPane`, `TabPane`, `TilePane`, `FlowPane`.

Controls: `Button`, `ToggleButton`, `CheckBox`, `RadioButton`, `Label`,
`TextField`, `TextArea`, `PasswordField`, `ComboBox`, `Spinner`, `Slider`,
`ProgressBar`, `ProgressIndicator`, `Hyperlink`, `MenuButton`, `SplitMenuButton`,
`ListView`, `TableView`, `TreeView`, `TreeTableView`, `Separator`,
`DatePicker`, `ColorPicker`, `QRLabel`.

**`QRLabel`** renders its `text` as a pixel-exact QR code image (no blur). Specific attributes:

| Attribute | Default | Effect |
|---|---|---|
| `text` | — | Text/URL to encode |
| `modulusMultiplier` | `4` | Pixels per QR module |
| `errorCorrectionLevel` | `M` | ZXing level: `L`, `M`, `Q`, `H` |

Menus: `MenuBar`, `Menu`, `MenuItem`, `CheckMenuItem`, `RadioMenuItem`,
`SeparatorMenuItem`, `ContextMenu`.

Scene wrappers: `SceneBorder` (BorderPane-rooted scene),
`SceneStack` (StackPane), `SceneTabs` (TabPane).

### Theme

Declared via the `theme` attribute on the scene node (`LIGHT`, `DARK`, `SYSTEM`,
`NONE`). Overridden at runtime by the `ui.theme` property in local/global
properties. `Styling.installGlobalCss()` applies the framework CSS.

### UI zoom (`ui.scale`)

`ui.scale` (local/global property, default `1.0`) sets a live, restart-free zoom
factor applied to every window: `Styling.setUiScale(double)` cascades an
`-fx-font-size` onto each scene root (the default stylesheets size controls in
`em` units, so this scales fonts, spacing, and control sizing together). Read
once at startup (before `Styling.installGlobalCss()`) from the `ui.scale`
property; call `Styling.setUiScale(...)` directly at runtime for an in-app
zoom-in/zoom-out action — it re-applies immediately to all open windows.

`-fx-font-size` only cascades to controls whose sizing is `em`-relative — icons
are rasterized at a fixed pixel size and don't follow it. At the same point,
`BSAppUI` also pushes the same value into `IconspecUtils` as its `ui-scale`
iconspec variable (`IconspecUtils.setVariable("ui-scale", ...)`), so icon sizes
at least start out proportionate — see the `icon-scale`/`ui-scale` iconspec
variables in **ImageUtils.md**. Unlike the live font-size cascade, this only
takes effect at startup: a runtime `ui.scale` change does not retroactively
re-rasterize already-bound icons.

Leaving `ui.scale` at its default `1.0` (or setting it to `1` explicitly in
settings) is harmless — it's a no-op, since a scale of `1.0` is identity for
both the font-size cascade and the `ui-scale` iconspec variable. (Given that,
loading could reasonably skip applying it at all when the value is `1`.)

This is independent of the OS DPI override documented in **BSApp.md**, which
corrects how JavaFX reads the display's physical scale — unlike `ui.scale=1`,
setting `ui.dpiScale=1` is *not* a no-op.

### Stage icons (`iconBase`)

The `iconBase` attribute on the scene node sets the window/taskbar icons.

**PNG variant** — append size + `.png` suffix to a base path:
```xml
<attribute name="iconBase" value="/icons/app/Home_" />
<!-- loads: /icons/app/Home_16.png, _22.png, _24.png, _32.png, _48.png, _64.png, _128.png, _256.png -->
```

**SVG variant** — use any ImageUtils spec with `${size}` placeholder:
```xml
<attribute name="iconBase" value="[F]:icons/app/Home.svg|${size}" />
<!-- renders: [F]:icons/app/Home.svg|16, |22, |24, |32, |48, |64, |128, |256 via SvgConverter -->
```

Sizes tried in both cases: `16`, `22`, `24`, `32`, `48`, `64`, `128`, `256`. Missing sizes are skipped. The
in-between sizes (`22`/`24`/`64`/`128`) exist mainly for Linux taskbars/panels (e.g. lxpanel on Raspbian) that
pick a nearby exact size rather than scaling well from a distant one — without them some panels fall back to a
generic icon even though the window's own icon (title bar, Alt-Tab) renders fine.

### SVG default colors

`currentColor` in SVG files is replaced at render time so that theme-aware icon
colors work without a live CSS context. Theme-aware defaults can be declared under
`/AppUI/colors/themes/light` and `/AppUI/colors/themes/dark`:

```xml
<file name="AppUI">
    …
    <file name="colors">
        <file name="themes">
            <file name="light">
                <attribute name="stroke" value="#222222" />
                <!-- <attribute name="fill" value="#222222" /> -->
            </file>
            <file name="dark">
                <attribute name="stroke" value="#e8e8e8" />
            </file>
        </file>
    </file>
</file>
```

`BSAppUI` reads the active theme's node after resolving the theme mode and sets
`SvgConverter.setDefaultStrokeColor` / `setDefaultFillColor`. The defaults apply
**only** when the SVG content actually contains `currentColor` (stroke) or
`fill="currentColor"` (fill) — explicit colors in the SVG are never touched.

Priority (highest → lowest):
1. Iconspec params[5]/[6] — `icon.svg|size||scale||strokeColor|fillColor`
2. Theme defaults from `/AppUI/colors/themes/<light|dark>`
3. Hard fallback `black` for stroke (so the renderer never receives an unresolved `currentColor`)

Bare hex values (`222222`) are accepted in addition to `#`-prefixed ones.

---

## UI stack

`BSAppUI` maintains a stack of `Context` objects. The top context drives what
appears in the main content area (`mainPane.setCenter()`).

```java
BSAppUI.pushUI(myComponent);           // push Node; creates wrapper context
BSAppUI.pushUI(ctx);                   // push existing Context
BSAppUI.pushUI(ctx, myComponent);      // push both (component stored as CTX_MAIN_COMPONENT)
BSAppUI.popUI();                       // pop top context; returns it
```

When a pushed `Node` implements `IStackedComponent`, the framework calls:
- `afterPush()` — immediately after the push (component is now visible)
- `beforePop()` — just before the pop (component is about to be removed)

Use `afterPush()` to restore UI state and `beforePop()` to save it or run
cleanup guards.

---

## Status messages

Fire a `MessageEvent` into the current context so the status bar (or any
registered listener) can display it:

```java
BSAppUI.showStatusMessage("Record saved.");
BSAppUI.showStatusMessage("Record saved.", "/icons/check.png");
BSAppUI.showStatusMessage("Error!", "/icons/error.png", "status-error");
```

---

## Progress dialog

Show a modal ControlsFX `ProgressDialog` while running a background operation:

```java
// Simple Runnable
BSAppUI.executeWaiting(() -> doLongOperation(), "Processing…");

// JavaFX Task (result returned)
MyResult result = BSAppUI.executeWaiting(myTask, "Loading data…");
```

For manual control:

```java
BSAppUI.showWorkingWheel("Importing", "Please wait…");
BSAppUI.setWorkingWheelTitle("Step 2 of 3");
BSAppUI.setWorkingWheelProgress(0.6);
BSAppUI.hideWorkingWheel();
```

All `set*` calls are thread-safe and may be called from background threads.

---

## FxStateManager — UI state persistence

`FxStateManager` traverses a `Node` subtree and delegates to registered
`FxStateBinder` implementations for each node that has a `state.key` property.

```java
FxStateManager manager = new FxStateManager("myWindowKey");
manager.restoreState(rootNode);   // on open / afterPush
manager.persistState(rootNode);   // on close / beforePop
```

Mark nodes with `FxStateMeta`:

```java
FxStateMeta.key(splitPane, "mainSplit");          // node is stateful
FxStateMeta.ctx(tableView, "ordersSection");      // add a context segment to the key prefix
FxStateMeta.resetCtx(embeddedPanel, true);        // start fresh key prefix for this subtree
```

State keys are composed as `windowKey[.ctx…].nodeKey`. State is stored in local
properties via `BSApp.getLocalProperties()`.

### Built-in binders

| Binder | Node type | Persists |
|---|---|---|
| `SplitPaneBinder` | `SplitPane` | Divider positions (`{pfx}.div`) |
| `TableViewBinder` | `TableView` | Column widths and sort order |
| `TreeTableViewBinder` | `TreeTableView` | Column widths |
| `StageStateBinder` | `Stage` | Position, size, maximized, fullscreen |

`StageStateBinder` is also used directly (not via the tree traversal) to persist
the main stage:

```java
StageStateBinder.restore(stage, "@main");   // called in BSAppUI.init()
StageStateBinder.persist(stage, "@main");   // called on close
```

When restoring a non-maximized window, `restore` calls
`BSAppUI.fitToScreen(window)`: if the saved bounds overlap a screen border or
exceed the screen (e.g. saved on a large monitor, restored on a 1280x720
display), the window is shrunk to the screen's visual bounds and moved fully
into view. A window that already fits — including one intentionally spanning
multiple monitors — is left untouched. `fitToScreen` is public with `Window`
and `Dialog` overloads, so custom dialogs can use it too (call it once the
window is shown and sized).

Add custom binders by implementing `FxStateBinder` and registering it in a
`FxStateManager` instance if needed.
