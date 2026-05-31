# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

BSToolbox-jfx is a JavaFX application framework built on top of `../BSToolbox` (common-java-utils). It provides reusable infrastructure for OpenJFX GUI applications: module lifecycle, session/rights management, declarative UI building, action binding, observable bean patterns, and UI state persistence.

## Build & Test

```bash
mvn clean install     # full build
mvn test              # run tests (JUnit 5 via Surefire)
mvn formatter:format  # reformat sources — run after every change to Java files
```

Java 21, JavaFX 21.0.9. Platform-specific JavaFX natives are activated via Maven profiles (`windows`, `linux`, `mac`).

Main test entry point: `cz.bliksoft.javautils.fx.test.FxTests`. The primary test (`UnusedMessagesKeysTest`) validates that all `*Messages.properties` keys are referenced in code/FXML and vice versa. Whitelist files exist for intentionally unused keys.

## Architecture

### Application Lifecycle

`BSApp` is the static hub. It initializes the framework via `BSApp.init()`, which uses ServiceLoader-based `Modules` to load, init, and install app modules in priority order. `BaseAppModule` (priority -10000) is the core framework module.

Properties use a two-level hierarchy: local (`~/.{appname}/{appname}.properties`) overrides global (`{app-dir}/.{appname}/{appname}.properties`). Environment-specific values are prefixed with `{configname}.{key}` (default config: `"default"`, set via `app.configname`).

Events (app close, user info change, messages) flow through a shared event bus.

### Rights & Session

`SessionManager` (abstract) provides the current `UserInfo` and rights context. The default implementation (`DefaultUnrestrictedSessionManager`) grants all rights. Implement `Right` interface and register via ServiceLoader or `Rights` registry to define permissions. Check with `Rights.isAllowed(MyRight.class)`.

### Action Framework

`IUIAction` is the core interface: bindable `enabled`, `visible`, `text`, `iconSpec`, `graphic`, `hint`, `accelerator` properties. `ActionBinder` wires actions to `ButtonBase`, `MenuItem`, `Hyperlink`, `MenuButton`, `SplitMenuButton`. `IUIActionWithSubactions` supports cascading menus.

`BasicAbsentContextUIAction` provides a ready-to-use base for actions that disable themselves when context is absent.

### Key Bindings

Keyboard shortcuts are loaded from the XML filesystem and applied to actions and controls at startup. There are two complementary mechanisms:

**1. Action shortcuts via `core/actions`**

When `UIActions` loads an action file from `core/actions`, it calls `ShortcutFileLoader.load(f)` on that file object. If the file (or a `<shortcut>` child) carries a `keys` attribute, the parsed `KeyCombination` is set on the action via `UIActionBase.setAccelerator(kc)`. Example file attribute:

```xml
<file name="com.example.MyAction" keys="Ctrl+S" />
```

**2. Standalone shortcut files via `core/key-bindings`**

Any code can look up an optional shortcut from `core/key-bindings/{subpath}` by calling `ShortcutFileLoader.loadFromKeyBindings(subpath)`. The file at that path must have a `keys` attribute. If the file is absent the method returns `null` and logs a `DEBUG` message. This is used by:

- `UIActionBase.of(key, text, iconSpec, shortcutFolder, runnable)` — looks up `shortcutFolder + "/" + key`
- Multivalue editors (`ListEditor`, `KeyValueEditor`, `TreeEditor`) — look up paths under `multivalue-editors/` with hardcoded fallback key codes

Built-in defaults (defined in `BSAppUI.xml`):

| Path | Default key |
|---|---|
| `multivalue-editors/add` | `Insert` |
| `multivalue-editors/preview` | `F3` |
| `multivalue-editors/remove` | `Delete` |

**3. Shortcut syntax**

Keys are parsed by JavaFX `KeyCombination.keyCombination(String)`. Examples: `"Ctrl+S"`, `"Shortcut+N"`, `"Ctrl+Shift+Delete"`, `"F3"`, `"Insert"`. `Shortcut` is the platform-native modifier (Ctrl on Windows/Linux, Cmd on macOS). Key sequences (chord bindings) are not supported — each binding is a single key combination.

**4. Runtime wiring — `AcceleratorManager`**

`AcceleratorManager` installs action accelerators into a JavaFX `Scene.getAccelerators()` map. It tracks `acceleratorProperty()` changes live, and gates execution on both `enabledProperty()` and `visibleProperty()` — a shortcut only fires when the action is both visible and enabled. Bind an action with `AcceleratorManager.bind(action)` after calling `attach(scene)`. Accelerators on `MenuItem` bindings are handled separately via `ActionBinder.bind(MenuItem, IUIAction)`, which binds `mi.acceleratorProperty()` directly to the action's property.

### UI Builder

`FileLoader` (from BSToolbox xmlfilesystem) is extended for each JavaFX control type. Loaders exist for: standard controls (Button, Label, TextArea, ComboBox, etc.), layout panes (HBox, VBox, BorderPane, GridPane, AnchorPane, etc.), menus (MenuBar, MenuItem, ContextMenu), and FXML (delegates to `FXMLLoader` with optional controller override). UI structure is described in XML and assembled via these loaders.

**XmlFilesystem attribute format — critical:** In XmlFilesystem XML files, attributes of a node are `<attribute name="..." value="..."/>` child elements — NOT XML element attributes. The control type is the `name` attribute of the `<file>` element. Loader code reads values via `f.getAttribute("key", null)`, `f.getDouble("key", default)`, `f.getInteger("key", null)`, `f.getBool("key")`, `f.getLocalizedAttribute("key", null)`, etc. — all of which read from these child attribute nodes. A UI definition looks like:

```xml
<file name="HBox">
    <attribute name="spacing" value="10"/>
    <file name="Button">
        <attribute name="text" value="Submit"/>
        <attribute name="defaultButton" value="true"/>
    </file>
</file>
```

Never write `<HBox spacing="10">` — that is not valid XmlFilesystem UI definition syntax.

### Observable Beans & Status

`IStatusBean` tracks object lifecycle state: `INITIAL → NEW → SAVED ↔ MODIFIED`, plus `DETACHED`, `DELETED`, `DELETED_SAVED`. `IParentedStatusBean` propagates changes up to parent beans. `BasicBeanWrapper` wraps plain POJOs as observable beans. `ObjectStatus` renders state as SVG status badges.

### UI State Persistence

`FxStateManager` / `FxStateBinder` persist and restore window positions, sizes, `SplitPane` dividers, `TableView`/`TreeTableView` column widths. Bindings are registered per control; state is stored in properties files.

### Custom Controls

- **CodebookField**: dropdown/search field backed by a provider framework for fetching codebook data
- **Editors**: basic object editors, collection editors, properties editors (under `fx.controls.editors`)
- **Images**: SVG-aware image loading (JSVG), QR code support (ZXing)
- **Validation**: form validation support integrated with controls

## Key Dependencies

| Dependency | Purpose |
|---|---|
| `common-java-utils` (BSToolbox) | Base utilities, xmlfilesystem, modules, events |
| ControlsFX 11.2.3 | Extended JavaFX controls |
| JSVG | SVG rendering for icons/status badges |
| ZXing | QR code generation |
| Log4j2 | Logging |
| JAXB | XML serialization |
