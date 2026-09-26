# Help System

The help system provides three independent actions covering the common help entry
points: a browser-based manual, context-sensitive help, and an About dialog.

---

## Actions

### `ShowAboutAction`

Opens a modal About dialog (`HelpAboutPane`) with two tabs - **About** (application
name and info, loaded modules with their versions) and **Credits** (the libraries and
resources the application uses, with their licences). Both are built from the
XmlFilesystem, see [`HelpAboutPane`](#helpaboutpane).

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

It is built from plain JavaFX controls - no WebView, so applications don't need
`javafx-web` (which `common-java-utils-jfx` declares only as optional). Links open in
the system browser.

### About tab

The application name (`BSApp.getAppName()`), the optional localized attributes of
`core/ui/about` and the loaded modules with their versions:

```xml
<file name="core">
    <file name="ui">
        <file name="about">
            <attribute name="description" value="Label designer for ZPL and TSPL printers" />
            <attribute name="copyright" value="Jakub Jelínek, © 2026" />
            <attribute name="url" value="https://example.com" />
        </file>
    </file>
</file>
```

### Credits tab

Every `lib_credits/*` entry. **Each module credits the third-party
libraries and resources it brings** (its runtime dependencies that ship with the
application) in its own XmlFilesystem XML; `BaseAppModule.xml` covers those of
`common-java-utils-jfx` itself (OpenJFX, ControlsFX, ZXing, JSVG, FreeMarker, Log4j, ...),
an application module adds the rest of its distribution's `lib/`:

```xml
<file name="lib_credits" sorted="true">
    <file name="jserialcomm">
        <attribute name="name" value="jSerialComm" />
        <attribute name="url" value="https://fazecast.github.io/jSerialComm/" />
        <attribute name="comment" value="serial port access" />
        <attribute name="licence" value="APACHE2,LGPL3" />
    </file>
</file>
```

The list is deduplicated and sorted by the XmlFilesystem itself: use the library's
**lowercase name as the id** - a library credited by several modules (e.g. jSerialComm by
an application and a plugin) merges into one entry - and declare `lib_credits` with
`sorted="true"` in **every** module (a merge doesn't copy the flag onto the node another
module created first), which orders the entries by id, i.e. alphabetically.

`licence` holds one or more comma-separated ids (a dual-licensed library lists each
option) of `licences/*` entries, which have a localized `name` and `url`.
`BaseAppModule.xml` defines `APACHE2`, `MIT`, `BSD2`, `BSD3`, `LGPL3`, `GPL2_CPE`
(GPLv2 with the Classpath Exception), `EPL2`, `EDL1`, `OFL11`, `CreativeCommons`
and `commercial`; a module can add more the same way.

### Customising the dialog from `ShowAboutAction`

Subclass `ShowAboutAction` and override `createAboutPane()`:

```java
public class AppShowAboutAction extends ShowAboutAction {

    @Override
    protected Node createAboutPane() {
        return new MyAboutPane();
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
