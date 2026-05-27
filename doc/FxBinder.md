# FxBinder — Property Binding Utility

`FxBinder` bridges JavaFX `Property<V>` values to plain model getter/setter
pairs. It handles the initial synchronisation and, for bidirectional bindings,
propagates view changes back to the model.

All variants return a `BindingHandle` — a functional interface with a single
`dispose()` method that removes the listener when the binding is no longer needed.

---

## Signatures

```java
// Read-only: initialise view from model, no write-back
static <T> BindingHandle bindProperty(
    Property<T> prop,
    Supplier<? extends T> getter
)

// Read-only with type conversion (model type M → view type V)
static <V, M> BindingHandle bindProperty(
    Property<V> prop,
    Supplier<? extends M> getter,
    Function<? super M, ? extends V> toView
)

// Bidirectional, same type
static <T> BindingHandle bindProperty(
    Property<T> prop,
    Supplier<? extends T> getter,
    Consumer<? super T> setter
)

// Bidirectional with type conversion in both directions
static <V, M> BindingHandle bindProperty(
    Property<V>  prop,              // JavaFX view property
    Supplier<? extends M> getter,   // model getter
    Function<? super M, ? extends V> toView,  // M → V for initialisation
    Consumer<? super M> setter,     // model setter
    Function<? super V, ? extends M> toModel  // V → M on view change
)
```

`BindingHandle.dispose()` removes the `ChangeListener` from the property so the
binding does not leak memory when the view is discarded.

---

## Usage examples

### Simple read-only binding

```java
FxBinder.bindProperty(nameLabel.textProperty(), model::getName);
```

### Read-only with conversion

```java
FxBinder.bindProperty(
    amountLabel.textProperty(),
    model::getAmount,
    a -> a == null ? "" : NumberFormat.getCurrencyInstance().format(a)
);
```

### Bidirectional, same type

```java
BindingHandle h = FxBinder.bindProperty(
    nameField.textProperty(),
    model::getName,
    model::setName
);
```

### Bidirectional with conversion

```java
BindingHandle h = FxBinder.bindProperty(
    prioritySpinner.getValueFactory().valueProperty(),
    model::getPriority,
    Integer::valueOf,             // model int → view Integer (boxing)
    model::setPriority,
    Integer::intValue             // view Integer → model int
);
```

---

## Lifecycle

Collect handles and dispose on cleanup:

```java
private final List<BindingHandle> bindings = new ArrayList<>();

private void bind(Order order) {
    bindings.add(FxBinder.bindProperty(nameField.textProperty(), order::getName, order::setName));
    bindings.add(FxBinder.bindProperty(noteArea.textProperty(), order::getNote, order::setNote));
}

private void unbind() {
    bindings.forEach(BindingHandle::dispose);
    bindings.clear();
}
```

Call `unbind()` before `bind()` when switching to a different model object, or
in a `beforePop()` / close hook.

---

## Notes

- `bindProperty` calls `prop.setValue(toView.apply(getter.get()))` immediately —
  the view is in sync as soon as the binding is created.
- There is no debouncing or dirty tracking; every view change immediately calls
  the setter. If the setter is expensive (e.g. a network call), wrap it in a
  `Platform.runLater` or defer with a dirty flag.
- `FxBinder` does not use `JavaFX bidirectionalBind()`. Standard bidirectional
  bindings throw if either property is already bound; `FxBinder` avoids this
  restriction by using a plain `ChangeListener`.
- For read-only bindings the returned `BindingHandle.dispose()` is a no-op (no
  listener is registered).
