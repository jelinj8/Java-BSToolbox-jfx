# Administration Framework

The administration framework provides a plugin-extensible administration panel.
Providers register themselves via the XmlFilesystem; the panel renders them in a
navigable tree with switched content on the right.

---

## Opening the panel

`OpenAdministrationAction` pushes an `AdministrationPanel` onto the UI stack.
The action is a `BasicContextUIAction<UserInfo>` — it becomes visible when a
`UserInfo` is present in context, and is enabled only when
`userInfo.isAllowed(PermissionOpenAdministration.class)` returns `true`.

The panel may be open only once at a time; the action is a no-op when
`AdministrationPanel.isOpen()` returns `true`.

---

## Implementing a provider

Implement `cz.bliksoft.javautils.app.ui.interfaces.IAdministrationProvider`:

```java
public class MySettingsProvider implements IAdministrationProvider {

    @Override
    public String getKey() { return "mySettings"; }          // unique, used as properties prefix

    @Override
    public String getTreeTitle() { return "My Settings"; }   // tree label

    @Override
    public String getPanelTitle() { return "My Settings"; }  // panel header (defaults to getTreeTitle)

    @Override
    public Node getSmallIcon() { return ImageUtils.getIconNode("/icons/settings_16.png"); }

    @Override
    public Node getLargeIcon() { return ImageUtils.getIconNode("/icons/settings_24.png"); }

    @Override
    public Node getAdministrationComponent() { return myPanel; }
}
```

**Optional contracts the panel honours automatically:**

| Interface | Effect |
|-----------|--------|
| `IContextProvider` | Provider's `getItemContext()` is published into the panel's context hierarchy while the provider is active. |
| `ISave` | Save guard runs on provider switch and on panel close. |

---

## Registering a provider

Providers are declared in any module's XmlFilesystem XML under the path `core/administration`.
The file name is the fully-qualified class name; no extra attributes are required.

```xml
<!-- leaf provider -->
<file name="com.example.MySettingsProvider"/>
```

### Groups

A directory node creates a collapsible group. Its display name comes from the
filesystem localisation mechanism (`FileObject.getLocalizedName()`); the optional
`icon` attribute provides a small icon.

```xml
<file name="core">
    <file name="administration">
        <file name="myGroup" translation="MyModule.administration.group.myGroup">
            <attribute name="icon">/icons/group_16.png</attribute>
            <file name="com.example.MySettingsProvider"/>
            <file name="com.example.AnotherProvider"/>
        </file>
    </file>
</file>
```

Child order follows the standard XmlFilesystem ordering.

### Permission filtering

If `getRequiredPermission()` returns a non-null permission class, the provider
node is hidden unless `Permissions.isAllowed(...)` returns `true` for the current
user. Group nodes with no visible descendants are also hidden.

---

## Context structure

While the panel is open it publishes a two-level context:

```
levelContext  (level context)
  └── providerContextHolder  (SingleContextHolder)
        └── active provider's context  (if provider implements IContextProvider)
```

`AdministrationPanel.getItemContext()` returns `levelContext`. Actions bound to
controls inside the panel (or the standard toolbar) will see the active provider's
context automatically.

---

## Save guard

When the active provider's `getAdministrationComponent()` implements `ISave` and
`getSaveEnabled()` is `true`, switching to another provider or closing the panel
shows a confirmation dialog:

- **Provider switch / IClose** — three buttons: Save · Discard · Cancel
- **`beforePop()`** (externally triggered close) — two buttons: Save · Discard

Buttons use `BSButtonTypes.SAVE` and `BSButtonTypes.DISCARD` (pre-localised
constants from `BSButtonTypes`).

---

## UI state persistence

The panel's `SplitPane` divider position is saved to local properties via
`FxStateManager` under the key `AdministrationPanel.split.div`.

- **Restore**: `afterPush()` — runs after the panel is pushed onto the stack.
- **Save**: `beforePop()` — runs before the panel is removed.

---

## `IConfigurable`

```java
public interface IConfigurable {
    void configure();
    default boolean isConfigurable() { return true; }
}
```

`OpenLocalConfigurationAction` is a `BasicContextUIAction<IConfigurable>` that
calls `configure()` when an `IConfigurable` is in context. Bind it to a toolbar
button in any view that wants to expose a configuration entry point.

---

## i18n

Administration-related strings live in the dedicated bundle
`cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages`.
App- and plugin-defined providers are responsible for their own localisation.

---

## Built-in providers

Both are registered under `core/administration/framework` in `BaseAppModule.xml`
and require no permission.

### Modules (`ModulesAdministrationProvider`)

Displays all loaded modules (name, version, priority, enabled state).

The **Enabled** checkbox lets the user toggle modules on and off. Changes are
written immediately to the global properties file under the environment-qualified
key `{configName}.DisabledModules` (semicolon-separated class names). Force-enabled
modules (e.g. `BaseAppModule`) have a disabled checkbox and cannot be toggled.
Changes take effect after restart.

### Permissions (`PermissionsAdministrationProvider`)

Read-only table of all registered `Permission` subclasses, showing alias, name,
category, and description.
