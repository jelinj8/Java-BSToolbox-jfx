# Binding Principles

The framework uses three complementary binding mechanisms: **context-driven
action visibility** (see UIActions.md), **property binding via FxBinder** (see
FxBinder.md), and **direct JavaFX property bindings** for controls that need
to reflect model state.

---

## Context as a binding bus

The context system (from `common-java-utils`) is the primary bus that connects
views to actions without direct coupling.

When a component is pushed onto the UI stack, it places objects into the context.
Any `BasicContextUIAction<I>` listening on context for type `I` reacts
automatically — becoming visible, computing its enabled state from the context
object, and updating its icon overlay.

```
push OrderView onto UI stack
   → OrderView registers itself in context as ISave, IDelete, IClose
   → SaveAction (listening for ISave) becomes visible and binds its enabled state
     to OrderView.getSaveEnabled()
   → when OrderView is popped, all those actions become invisible again
```

Nothing in `OrderView` needs to know about `SaveAction`. Nothing in `SaveAction`
needs to know about `OrderView`. The context type is the contract.

---

## Binding a view to a model

For typical form/editor components, use `FxBinder` to connect JavaFX control
properties to model getters/setters (see **FxBinder.md**).

For read-only display, the simpler standard JavaFX bindings work well:

```java
nameLabel.textProperty().bind(person.nameProperty());
```

For bidirectional with type conversion:

```java
BindingHandle h = FxBinder.bindProperty(
    amountField.textProperty(),
    () -> model.getAmount(),
    amount -> amount == null ? "" : amount.toPlainString(),
    model::setAmount,
    text -> { try { return new BigDecimal(text); } catch (Exception e) { return null; } }
);
// later: h.dispose();
```

---

## Dirty flag pattern

The `ISave` marker interface requires `getSaveEnabled()` — a `BooleanProperty`
that is `true` when there are unsaved changes. The `SaveAction` binds to it
automatically. A common implementation:

```java
private final BooleanProperty dirty = new SimpleBooleanProperty(false);

@Override
public BooleanProperty getSaveEnabled() { return dirty; }
```

Mark dirty when the model is changed:

```java
nameField.textProperty().addListener((obs, o, n) -> dirty.set(true));
```

Clear after saving:

```java
@Override
public void save() {
    service.save(model);
    dirty.set(false);
}
```

---

## Action binding in XML

The `action` attribute in any XML UI description (handled by `UIComposer`)
performs the full lookup + binding:

```xml
<file name="Button">
    <attribute name="action">Save</attribute>
</file>
```

This is equivalent to:

```java
ActionBinder.bind(button, UIActions.getAction("Save"));
```

The binding is live: the button's disabled/visible/text/icon state tracks the
action's properties for the lifetime of the control.

---

## Observable collections

For lists that drive `TableView` or `ListView`, prefer `FXCollections
.observableArrayList()` with an extractor when row-level changes should trigger
cell updates:

```java
ObservableList<Order> orders = FXCollections.observableArrayList(
    o -> new Observable[] { o.statusProperty(), o.totalProperty() }
);
tableView.setItems(orders);
```

Without an extractor, only list-structural changes (add/remove) trigger a
`TableView` refresh; property changes on existing items are invisible.

---

## Context provider pattern

Components that want to expose state down the context hierarchy implement
`IContextProvider`:

```java
public class OrderEditorPanel extends BorderPane implements IContextProvider {

    private final Context itemContext = new Context("OrderEditor");

    public OrderEditorPanel(Order order) {
        itemContext.put(ISave.class, this);    // exposes ISave to actions
        itemContext.put(Order.class, order);   // exposes the order itself
    }

    @Override
    public Context getItemContext() { return itemContext; }
}
```

When `BSAppUI.pushUI(ctx, panel)` or the administration panel activates a
provider that implements `IContextProvider`, the provider's context is merged
into the active context hierarchy, making its objects visible to all context
listeners (actions, codebook fields, etc.).
