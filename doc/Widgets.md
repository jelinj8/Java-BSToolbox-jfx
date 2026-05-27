# Configurable Widgets

`cz.bliksoft.javautils.app.ui.widgets`

The widget system provides resizable, user-configurable display slots ("`WidgetContainer`s") into which plugins can plug small components ("widgets"). The user picks a widget from a categorised dialog; the choice is persisted per-slot across sessions.

---

## Core interfaces

### `IWidget`

```java
public interface IWidget {
    String getTitle();
    Node getComponent();             // the JavaFX node to embed
    default ContextMenu getContextMenu() { return null; }
    default void cleanup() {}        // called when the widget is removed
}
```

Implement this for each concrete widget component. `cleanup()` should unregister listeners and release resources.

### `IWidgetFactory`

```java
public interface IWidgetFactory {
    String getName();            // unique key — typically the factory's FQCN
    String getCategory();        // grouping label in the selector tree
    String getLocalizedName();   // display label in the selector dialog
    default double getMinWidth()  { return 0; }  // 0 = unconstrained
    default double getMinHeight() { return 0; }
    IWidget create();
}
```

The factory is a singleton-style descriptor. `getName()` is the persistence key; changing it invalidates previously saved user choices.

---

## WidgetContainer

`WidgetContainer` extends `StackPane`. Place it in a layout wherever a user-configurable slot is needed.

```java
WidgetContainer slot = new WidgetContainer();
slot.setPlacementGroup("mainDashboard");   // logical grouping
slot.setPlacementID("topLeft");            // unique within the group — triggers load()
slot.setDefaultWidgetFactoryName("com.example.ClockWidgetFactory");
slot.setConstrainedMaxWidth(300);          // only show factories that fit
slot.setConstrainedMaxHeight(200);
// slot.setConfigurable(false);            // lock — no context menu for the user
```

Setting **both** `placementGroup` and `placementID` (in any order) immediately triggers `load()`, which restores the previously persisted widget or falls back to the default factory.

### Persistence

Widget choices are stored in local properties (`BSApp.getLocalProperties()`) under the key:

```
widget.{placementGroup}.{placementID}
```

Three-state semantics:

| Stored value | Meaning |
|---|---|
| *(absent)* | No user choice — use `defaultWidgetFactoryName` if set, otherwise empty |
| `""` (empty string) | User explicitly removed the widget — keep the slot empty |
| factory name | Load this factory and install its widget |

### Size filtering

`constrainedMaxWidth` / `constrainedMaxHeight` (default 0 = no constraint) filter the selector dialog: a factory is shown only if `factory.getMinWidth() <= constrainedMaxWidth` (when > 0) and `factory.getMinHeight() <= constrainedMaxHeight` (when > 0).

### Context menu

Right-clicking a `WidgetContainer` (when `configurable = true`, the default) shows a dynamic context menu:

| Container state | Menu items |
|---|---|
| Empty | *Add widget* |
| Filled | *Replace widget*, *Remove widget* |
| Either + default set | + *Reset to default* |
| Filled, not configurable | widget's own `getContextMenu()` (if any) |
| Empty, not configurable | *(no menu)* |

*Remove widget* persists an explicit empty choice so the default is not re-applied on next load. *Reset to default* deletes the persisted key and calls `load()` again.

### Programmatic widget set

```java
slot.setContents(myWidget);   // installs widget; saves if both keys set
slot.setContents(null);       // same as remove (does NOT save explicit-empty)
```

Note: `setContents` is for cases where the caller provides a widget instance directly. Because no factory is involved, the slot cannot persist a factory name — the widget will not be restored after a restart. For a fully persistent slot, go through the selector dialog or ensure the factory is configured as the default.

---

## Registering a factory (plugin side)

Add an entry to your module's XML resource under the `core/widgets` filesystem path:

```xml
<file name="core">
    <file name="widgets">
        <file name="com.example.widgets.ClockWidgetFactory"/>
    </file>
</file>
```

The entry name must be the **fully-qualified class name** of an `IWidgetFactory` implementation that has a no-argument constructor. The factory is loaded via `FileObjectClassLoader` on the first `selectWidget()` call (lazy, once per JVM lifetime).

The framework itself ships no built-in widgets; the `core/widgets` folder is declared empty in `BaseAppModule.xml` so the path always exists for plugins to contribute to.

---

## Factory discovery lifecycle

Factories are loaded lazily, once, into a shared `static` map keyed by category. Discovery happens automatically the first time `selectWidget()` is called on any `WidgetContainer`. The map is never invalidated at runtime — factories registered while the app is running are not picked up until the next JVM start.

---

## Implementing a widget

```java
public class ClockWidgetFactory implements IWidgetFactory {
    @Override public String getName()          { return getClass().getName(); }
    @Override public String getCategory()      { return "Time"; }
    @Override public String getLocalizedName() { return Messages.get("clock.name"); }
    @Override public IWidget create()          { return new ClockWidget(); }
}

public class ClockWidget implements IWidget {
    private final Label label = new Label();
    private final Timeline timeline = ...;

    @Override public String getTitle()     { return "Clock"; }
    @Override public Node getComponent()   { return label; }
    @Override public void cleanup()        { timeline.stop(); }
}
```
