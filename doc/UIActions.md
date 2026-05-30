# UI Actions

The action framework decouples operations from the controls that trigger them.
An `IUIAction` is a self-describing command object: it carries its own enabled,
visible, text, icon, hint, and accelerator observables. `ActionBinder` wires
those observables to JavaFX controls. `UIActions` is the application-wide
registry.

---

## IUIAction

```java
public interface IUIAction {
    void execute();

    default ObservableBooleanValue enabledProperty()  { return null; }  // null = always enabled
    default ObservableBooleanValue visibleProperty()  { return null; }  // null = always visible
    default ReadOnlyStringProperty textProperty()     { return null; }
    default ReadOnlyStringProperty iconSpecProperty() { return null; }  // ImageUtils spec
    default ReadOnlyObjectProperty<Node> graphicProperty() { return null; }
    default ReadOnlyStringProperty hintProperty()    { return null; }
    default ReadOnlyObjectProperty<KeyCombination> acceleratorProperty() { return null; }

    String getKey();   // unique registry key
}
```

`null` properties mean "I don't control this" — `ActionBinder` simply does not
bind that aspect of the control.

### IUIActionWithSubactions

Extends `IUIAction` with an observable list of child actions. Used for
`MenuButton` and `SplitMenuButton`:

```java
public interface IUIActionWithSubactions extends IUIAction {
    ObservableList<IUIAction> getSubactions();
}
```

---

## UIActions registry

Actions are loaded once (lazily, thread-safe) from two sources:

1. **XmlFilesystem** — files under `core/actions`; file name is the FQCN; the
   optional `key` attribute overrides the value from `getKey()`.
2. **ServiceLoader** — `META-INF/services/cz.bliksoft.javautils.app.ui.actions.IUIAction`

Register an action at runtime (e.g. for dynamic actions):

```java
UIActions.registerAction("MyKey", myAction, "plugin");
```

Retrieve:

```java
IUIAction action = UIActions.getAction("Save");
```

In XML UI descriptions the `action` attribute on any `ButtonBase` or `MenuItem`
does this lookup automatically and wires the control via `ActionBinder`.

---

## ActionBinder

Wires action observables to controls as one-way, permanent bindings.

```java
ActionBinder.bind(button, action);           // ButtonBase (Button, ToggleButton, …)
ActionBinder.bind(menuItem, action);         // MenuItem
ActionBinder.bind(menuButton, action);       // MenuButton (with subactions)
ActionBinder.bind(splitMenuButton, action);  // SplitMenuButton
ActionBinder.bind(hyperlink, action);        // Hyperlink
```

What `bind` does:
- `onAction` → `action.execute()`
- `disableProperty` ← `!action.enabledProperty()` (if non-null)
- `visibleProperty` and `managedProperty` ← `action.visibleProperty()` (if non-null; `managed` is also bound so hidden controls don't consume layout space)
- `textProperty` ← `action.textProperty()` (if non-null)
- Icon: `IconBinder` resolves the icon spec via `ImageUtils` and updates the graphic at the toolbar size (24 px) or menu size (16 px)
- `tooltipProperty` ← `action.hintProperty()` (if non-null)
- `acceleratorProperty` ← `action.acceleratorProperty()` (MenuItem only)

---

## UIActionBase

`UIActionBase` is the recommended abstract base class for action implementations.
It adds lazy, zero-overhead support for a keyboard accelerator, label text, and
icon spec on top of `IUIAction`:

```java
public class MyAction extends UIActionBase {

    @Override public String getKey() { return "MyKey"; }

    @Override
    public void execute() { doSomething(); }
}
```

Set properties programmatically:

```java
action.setAccelerator(KeyCombination.keyCombination("Ctrl+M"));
action.setText("My Action");
action.setIconSpec("/icons/my.svg");
```

Properties are lazy — `acceleratorProperty()` / `textProperty()` / `iconSpecProperty()`
return `null` (not a null-valued property) until the corresponding setter is called,
so unused properties carry zero overhead.

### Factory method

For simple in-place actions use the static factory:

```java
UIActionBase action = UIActionBase.of(
    "my-key",           // registry key
    "My Action",        // label (null = no text property)
    "/icons/my.svg",    // icon spec (null = no icon)
    "my-module",        // shortcut folder under core/key-bindings (null = no shortcut)
    () -> doSomething() // runnable
);
```

When `shortcutFolder` is non-null the accelerator is loaded from
`core/key-bindings/{shortcutFolder}/{key}` via `ShortcutFileLoader`.

---

## Key Bindings

### ShortcutFileLoader

`cz.bliksoft.javautils.app.ui.actions.ShortcutFileLoader` reads keyboard
shortcuts from the XML filesystem. Two entry points:

**From an arbitrary `FileObject`** (used by `UIActions` when loading action files):

```java
KeyCombination kc = ShortcutFileLoader.load(fileObject);
// reads the "keys" attribute on the FileObject
```

**From the `core/key-bindings` folder** (used by controls and `UIActionBase.of`):

```java
KeyCombination kc = ShortcutFileLoader.loadFromKeyBindings("my-module/save");
// loads /core/key-bindings/my-module/save; returns null if absent
```

Both methods return `null` on missing or unparsable input and never throw.

#### Shortcut syntax

Keys are parsed by JavaFX `KeyCombination.keyCombination(String)`:

| Example | Meaning |
|---|---|
| `"Ctrl+S"` | Control + S |
| `"Shortcut+N"` | Platform modifier + N (Cmd on macOS, Ctrl elsewhere) |
| `"Ctrl+Shift+Delete"` | Three-key combination |
| `"F3"` | Function key alone |
| `"Insert"` | Named key alone |

Key sequences (chord bindings, e.g. `Ctrl+N, T`) are not supported — each
binding is a single combination.

#### Defining a binding in XML

Add a `keys` attribute to the file node in your module's XML:

```xml
<!-- in core/actions — shortcut on an action file -->
<file name="com.example.MyAction" keys="Ctrl+M" />

<!-- in core/key-bindings — standalone lookup node -->
<file name="core">
    <file name="key-bindings">
        <file name="my-module">
            <file name="save">
                <attribute name="keys" value="Ctrl+S" />
            </file>
        </file>
    </file>
</file>
```

#### Built-in framework defaults

| Path | Default key | Used by |
|---|---|---|
| `multivalue-editors/add` | `Insert` | `ListEditor`, `KeyValueEditor`, `TreeEditor` |
| `multivalue-editors/preview` | `F3` | `ListEditor`, `KeyValueEditor`, `TreeEditor` |
| `multivalue-editors/remove` | `Delete` | `ListEditor`, `KeyValueEditor`, `TreeEditor` |

Override any of these by providing the same path in your module's XML.

### AcceleratorManager

`AcceleratorManager` installs action accelerators into `Scene.getAccelerators()`
so they fire regardless of which control has focus.

```java
AcceleratorManager mgr = new AcceleratorManager();
mgr.attach(scene);       // call once the scene is ready

mgr.bind(myAction);      // installs accelerator, respects enabled + visible
mgr.unbind(myAction);    // removes accelerator and all listeners
```

The shortcut only fires when both `enabledProperty()` and `visibleProperty()` are
`true`. Accelerator changes on the action are tracked live.

**Note:** `MenuItem` accelerators are handled separately by `ActionBinder.bind(MenuItem,
IUIAction)`, which binds `mi.acceleratorProperty()` directly to the action's property.
`AcceleratorManager` is for scene-level shortcuts that should fire regardless of
menu state.

---

## Context-aware base classes

### BasicContextUIAction\<I\>

Visible and enabled only when an object of type `I` is present in the current
context. The `I` type is the *marker interface* — any context object implementing
`I` activates the action.

```java
public class SaveAction extends BasicContextUIAction<ISave> {

    public SaveAction() { super(ISave.class); }

    @Override
    protected void execute(ISave current) { current.save(); }

    @Override
    protected BooleanProperty getEnabledProperty(ISave current) {
        return current.getSaveEnabled();   // null = always enabled
    }

    @Override
    protected String getBaseIconSpec() { return "/icons/base/SAVE_24.png"; }

    @Override
    public String getKey() { return "Save"; }
}
```

**Icon overlays:** Override `getIconOverlay(I current)` to return a
`StringProperty` whose value is appended as `baseIcon#overlaySpec`. The
overlay updates automatically when the property changes.

**Constructor trap:** `onValueChanged()` may be called from the superclass
constructor before your own fields are initialized. If you override
`onValueChanged()` and access subclass fields, guard against `null` at the top of
the override and call `refreshContext()` as the last line of your constructor.

### BasicAbsentContextUIAction\<I\>

Visible and enabled only when `I` is *absent* from context. Typical use: a
Login action visible before the user is authenticated.

```java
public class LoginAction extends BasicAbsentContextUIAction<UserInfo> {

    public LoginAction() { super(UserInfo.class); }

    @Override
    protected void executeAbsent() { showLoginDialog(); }

    @Override
    protected String getBaseIconSpec() { return "/icons/base/LOGIN_24.png"; }

    @Override
    public String getKey() { return "Login"; }
}
```

---

## Built-in actions

All live in `cz.bliksoft.javautils.app.ui.actions.basic` and are registered via
ServiceLoader (or `core/actions` in the XML filesystem).

| Key | Marker interface | Effect |
|---|---|---|
| `Save` | `ISave` | Calls `save()`; enabled by `getSaveEnabled()` |
| `SaveAll` | `ISaveAll` | Calls `saveAll()`; enabled by `getSaveAllEnabled()` |
| `Close` | `IClose` | Calls `close()`; enabled by `getCloseEnabled()` |
| `CloseAll` | `ICloseAll` | Calls `closeAll()` |
| `Delete` | `IDelete` | Calls `delete()`; enabled by `getDeleteEnabled()` |
| `Remove` | `IRemove` | Calls `remove()`; enabled by `getRemoveEnabled()` |
| `Add` | `IAdd` | Calls `add()`; enabled by `getAddEnabled()` |
| `AddSelect` | `IAddOptions` | Calls `addOptions()` |
| `Refresh` | `IRefresh` | Calls `refresh()` |
| `Reload` | `IReload` | Calls `reload()` |
| `Preview` | `IPreview` | Calls `preview()`; enabled by `getPreviewEnabled()` |
| `Print` | `IPrint` | Calls `print()`; enabled by `getPrintEnabled()` |
| `AppClose` | _(static)_ | Fires `TryCloseEvent` to close the application |
| `OpenAdministration` | `UserInfo` | Opens `AdministrationPanel`; enabled by `PermissionOpenAdministration` |
| `OpenLocalConfiguration` | `IConfigurable` | Calls `configure()`; enabled by `isConfigurable()` |
| `ShowAbout` | _(static)_ | Opens the About dialog (module versions + credits) |
| `ShowHelp` | _(static)_ | Opens the help URL in the browser; hidden when no URL is configured |
| `ContextHelp` | `IContextHelp` | Calls `openHelp()` on the current context object |

---

## Marker interfaces

The action interfaces live in `cz.bliksoft.javautils.app.ui.actions.interfaces`.
Implement them on any view component that supports that operation:

```java
public class OrderView extends BorderPane implements ISave, IDelete, IClose {

    @Override public void save()   { … }
    @Override public BooleanProperty getSaveEnabled() { return dirtyFlag; }

    @Override public void delete() { … }
    @Override public BooleanProperty getDeleteEnabled() { return hasSelectionProperty; }

    @Override public void close()  { BSAppUI.popUI(); }
    @Override public BooleanProperty getCloseEnabled() { return new SimpleBooleanProperty(true); }
}
```

Place an `OrderView` instance into context and the `Save`, `Delete`, `Close`
toolbar buttons automatically become enabled/visible without any glue code.

### `IContextHelp`

```java
public interface IContextHelp {
    void openHelp();
}
```

Implement on any view that wants to provide context-sensitive help. While that
view is in context the `ContextHelp` action becomes visible. See **Help.md** for
the full help system overview.
