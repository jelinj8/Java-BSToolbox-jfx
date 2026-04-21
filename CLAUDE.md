# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

BSToolbox-jfx is a JavaFX application framework built on top of `../BSToolbox` (common-java-utils). It provides reusable infrastructure for OpenJFX GUI applications: module lifecycle, session/rights management, declarative UI building, action binding, observable bean patterns, and UI state persistence.

## Build & Test

```bash
mvn clean install     # full build
mvn test              # run tests (JUnit 5 via Surefire)
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

### UI Builder

`FileLoader` (from BSToolbox xmlfilesystem) is extended for each JavaFX control type. Loaders exist for: standard controls (Button, Label, TextArea, ComboBox, etc.), layout panes (HBox, VBox, BorderPane, GridPane, AnchorPane, etc.), menus (MenuBar, MenuItem, ContextMenu), and FXML (delegates to `FXMLLoader` with optional controller override). UI structure is described in XML and assembled via these loaders.

### Observable Beans & Status

`IStatusBean` tracks object lifecycle state: `INITIAL → NEW → SAVED ↔ MODIFIED`, plus `DETACHED`, `DELETED`, `DELETED_SAVED`. `IParentedStatusBean` propagates changes up to parent beans. `BasicBeanWrapper` wraps plain POJOs as observable beans. `ObjectStatus` renders state as SVG status badges.

### UI State Persistence

`FxStateManager` / `FxStateBinder` persist and restore window positions, sizes, `SplitPane` dividers, `TableView`/`TreeTableView` column widths. Bindings are registered per control; state is stored in properties files.

### Custom Controls

- **CodebookField**: dropdown/search field backed by a provider framework for fetching codebook data
- **Editors**: basic object editors, collection editors, properties editors (under `fx.controls.editors`)
- **Images**: SVG-aware image loading (SVG Salamander), QR code support (ZXing)
- **Validation**: form validation support integrated with controls

## Key Dependencies

| Dependency | Purpose |
|---|---|
| `common-java-utils` (BSToolbox) | Base utilities, xmlfilesystem, modules, events |
| ControlsFX 11.2.3 | Extended JavaFX controls |
| SVG Salamander | SVG rendering for icons/status badges |
| ZXing | QR code generation |
| Log4j2 | Logging |
| JAXB | XML serialization |
