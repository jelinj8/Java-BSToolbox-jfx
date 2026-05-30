# Codebook Framework

A **codebook field** lets the user pick a typed value from a reference list (codebook / lookup table). It combines a text field with a select button and supports keyboard-driven resolution, live-filtered popups, and modal dialogs.

Package: `cz.bliksoft.javautils.fx.controls.codebooks`

---

## CodebookField\<T\>

The main control. Pair it with an `ICodebookProvider<T>` and bind its value property.

```java
CodebookField<Customer> field = new CodebookField<>(new CustomerCodebookProvider());
field.valueProperty().addListener((obs, o, n) -> ...);

// or bind bidirectionally to a model property
field.bindBidirectional(viewModel.customerProperty());
```

`StringCodebookField` is a typed convenience subclass for `String`-valued fields. It also
provides a zero-arg constructor with a demo list `["A","B","C"]` for use in a UI editor.

---

## Lock / unlock lifecycle

The field has two modes:

| Mode | Text field | Description |
|------|-----------|-------------|
| **Unlocked** | Editable | User types a search query or a raw value |
| **Locked** | Read-only | A confirmed value is displayed; editing is blocked |

Transitions:

- Setting a non-null value → **locks** (displays `toDisplayString(value)`)
- Setting `null` → **unlocks**
- `Backspace` while locked → unlocks, seeds the text field with `toEditString(lastValue)`, selects all
- `Delete` while locked → clears value and unlocks
- Successful resolution (Enter / Down / select button) → **locks**

```java
field.lock();
field.unlock();
field.clear();   // null value + unlock
field.isLocked();
```

---

## Keyboard shortcuts

| Key | Locked | Unlocked |
|-----|--------|---------|
| **Enter** | — | Attempt to resolve current text; lock on success, stay unlocked silently on failure |
| **Down** | — | Attempt to resolve; open popup/dialog on failure |
| **Select button** | Clear value | Attempt to resolve; open popup/dialog on failure |
| **Delete** | Clear + unlock | — |
| **Backspace** | Unlock, seed text | — |
| **Escape** | — | Close popup if open |

Enter and Down differ only in their failure behaviour: Enter stays silent (useful when the user typed a raw value like an iconspec); Down opens the selector (browse mode).

---

## ICodebookProvider\<T\>

Implement this interface to connect the field to a data source.

```java
public interface ICodebookProvider<T> {

    // Resolve text → single item, or null if not found / ambiguous.
    // refineIfNotUnique: hint to open a refinement dialog (currently unused by built-ins).
    T identify(String selectorText, boolean refineIfNotUnique);

    // String shown in the locked field and inside popup/dialog cells.
    String toDisplayString(T value);

    // String seeded into the text field when unlocking. Default: toDisplayString().
    default String toEditString(T value) { return toDisplayString(value); }

    // Create a Selector that calls onConfirm when the user picks an item.
    Selector<T> createSelector(Consumer<T> onConfirm);
}
```

### Selector types

`createSelector` must return one of:

| Type | Behaviour |
|------|-----------|
| `PopupSelector<T>` | CodebookField shows its `content()` in a `CodebookPopup` anchored below the field. If it also implements `IFilterableSelector`, the field's text is forwarded as a live filter. |
| `DialogSelector<T>` | Provider shows its own modal window; CodebookField calls `show(owner, initialFilterText)`. |

---

## BasicCodebookProvider\<T\>

Abstract base for flat and hierarchical providers. Subclassed by the built-in popup/dialog providers. Configure via public fields before attaching.

### Constructors

```java
// Flat list
new BasicCodebookProvider<>(List<T> items)
new BasicCodebookProvider<>(Supplier<List<T>> dataSource)      // re-evaluated on each call
new BasicCodebookProvider<>(Property<List<T>> itemsProperty)   // live property

// Hierarchical
new BasicCodebookProvider<>(T rootItem, Function<T, List<T>> childrenProvider, boolean showRoot)
```

### Configuration fields / setters

| Field | Default | Description |
|-------|---------|-------------|
| `dataSource` | set by constructor | `Supplier<List<T>>` — the item list |
| `childrenProvider` | `null` | `Function<T, List<T>>` — children for tree providers |
| `filter` | case-insensitive `contains` on `toString()` | `BiPredicate<T, String>` — filter predicate |
| `additionalFilter` | `null` | `Predicate<T>` — extra business-rule filter |
| `toDisplayString` | `Object::toString` | `Function<T, String>` |
| `toEditString` | `Object::toString` | `Function<T, String>` |
| `textProvider` | `null` | Override cell text (takes precedence over `toDisplayString` in cells) |
| `iconProvider` | `null` | `Function<T, Image>` — cell icon |
| `overlayPathProvider` | `null` | `Function<T, String>` — overlay icon path |
| `classProvider` | `null` | `Function<T, Set<String>>` — per-cell CSS classes |

`identify()` streams `dataSource`, applies `filter`, and returns the item if exactly one matches; `null` otherwise.

---

## Built-in providers

Six concrete providers cover the common shapes (list / table / tree) × (popup / dialog):

| Class | Selector | Layout |
|-------|----------|--------|
| `ListCodebookPopupProvider<T>` | Popup | `ListView` with live filter |
| `ListCodebookDialogProvider<T>` | Dialog | Filter field + `ListView` |
| `TableCodebookPopupProvider<T>` | Popup | `TableView` with configurable columns |
| `TableCodebookDialogProvider<T>` | Dialog | Filter field + `TableView` |
| `TreeCodebookPopupProvider<T>` | Popup | `TreeView` with live filter |
| `TreeCodebookDialogProvider<T>` | Dialog | Filter field + `TreeView` |

All extend `BasicCodebookProvider<T>` and inherit its configuration fields.

### Example — simple popup list

```java
var provider = new ListCodebookPopupProvider<>(customerSupplier);
provider.setToDisplayString(c -> c.getName());
provider.setFilterPredicate((c, text) ->
        c.getName().toLowerCase().contains(text.toLowerCase()));
provider.setIconProvider(c -> c.getAvatar());

CodebookField<Customer> field = new CodebookField<>(provider);
```

---

## IconCodebookPopupProvider

Specialist provider for picking icon files from the filesystem. Value type is `String` (an *iconspec*).

Package: `cz.bliksoft.javautils.fx.controls.codebooks.providers.basic`

### Iconspec format

| Example | Meaning |
|---------|---------|
| `[F]:icons/flags/svg/eu.svg` | File-based icon at the given path |
| `[F]:icons/flags/svg/eu.svg\|20` | Same file, rendered at 20 px |
| `save_16.png` | Classpath / ImageUtils-resolved icon |

The `[F]:` prefix marks a filesystem path. Everything after `|` is a size hint passed to the renderer; it is stripped before the path is used for file existence checks.

### Usage

```java
var provider = new IconCodebookPopupProvider(new File("icons"));
// or add multiple roots:
provider.addFolder(new File("extra-icons"));

// global roots shared by all instances (set at startup):
IconCodebookPopupProvider.globalRootFolders.add(new File("icons"));
```

The popup shows a filterable `TreeView` of the folders. Double-click or Enter confirms a file.
`toDisplayString` and `toEditString` both return the full iconspec (including the `|size` suffix if present) so the user can see and edit the complete spec.

### Workflow for adding size to a file icon

1. Field is locked showing `[F]:icons/flags/svg/eu.svg`.
2. Press **Backspace** → field unlocks, text seeded with the full iconspec.
3. Edit to `[F]:icons/flags/svg/eu.svg|20`.
4. Press **Enter** → `identify()` strips `|20`, confirms the file exists, locks with the new spec.

---

## IFilterableSelector

Popup selectors that support live filtering implement this interface:

```java
public interface IFilterableSelector {
    void setFilterText(String filterText);
}
```

`CodebookField` detects it via `popupSel.filterable()` and forwards every keystroke in the text field. The popup is responsible for filtering its own list/tree/table in response.

---

## Implementing a custom provider

```java
public class StatusCodebookProvider implements ICodebookProvider<Status> {

    @Override
    public Status identify(String text, boolean refineIfNotUnique) {
        return Arrays.stream(Status.values())
                .filter(s -> s.name().equalsIgnoreCase(text.trim()))
                .findFirst().orElse(null);
    }

    @Override
    public String toDisplayString(Status value) {
        return value == null ? "" : value.getLabel();
    }

    @Override
    public Selector<Status> createSelector(Consumer<Status> onConfirm) {
        return new ListCodebookPopupProvider<>(List.of(Status.values()),
                this::toDisplayString, onConfirm);
        // or build a custom PopupSelector / DialogSelector
    }
}
```
