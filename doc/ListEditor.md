# ListEditor

`cz.bliksoft.javautils.fx.controls.editors.multivalue.ListEditor<V>`

A reusable single-column list editor built on `VBox`. Each item is displayed as text and edited inline on double-click or ENTER. The editor type is supplied via `IValueEditorProvider<V>`. The `items` list is a live `ObservableList<V>` that updates as the user edits.

## Layout

```
┌─────────────────────────────────────────────────────┐
│ [Title]  <spacer>  [+] [−] [↑] [↓] [✎] [⎆] [👁]   │  ← toolbar
├─────────────────────────────────────────────────────┤
│ Item 1                                              │
│ Item 2  ← selected                                  │
│ Item 3                                              │
│ ...                                                 │
└─────────────────────────────────────────────────────┘
```

The toolbar buttons appear/hide depending on configuration:

| Button | Icon | Visible by default | Controlled by |
|---|---|---|---|
| Add | `editor/add` | Yes | `setAddAction(null)` hides it |
| Remove | `editor/remove` | Yes | `setRemoveAction(null)` hides it |
| Move Up | `editor/move-up` | No | `setOrderingEnabled(true)` shows it |
| Move Down | `editor/move-down` | No | `setOrderingEnabled(true)` shows it |
| Edit | `editor/edit` | No | `setEditAction(Runnable)` shows it |
| Item Action | (bound from action) | No | `setItemAction(IUIAction)` shows it |
| Preview | `editor/preview` | No | `setPreviewAction(Runnable)` shows it |


## Constructors

```java
// Default constructor — uses StringEditorProvider
new ListEditor<String>()

// Custom value editor provider
new ListEditor<>(myProvider)
```

The no-arg constructor defaults to `StringEditorProvider`, so `ListEditor<String>` works out of the box.


## Configuration

### Value Editor Provider

The `IValueEditorProvider<V>` controls how values are displayed and edited inline. It can be set at construction time or changed dynamically via the property:

```java
editor.valueProviderProperty().set(new MyCustomProvider());
```

When the provider changes, the table refreshes all cells automatically.

Built-in providers (via `ValueEditorFactory.forType(Class)`):

| Type | Provider | Editor widget |
|---|---|---|
| `String` / `Object` / `null` | `StringEditorProvider` | `TextField` |
| `Integer` / `int` | `IntegerEditorProvider` | `TextField` (numeric) |
| `Double` / `double` | `DoubleEditorProvider` | `TextField` (decimal) |
| `Boolean` / `boolean` | `BooleanEditorProvider` | `CheckBox` |
| `LocalDate` | `LocalDateEditorProvider` | `DatePicker` |
| `LocalDateTime` | `LocalDateTimeEditorProvider` | Date/time editor |
| `Timestamp` | `TimestampEditorProvider` | Timestamp editor |
| Any enum | `EnumEditorProvider` | `ComboBox` |

### Add Item Supplier

Controls what value is inserted when the user clicks the Add button:

```java
// Insert a default value (e.g. empty string)
editor.setAddItemSupplier(() -> "");

// Insert from a dialog
editor.setAddItemSupplier(() -> showMyDialog());

// Return null to cancel the add (nothing is inserted)
editor.setAddItemSupplier(() -> userCancelled ? null : value);
```

Without a supplier, clicking Add inserts a `null` entry and immediately opens inline editing.

### Add Item Choices (SplitMenuButton)

When the list can contain multiple item types, configure add choices to show a `SplitMenuButton` instead of a plain button:

```java
editor.setAddItemChoices(List.of(
    new ListEditor.AddChoice<>(myTitleProvider1, () -> new TypeA()),
    new ListEditor.AddChoice<>(myTitleProvider2, () -> new TypeB())
));
```

- 1 choice: plain add button, calls that choice's factory
- 2+ choices: `SplitMenuButton` — first choice is the primary action, rest appear as menu items
- `null` or empty list: reverts to default plain add button

The `AddChoice<T>` record:
```java
public record AddChoice<T>(ITitleProvider title, Supplier<T> factory) {}
```

`ITitleProvider` supplies the display text (and optionally a graphic via `graphicProperty()`).

### Ordering (Drag-and-Drop)

```java
editor.setOrderingEnabled(true);
```

Enables:
- Move Up / Move Down buttons in the toolbar
- Drag-and-drop row reordering (drag a row and drop it on another position)

### Custom Toolbar Node

```java
editor.setLeadingToolbarNode(myNode);
```

Inserts a custom `Node` into the toolbar after the title label and before the spacer. Pass `null` to remove it.

### Title

```java
editor.setTitle("Items");
// or bind
editor.titleProperty().bind(someStringProperty);
```

The title label is hidden automatically when the title is empty.


## Action Hooks

### setAddAction / setRemoveAction

Replace the default add/remove behavior. Pass `null` to hide the button entirely:

```java
// Custom add behavior
editor.setAddAction(() -> {
    MyItem item = showCreationDialog();
    if (item != null) editor.addItem(item);
});

// Hide the remove button (items cannot be removed)
editor.setRemoveAction(null);
```

### setEditAction

Shows the Edit button and sets its handler:

```java
editor.setEditAction(() -> {
    MyItem selected = editor.getSelectedItem();
    if (selected != null) showEditDialog(selected);
});
```

### setPreviewAction

Shows the Preview button and sets its handler (also triggered by F3):

```java
editor.setPreviewAction(() -> {
    MyItem selected = editor.getSelectedItem();
    if (selected != null) showPreview(selected);
});
```

### setItemAction (IUIAction)

Binds a full `IUIAction` as the primary item action. This is triggered by double-clicking a row and shows a bound toolbar button:

```java
editor.setItemAction(myAction);
```

The button inherits the action's icon (via `IIconSpecPropertyProvider`), text, tooltip, and enabled state. It is disabled when nothing is selected or when the action itself is disabled.


## Data Access

### Loading Data

```java
// Replace all items from a collection
editor.loadFrom(List.of("a", "b", "c"));

// Add a single item programmatically
editor.addItem("d");
```

### Live Items List

```java
ObservableList<V> items = editor.getItems();

// Listen for changes
items.addListener((ListChangeListener<V>) change -> {
    // handle additions, removals, permutations
});
```

The `items` list is kept in sync bidirectionally with the internal table entries. Modifications to `items` from outside are reflected in the UI and vice versa.

### Selection

```java
// Get selected item (null if nothing selected)
V selected = editor.getSelectedItem();

// Get selected index (-1 if nothing selected)
int idx = editor.getSelectedIndex();

// Select a specific item
editor.setSelectedItem(myItem);

// Update the value of the selected entry without changing selection
editor.updateSelectedItem(newValue);

// Bind to selection changes
editor.selectedItemProperty().addListener((obs, old, newVal) -> { ... });
```

### Content-Based Sizing

```java
// Bind the editor's height to show all items without scrollbar
editor.prefHeightProperty().bind(editor.prefHeightForContent());
```

Returns a `DoubleBinding` that calculates: toolbar height + spacing + (row count * cell height).

### Refresh

```java
editor.refresh();
```

Forces all visible cells to re-render. Useful when the display representation of items has changed externally without the `ObjectProperty` value itself changing.


## Keyboard Shortcuts

All shortcuts are configurable via the XmlFilesystem key bindings at `core/key-bindings/multivalue-editors/`. The table shows the binding path and the hardcoded fallback:

| Action | Key binding path | Default key |
|---|---|---|
| Add item | `multivalue-editors/add` | `Insert` |
| Remove item | `multivalue-editors/remove` | `Delete` |
| Preview | `multivalue-editors/preview` | `F3` |
| Move up | `multivalue-editors/move-up` | `Alt+Up` |
| Move down | `multivalue-editors/move-down` | `Alt+Down` |
| Start inline edit | (hardcoded) | `Enter` |
| Commit edit | (hardcoded) | `Enter` |
| Cancel edit | (hardcoded) | `Escape` |


## Inline Editing Behavior

1. **Double-click** on a row starts inline editing (unless `itemAction` is set, in which case double-click fires the item action instead)
2. **Enter** on a selected row (not editing) starts inline editing
3. **Enter** while editing commits the change
4. **Escape** while editing cancels and reverts the value
5. When the editor provider's `dialogOnly()` returns `true`, double-click/Enter opens the dialog instead of inline editing


## CSS Classes

- `ui-title` — applied to the title label
- `object-status-<name>` — dynamically applied to cells when items implement `IObjectStatusProvider`
- Custom CSS classes from items implementing `ICSSClassesProvider` are synchronized to cell style classes


## Complete Example

```java
// Simple string list editor
ListEditor<String> tagEditor = new ListEditor<>();
tagEditor.setTitle("Tags");
tagEditor.setAddItemSupplier(() -> "new-tag");
tagEditor.loadFrom(List.of("alpha", "beta", "gamma"));

// Listen for changes
tagEditor.getItems().addListener((ListChangeListener<String>) c -> {
    System.out.println("Tags: " + tagEditor.getItems());
});

parent.getChildren().add(tagEditor);
```

```java
// Ordered list with custom types
ListEditor<MyItem> itemEditor = new ListEditor<>(new MyItemEditorProvider());
itemEditor.setTitle("Steps");
itemEditor.setOrderingEnabled(true);
itemEditor.setPreviewAction(() -> {
    MyItem sel = itemEditor.getSelectedItem();
    if (sel != null) showPreview(sel);
});
itemEditor.loadFrom(existingItems);

parent.getChildren().add(itemEditor);
```

```java
// Multi-type add with SplitMenuButton
ListEditor<Shape> shapeEditor = new ListEditor<>(new ShapeEditorProvider());
shapeEditor.setAddItemChoices(List.of(
    new ListEditor.AddChoice<>(() -> "Circle", () -> new Circle()),
    new ListEditor.AddChoice<>(() -> "Rectangle", () -> new Rectangle()),
    new ListEditor.AddChoice<>(() -> "Triangle", () -> new Triangle())
));
```
