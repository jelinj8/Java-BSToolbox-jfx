# Controls

Custom controls and utilities provided by the framework.

---

## ImageUtils — icon spec system

`cz.bliksoft.javautils.fx.tools.ImageUtils` — see **[ImageUtils.md](ImageUtils.md)** for full documentation.

```java
Image img  = ImageUtils.getImage("save_16.png");
ImageView  iv   = ImageUtils.getIconView("save.svg", 24.0);
Node       node = ImageUtils.getIconNode("[P]:M0 0 L10 10|16|16");  // SVGPath
```

---

## CodebookField\<T\>

A combined text field and selection button for picking values from a codebook
(lookup/reference list).

```java
CodebookField<Customer> field = new CodebookField<>(new CustomerCodebookProvider());
field.valueProperty().addListener((obs, o, n) -> …);
```

### ICodebookProvider\<T\>

Implement to supply the available values:

```java
public interface ICodebookProvider<T> {
    String toDisplayString(T value);
    void openSelector(Window owner, ObjectProperty<T> valueProperty);
    // optional: PopupSelector for in-field filtering
}
```

The provider opens a dialog or popup when the user clicks the button (or presses
F4 / Down). A `PopupSelector` implementing `IFilterableSelector` enables live
search in the popup.

### Keyboard shortcuts

| Key | Effect |
|---|---|
| F4 / Down | Open selector |
| Delete | Clear value |
| Backspace | Unlock (if locked) |
| Escape | Close popup |

### Read-only mode

```java
field.lock();    // display-only, shows the value but blocks selection
field.unlock();  // back to editable
```

---

## Value editors

### IValueEditorProvider\<V\>

Provides an inline editor node (and optionally a dialog) for a typed value:

```java
public interface IValueEditorProvider<V> {
    Node createEditor(ObjectProperty<V> valueProperty);  // inline editor
    String toDisplayString(V value);
    V fromString(String s);

    default boolean supportsDialog() { return false; }
    default void showDialog(Window owner, ObjectProperty<V> valueProperty) {}
    default String providerKey() { return getClass().getName(); }
}
```

### ValueEditorFactory

Resolves a built-in provider from a Java type:

```java
IValueEditorProvider<LocalDate> p = ValueEditorFactory.forType(LocalDate.class);
```

Built-in types: `String`, `Integer`, `Double`, `Boolean`, `LocalDate`,
`LocalDateTime`, `Timestamp`, any `enum`.

### ListEditor\<V\>

Single-column table editor for a list of values. Items are editable inline on
double-click or Enter. The underlying `ObservableList` (via `getItems()`) stays
in sync with edits.

```java
ListEditor<String> editor = new ListEditor<>();  // defaults to String provider
editor.setTitle("Tags");
editor.loadFrom(myList);

// Later, read back:
List<String> result = new ArrayList<>(editor.getItems());
```

Custom item type:

```java
ListEditor<Priority> editor = new ListEditor<>(new EnumEditorProvider<>(Priority.class));
```

Custom add behaviour (e.g. open a picker dialog):

```java
editor.setAddItemSupplier(() -> {
    // return new item, or null to cancel
    return showPickerDialog();
});
```

Keyboard: Insert to add, Delete to remove, Enter to start/commit edit.

### KeyValueEditor\<V\>

Two-column (key/value) table editor. The value column editor is resolved per-key
type when a `propertyRegistry` is set; otherwise the `defaultValueProvider` is
used for all rows.

```java
KeyValueEditor<String> editor = new KeyValueEditor<>();
editor.setTitle("Properties");
editor.loadFrom(myMap);

ObservableMap<String, String> live = editor.getValues();  // live sync
```

With a typed property registry:

```java
Map<String, Class<?>> registry = Map.of(
    "timeout",  Integer.class,
    "enabled",  Boolean.class,
    "baseUrl",  String.class
);
editor.propertyRegistryProperty().set(registry);
```

When a registry is set, the key column uses a codebook popup to restrict keys to
the defined names, and the value editor is switched automatically to match the
key's declared type.

---

## CameraCaptureDialog

Dialog that captures an image from a connected webcam. Requires the
`com.github.sarxos:webcam-capture` library as a provided dependency.

```java
// Simple capture
WritableImage img = CameraCaptureDialog.capture(ownerWindow);

// Capture with initial image (for editing/replacing)
WritableImage img = CameraCaptureDialog.capture(ownerWindow, existingImage);
```

The dialog presents a camera source selector (if multiple cameras are detected),
a live preview, and crop handles. The OK button is disabled until an image is
captured.

---

## AnyImageLoader

`cz.bliksoft.javautils.fx.controls.images.AnyImageLoader` bridges `ImageUtils` to the BSToolbox `ImageLoader` SPI. Installed automatically by `BSApp.init()`. See [ImageUtils.md](ImageUtils.md#anyimageloader).
