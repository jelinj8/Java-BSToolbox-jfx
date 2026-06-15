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

Keyboard: Insert to add, Delete to remove, Enter to start/commit edit. The
Insert and Delete shortcuts are loaded from `core/key-bindings/multivalue-editors/add`
and `core/key-bindings/multivalue-editors/remove` respectively and can be
overridden per application (see **[UIActions.md — Key Bindings](UIActions.md#key-bindings)**).

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

When a registry is set, the key column uses a codebook popup, and the value
editor is switched automatically to match the key's declared type (falling back
to the default/string editor for keys not in the registry).

By default (`keysRestrictedToRegistry` = `true`), the popup *restricts* keys to
the registry's names. Set it to `false` to use the registry's names as
*suggestions only* — the popup offers them, but any typed key is accepted:

```java
editor.setKeysRestrictedToRegistry(false);
```

Keyboard: Insert to add, Delete to remove, F3 to preview (when a preview action is set),
Enter to start/commit edit. Shortcuts are loaded from `core/key-bindings/multivalue-editors/`
and can be overridden per application (see **[UIActions.md — Key Bindings](UIActions.md#key-bindings)**).

### TreeEditor\<V\>

Hierarchical tree editor. Nodes are added, removed, and previewed using the same
`multivalue-editors/add`, `multivalue-editors/remove`, and `multivalue-editors/preview`
key-binding paths as `ListEditor` and `KeyValueEditor`.

---

## CameraCaptureDialog

Dialog that captures an image from a connected webcam, a high-resolution OpenCV
device, or a network ("virtual") camera. Requires the
`com.github.sarxos:webcam-capture` library as a provided dependency.

```java
// Simple capture
WritableImage img = CameraCaptureDialog.capture(ownerWindow);

// Capture with initial image (for editing/replacing)
WritableImage img = CameraCaptureDialog.capture(ownerWindow, existingImage);

// Handsfree capture — no dialog shown. Camera, autocrop, and max-dimension
// downscaling are configured in the Cameras administration panel
// (CameraAdministrationProvider); resolution/rotation are loaded the same
// way as for the interactive dialog (per-camera saved preferences).
CameraCaptureDialog.captureHandsfree(onSuccess, onError);
```

The dialog presents a camera source selector (if more than one source is
available), a live preview, resolution/rotation controls, and crop handles. The
OK button is disabled until an image is captured.

### Camera sources (`cz.bliksoft.javautils.fx.controls.images.cam`)

The source combo lists every `ICameraSource` available:

- **`WebcamCameraSource`** — a physical webcam, enumerated at runtime via Sarxos
  `Webcam.getWebcams()`. If `org.bytedeco:opencv-platform` is available at
  runtime (BSToolbox-jfx declares it as `provided`), `OpenCvResolutionProbe` /
  `OpenCvCapture` are used to discover and capture resolutions beyond Sarxos's
  hardcoded 640x480 cap; otherwise Sarxos's own capture is used.
- **`NetworkCameraSource`** — an HTTP snapshot endpoint (e.g. a phone running an
  IP-camera app such as Android "IP Webcam"), captured via a plain HTTP GET +
  `ImageIO` (`NetworkCameraCapture`), no extra dependency required.

Network cameras are not auto-detected — they are registered declaratively as
`ICameraSource` singletons via the XmlFilesystem `/singletons` registry (see
`cz.bliksoft.javautils.xmlfilesystem.singletons.Singletons`), e.g. in the
application's local `settings.xml`:

```xml
<file name="singletons">
    <file name="warehouse-phone" type="cz.bliksoft.javautils.fx.controls.images.cam.NetworkCameraSource">
        <attribute name="name" value="Warehouse phone"/>
        <attribute name="url" value="http://192.168.1.50:8080/shot.jpg"/>
    </file>
</file>
```

`CameraCapturePane` drives resolution discovery, capture, and live preview
entirely through the `ICameraSource` interface — `getAvailableResolutions()`,
`grabFrame(Dimension)`, and `openPreview(Dimension)` (returning an
`ICameraPreviewSession`) — with no type-specific handling. Applications can
therefore implement their own `ICameraSource`, register it as a singleton the
same way as `NetworkCameraSource`, and it works in the source combo, capture,
preview, and handsfree capture without further changes.

`ICameraSource`, `ICameraPreviewSession`, `WebcamCameraSource`,
`NetworkCameraSource`, `NetworkCameraCapture`, `OpenCvCapture`, and
`OpenCvResolutionProbe` are all public, so applications can also reuse the
capture helpers directly.

---

## AnyImageLoader

`cz.bliksoft.javautils.fx.controls.images.AnyImageLoader` bridges `ImageUtils` to the BSToolbox `ImageLoader` SPI. Installed automatically by `BSApp.init()`. See [ImageUtils.md](ImageUtils.md#anyimageloader).
