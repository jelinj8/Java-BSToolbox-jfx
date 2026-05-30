# BSAppUI — UI Framework

`BSAppUI` is a `ModuleBase` subclass (loading order `10000`) that owns the main
window and the UI context stack. It provides the scene builder, the UI stack
API, status messaging, and progress dialogs.

---

## Initialization

Call `BSAppUI.init(app, stage)` once from `Application.start()`. This
registers `BSAppUI` in the module system and triggers the full startup sequence
(see **BSApp.md**). After `installModules()` runs, `BSAppUI.install()` makes the
primary stage visible.

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
`DatePicker`, `ColorPicker`.

Menus: `MenuBar`, `Menu`, `MenuItem`, `CheckMenuItem`, `RadioMenuItem`,
`SeparatorMenuItem`, `ContextMenu`.

Scene wrappers: `SceneBorder` (BorderPane-rooted scene),
`SceneStack` (StackPane), `SceneTabs` (TabPane).

### Theme

Declared via the `theme` attribute on the scene node (`LIGHT`, `DARK`, `SYSTEM`,
`NONE`). Overridden at runtime by the `ui.theme` property in local/global
properties. `Styling.installGlobalCss()` applies the framework CSS.

### Stage icons (`iconBase`)

The `iconBase` attribute on the scene node sets the window/taskbar icons.

**PNG variant** — append size + `.png` suffix to a base path:
```xml
<attribute name="iconBase" value="/icons/app/Home_" />
<!-- loads: /icons/app/Home_16.png, _32.png, _48.png, _256.png -->
```

**SVG variant** — use any ImageUtils spec with `${size}` placeholder:
```xml
<attribute name="iconBase" value="[F]:icons/app/Home.svg|${size}" />
<!-- renders: [F]:icons/app/Home.svg|16, |32, |48, |256 via SvgConverter -->
```

Sizes tried in both cases: `16`, `32`, `48`, `256`. Missing sizes are skipped.

### SVG default colors

`currentColor` in SVG files is not supported by SVG Salamander and must be
replaced at render time. Theme-aware defaults can be declared under
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
3. Hard fallback `black` for stroke (so SVG Salamander never sees `currentColor`)

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

Add custom binders by implementing `FxStateBinder` and registering it in a
`FxStateManager` instance if needed.
