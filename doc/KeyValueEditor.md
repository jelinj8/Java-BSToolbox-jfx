# KeyValueEditor

`cz.bliksoft.javautils.fx.controls.editors.multivalue.KeyValueEditor<V>`

A reusable key/value table editor built on `VBox`. When a `propertyRegistry` is set, the key column restricts selection to defined property names using a codebook popup. The value column editor is resolved per-key type via `ValueEditorFactory`. The `values` map is a live `ObservableMap<String, V>` that updates as the user edits.

## Layout

```
┌───────────────────────────────────────────────┐
│ [Title]  <spacer>  [+] [−] [✎] [⎆] [👁]      │  ← toolbar
├───────────────────────────────────────────────┤
│ name         │ My Graph                       │
│ grid         │ DOT_GRID  ← selected           │
│ description  │ A sample graph                 │
└───────────────────────────────────────────────┘
```

The table has two columns (key and value) with the header row hidden. Toolbar buttons appear/hide depending on configuration:

| Button | Icon | Visible by default | Controlled by |
|---|---|---|---|
| Add | `editor/add` | Yes | `setAddAction(null)` hides it |
| Remove | `editor/remove` | Yes | `setRemoveAction(null)` hides it |
| Edit | `editor/edit` | Auto | Auto-shown when selected row's provider supports dialog; `setEditAction(Runnable)` overrides |
| Item Action | (bound from action) | No | `setItemAction(IUIAction)` shows it |
| Preview | `editor/preview` | No | `setPreviewAction(Runnable)` shows it |


## Constructors

```java
// No default value provider — falls back to StringEditorProvider
new KeyValueEditor<String>()

// Explicit default provider for values whose type is not in the registry
new KeyValueEditor<>(myDefaultProvider)
```


## Configuration

### Property Registry

The property registry maps key names to their value types. When set, it controls both the key column input and the value column editor:

```java
Map<String, Class<?>> registry = new LinkedHashMap<>();
registry.put("name", String.class);
registry.put("width", Integer.class);
registry.put("height", Integer.class);
registry.put("visible", Boolean.class);
registry.put("style", GridStyle.class);   // enum → ComboBox

editor.propertyRegistryProperty().set(registry);
```

The value column editor is resolved automatically from the registry's type via `ValueEditorFactory`:

| Type | Editor widget |
|---|---|
| `String` / `Object` / `null` | `TextField` |
| `Integer` / `int` | `TextField` (numeric) |
| `Double` / `double` | `TextField` (decimal) |
| `Boolean` / `boolean` | `CheckBox` |
| `LocalDate` | `DatePicker` |
| `LocalDateTime` | Date/time editor |
| `Timestamp` | Timestamp editor |
| Any enum | `ComboBox` |

### Key Input Modes

The key column adapts based on whether a registry is present and whether keys are restricted:

| Registry | `keysRestrictedToRegistry` | Key column behavior |
|---|---|---|
| Not set | — | Plain `TextField` (any key allowed) |
| Set | `true` (default) | `CodebookField` with popup — only registry keys selectable |
| Set | `false` | Editable `ComboBox` — registry keys offered as suggestions, any typed key accepted |

```java
// Allow free-text keys with suggestions from registry
editor.setKeysRestrictedToRegistry(false);
```

### Keys Editable

Controls whether the key column can be edited at all. Set to `false` when the set of keys is fixed and only values may be edited:

```java
editor.setKeysEditable(false);
```

### Inline Editing

Controls whether values can be edited inline in cells:

```java
// Disable inline editing — ENTER and double-click open dialog or fire editAction instead
editor.setInlineEditing(false);
```

When `inlineEditing` is `false`:
- Per-cell edit buttons are hidden
- ENTER / double-click delegates to the provider's dialog (if supported) or the configured `editAction`

### Default Value Provider

Fallback editor provider used when the key's type is not in the registry (or no registry is set):

```java
editor.defaultValueProviderProperty().set(myFallbackProvider);
```

### Type-Specific Provider Overrides

Override the automatically resolved editor for specific types:

```java
editor.getTypeProviders().put(GridStyle.class, myCustomGridStyleProvider);
```

When a key maps to `GridStyle.class` in the registry, the editor will use `myCustomGridStyleProvider` instead of the default `EnumEditorProvider`.

### Title

```java
editor.setTitle("Properties");
editor.titleProperty().bind(someStringProperty);
```

Hidden automatically when empty.

### Placeholder

```java
editor.setPlaceholderText("No properties defined");
```

Text shown when the table has no entries.

### Custom Toolbar Node

```java
editor.setLeadingToolbarNode(myNode);
```

Inserts a custom `Node` into the toolbar after the title label, before the spacer.


## Action Hooks

### setAddAction / setRemoveAction

Replace the default add/remove behavior. Pass `null` to hide the button entirely:

```java
// Hide add button (set of keys is fixed)
editor.setAddAction(null);

// Hide remove button
editor.setRemoveAction(null);
```

Default add behavior: inserts a blank row and starts editing its key column.
Default remove behavior: removes the selected row.

### setEditAction

Shows the Edit button and sets its handler. When `inlineEditing` is `false`, this action is also invoked on ENTER/double-click if the selected row's provider does not support a dialog:

```java
editor.setEditAction(() -> {
    String key = editor.getSelectedKey();
    V value = editor.getSelectedValue();
    if (key != null) showEditDialog(key, value);
});
```

When no explicit `editAction` is set, the Edit button appears/hides automatically based on whether the selected row's value provider supports a dialog.

### setPreviewAction

Shows the Preview button (also triggered by F3):

```java
editor.setPreviewAction(() -> {
    V selected = editor.getSelectedValue();
    if (selected != null) showPreview(selected);
});
```

### setItemAction (IUIAction)

Binds a full `IUIAction` as the primary item action, triggered by double-clicking a row:

```java
editor.setItemAction(myAction);
```

The button inherits the action's icon, text, tooltip, and enabled state.


## Data Access

### Loading Data

```java
// Replace all entries from a map
editor.loadFrom(Map.of("name", "Graph1", "width", 800));
```

### Live Values Map

```java
ObservableMap<String, V> values = editor.getValues();

// Listen for changes
values.addListener((MapChangeListener<String, V>) change -> {
    if (change.wasAdded()) {
        String key = change.getKey();
        V val = change.getValueAdded();
        // handle change
    }
});
```

The map is synchronized bidirectionally with the table entries. Key changes remove the old key and add the new key. Empty/blank keys are not added to the map.

### Selection

```java
// Get selected value
V value = editor.getSelectedValue();

// Get selected key
String key = editor.getSelectedKey();

// Update value of selected entry without changing selection
editor.updateSelectedValue(newValue);

// Bind to selection changes
editor.selectedValueProperty().addListener((obs, old, newVal) -> { ... });
```

### Refresh

```java
editor.refresh();
```

Forces all visible cells to re-render.


## Keyboard Shortcuts

| Action | Key binding path | Default key |
|---|---|---|
| Add row | `multivalue-editors/add` | `Insert` |
| Remove row | `multivalue-editors/remove` | `Delete` |
| Preview | `multivalue-editors/preview` | `F3` |
| Start edit | (hardcoded) | `Enter` |
| Commit edit | (hardcoded) | `Enter` |
| Cancel edit | (hardcoded) | `Escape` |

Shortcuts are configurable via XmlFilesystem key bindings at `core/key-bindings/multivalue-editors/`.


## Inline Editing Behavior

### Key Column
1. Double-click or Enter on an empty/selected key starts editing
2. Input mode depends on registry configuration (see Key Input Modes above)
3. On commit, focus moves to the value column

### Value Column
1. Double-click or Enter on the value starts inline editing (if `inlineEditing` is `true`)
2. The editor widget is resolved from the key's type via the property registry
3. If the provider is `dialogOnly()`, double-click/Enter opens the dialog instead
4. A dialog button ("...") appears in the cell when the provider supports dialog editing

### Non-Inline Mode
When `inlineEditing` is `false`:
1. Enter / double-click opens the provider's dialog (if supported)
2. Falls back to the configured `editAction`
3. Per-cell edit buttons are hidden


## CSS Classes

- `ui-title` — applied to the title label
- `object-status-<name>` — dynamically applied to value cells when values implement `IObjectStatusProvider`
- Custom CSS classes from values implementing `ICSSClassesProvider` are synchronized to cell style classes


## Complete Example

```java
// Typed property editor with restricted keys
Map<String, Class<?>> registry = new LinkedHashMap<>();
registry.put("name", String.class);
registry.put("grid", GridStyle.class);
registry.put("snapToGrid", Boolean.class);
registry.put("gridSpacing", Double.class);

KeyValueEditor<Object> editor = new KeyValueEditor<>();
editor.setTitle("Graph Properties");
editor.propertyRegistryProperty().set(registry);
editor.setKeysRestrictedToRegistry(true);
editor.setKeysEditable(false);
editor.setAddAction(null);     // no adding — fixed set of keys
editor.setRemoveAction(null);  // no removing

// Pre-populate
Map<String, Object> data = new LinkedHashMap<>();
data.put("name", "My Graph");
data.put("grid", GridStyle.DOT_GRID);
data.put("snapToGrid", true);
data.put("gridSpacing", 10.0);
editor.loadFrom(data);

// React to changes
editor.getValues().addListener((MapChangeListener<String, Object>) change -> {
    if (change.wasAdded()) {
        applyProperty(change.getKey(), change.getValueAdded());
    }
});

parent.getChildren().add(editor);
```

```java
// Free-form key/value editor with string values
KeyValueEditor<String> editor = new KeyValueEditor<>();
editor.setTitle("Environment Variables");
editor.loadFrom(System.getenv());

parent.getChildren().add(editor);
```

```java
// Custom provider override for a specific type
KeyValueEditor<Object> editor = new KeyValueEditor<>();
editor.propertyRegistryProperty().set(registry);
editor.getTypeProviders().put(Color.class, new ColorPickerEditorProvider());
```
