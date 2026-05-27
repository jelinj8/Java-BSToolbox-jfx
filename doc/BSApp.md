# BSApp — Application Startup & Configuration

`BSApp` is the static hub for core application services: properties, session
management, and module lifecycle. `BSAppUI` is its UI-layer counterpart and is
the actual entry point for JavaFX applications.

---

## Entry point

A JavaFX application extends `javafx.application.Application` and calls
`BSAppUI.init()` from `start()`:

```java
public class MyApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BSAppUI.init(this, stage);
    }
}
```

`BSAppUI.init(app, stage)` registers `BSAppUI` as a module (so it participates
in the normal module lifecycle) and then delegates to `BSApp.init(app)`, which
drives the full module startup sequence: load → init → install.

### Running from an IDE

No special launcher is needed — run `MyApp` directly. The framework locates the
user settings directory automatically from the application name.

Set the application name **before** calling `BSAppUI.init()`:

```java
BSApp.setAppName("myapp");   // sets ~/.myapp/ and APPDATA/.myapp/ directories
BSAppUI.init(this, stage);
```

---

## Module startup sequence

`BSApp.init()` executes in this order:

1. Read global properties to find `moduleDir` and add its JARs to the classpath.
2. Read `EnabledModules` / `DisabledModules` from environment-qualified global
   properties and configure the `Modules` registry accordingly.
3. Determine the environment name from `app.configname` (default `"default"`).
4. Apply the locale from `{envName}.lang` (e.g. `default.lang=cs`).
5. Install the session manager (defaults to `DefaultUnrestrictedSessionManager`
   if none was set).
6. Call `Modules.loadModules()` → `initModules()` → `installModules()`.

The session manager must be set *before* `BSAppUI.init()`:

```java
BSApp.setSessionManager(new MySessionManager());
BSAppUI.init(this, stage);
```

---

## Properties

Two property files are maintained:

| Store | Location | Purpose |
|---|---|---|
| Global | `{app-dir}/.{appName}/{appName}.properties` | Shared/machine config |
| Local | `{user-home}/.{appName}/{appName}.properties` | Per-user overrides |

`BSApp.getProperty(key)` reads local first, falling back to global.
`BSApp.getProperty(key, default)` adds a further fallback.

Write helpers:

```java
BSApp.setLocalProperty("myKey", "value");      // user-level
BSApp.setGlobalProperty("myKey", "value");     // machine-level
BSApp.saveLocalProperties();
BSApp.saveGlobalProperties();
```

### Environment-qualified properties

Some keys are environment-scoped: stored as `{envName}.{key}`.
Use the `*EnvironmentProperty` variants:

```java
BSApp.setGlobalEnvironmentProperty("DisabledModules", "com.example.Foo");
String val = (String) BSApp.getEnvironmentProperty("DisabledModules", "");
```

---

## Module enable/disable

Modules are enabled or disabled at startup via global environment properties:

| Property | Default | Meaning |
|---|---|---|
| `EnabledModules` | `*` | Semicolon-separated class names, or `*` for all |
| `DisabledModules` | _(empty)_ | Semicolon-separated class names to skip |

`BaseAppModule` is always force-enabled regardless of these properties.

At runtime the `ModulesAdministrationProvider` writes `DisabledModules` and saves
the global properties file directly. Changes take effect after restart.

---

## Session manager

Provide a custom `SessionManager` to control which permissions the current user
has:

```java
public class MySessionManager extends SessionManager {
    @Override
    public UserInfo getUserInfo() { return myUserInfo; }
}
```

`DefaultUnrestrictedSessionManager` (the built-in default) grants all registered
permissions to every user.

---

## UI scene declaration

The main window layout is described in the XmlFilesystem under `/AppUI`. The
`root` attribute of that node names the sub-file that describes the scene (see
**BSAppUI.md** for details). The theme is declared there or overridden by the
`ui.theme` property.

---

## Packaging

The framework uses ServiceLoader extensively. Ensure `META-INF/services/`
entries exist for:

- `cz.bliksoft.javautils.modules.IModule` — your module classes
- `cz.bliksoft.javautils.app.ui.actions.IUIAction` — any actions registered
  via ServiceLoader
- `cz.bliksoft.javautils.app.permissions.Permission` — any permissions
  registered via ServiceLoader

Add the JavaFX modules to your module path. Platform-specific natives are
activated via Maven profiles (`windows`, `linux`, `mac`) using the `javafx-maven-plugin`
or by including the correct classifier JARs.

Typical `pom.xml` excerpt:

```xml
<profiles>
    <profile>
        <id>windows</id>
        <activation><os><family>windows</family></os></activation>
        <dependencies>
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-controls</artifactId>
                <classifier>win</classifier>
            </dependency>
            <!-- repeat for javafx-graphics, javafx-fxml, javafx-swing -->
        </dependencies>
    </profile>
    <!-- linux / mac profiles follow the same pattern -->
</profiles>
```
