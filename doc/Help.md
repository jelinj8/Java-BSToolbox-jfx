# Help System

The help system provides three independent actions covering the common help entry
points: a browser-based manual, context-sensitive help, and an About dialog.

---

## Actions

### `ShowAboutAction`

Opens a modal About dialog containing two tabs rendered from FreeMarker templates:

| Tab | Template | Data variable |
|-----|----------|---------------|
| About | `About.ftl` | `data` — sorted `List<IModule>` (name + version) |
| Credits | `Credits.ftl` | `data` — map with `credits` and `licences` `FileObject` entries (from `BaseAppModule.xml`) |

The dialog is always visible and enabled.

### `ShowHelpAction`

Opens the application manual in the system browser. Visible and enabled only when
a help URL is configured; hidden otherwise.

Configure the URL in any module's XmlFilesystem XML:

```xml
<file name="core">
    <file name="ui">
        <file name="help">
            <attribute name="url" value="https://example.com/docs" />
        </file>
    </file>
</file>
```

The URL is resolved once at action construction time. Removing or not setting the
attribute means the action will never appear.

### `ContextHelpAction`

A `BasicContextUIAction<IContextHelp>` — visible only when an object implementing
`IContextHelp` is present in the current context. Calls `current.openHelp()`.

```java
public class OrderView extends BorderPane implements IContextHelp {

    @Override
    public void openHelp() {
        // open help for this specific view, e.g. navigate to an anchor in the manual
        app.getHostServices().showDocument("https://example.com/docs#orders");
    }
}
```

---

## `IContextHelp`

```java
public interface IContextHelp {
    void openHelp();
}
```

Implement this on any component that wants to surface a context-sensitive help
entry point. While the component is in context the `ContextHelp` toolbar button
becomes visible; when the component is removed from context it disappears.

---

## `HelpAboutPane`

`HelpAboutPane` is a `TabPane` used by `ShowAboutAction` as the dialog content.
It can be used directly when embedding the About content elsewhere.

### Template location

By default templates are loaded from the classpath path
`/cz/bliksoft/javautils/app/templates/help/`. Override by subclassing and
replacing the `FreemarkerGenerator` configuration, or by shadowing the classpath
entries in your application JAR.

### Translations in templates

Pass a `ResourceBundle` or a `Map<String, String>` to the constructor. The object
is registered as the `msg` variable and is accessible in both templates.

```java
// ResourceBundle — access via ${msg.getString("key")}
new HelpAboutPane(ResourceBundle.getBundle("com.example.HelpMessages"));

// Map — access via ${msg["key"]}
new HelpAboutPane(Map.of("title", "My App", "copyright", "© 2025 Example"));
```

No translations are registered when the no-arg constructor is used; accessing
`msg` in that case will cause a FreeMarker error, so omit `msg` references if you
use the framework's default templates without a bundle.

### Customising the dialog from `ShowAboutAction`

Subclass `ShowAboutAction` and override `createAboutPane()`:

```java
public class AppShowAboutAction extends ShowAboutAction {

    @Override
    protected HelpAboutPane createAboutPane() {
        return new HelpAboutPane(ResourceBundle.getBundle("com.example.HelpMessages"));
    }
}
```

Register the subclass in your module's XmlFilesystem instead of the framework class:

```xml
<file name="core">
    <file name="availableActions">
        <file name="com.example.AppShowAboutAction" />
    </file>
</file>
```

---

## i18n

Framework strings (tab labels, dialog title) live in
`cz.bliksoft.javautils.app.ui.help.BSAppHelpMessages`.
