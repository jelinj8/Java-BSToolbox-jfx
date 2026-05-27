# Validation

The validation framework decorates controls with error/warning/info badges and
tooltips, driven by a `ValidationResult` produced by your own validation logic.
Visuals are rendered via ControlsFX `Decorator`.

---

## Core types

### ValidationResultLevel

```java
public enum ValidationResultLevel { OK, INFO, WARN, ERROR }
```

`Comparator.naturalOrder()` orders these from least to most severe — `ERROR` is
the highest.

### ValidationMessage

```java
public record ValidationMessage(Object key, ValidationResultLevel level, String message) {}
```

`key` links a message to the control(s) that should be decorated. It may be any
object (an enum constant, a string, a field name) — the only requirement is that
`equals()` works correctly.

### ValidationResult

Aggregates a list of messages:

```java
ValidationResult vr = new ValidationResult();
vr.add(new ValidationMessage(Fields.NAME, ValidationResultLevel.ERROR, "Name is required."));
vr.add(new ValidationMessage(Fields.EMAIL, ValidationResultLevel.WARN, "Email looks unusual."));
```

---

## Applying results to a form

### Option 1 — tree traversal

Give each control a validation key and call `apply(root, vr)`:

```java
Validation.setKey(nameField,  Fields.NAME);
Validation.setKey(emailField, Fields.EMAIL);

Validation v = new Validation();
v.apply(formPane, validationResult);   // scans all nodes under formPane
```

`apply(Parent root, ValidationResult vr)`:
1. Collects all nodes under `root` that have a validation key.
2. Clears existing decorations.
3. Decorates matching nodes based on the result.

Multiple messages for the same key are merged: the highest level determines the
badge, all message texts appear in the tooltip as a bullet list.

### Option 2 — registry

Register controls explicitly (useful when the form root isn't a single parent,
or for dynamic forms):

```java
ValidationRegistry registry = new ValidationRegistry();
registry.register(nameField,  Fields.NAME);
registry.register(emailField, Fields.EMAIL);

Validation v = new Validation(registry);
v.apply(validationResult);
```

`ValidationRegistry` holds weak references so entries are collected automatically
when controls are garbage-collected.

### Clearing a single control

```java
v.clear(nameField);
```

---

## Visual output

Each decorated control receives:

- A small SVG badge at the bottom-right corner:
  - **ERROR** — triangle
  - **WARN** — triangle (styled differently via CSS)
  - **INFO** — circle
- A `Tooltip` on both the control and the badge icon, listing all messages for
  that key as `• text` lines.

Customize badge appearance via CSS classes:

```css
.validation-icon { … }
.validation-icon.validation-error { -fx-fill: red; }
.validation-icon.validation-warn  { -fx-fill: orange; }
.validation-icon.validation-info  { -fx-fill: steelblue; }
```

---

## IValidable

Marker interface for components that perform their own validation on demand:

```java
public interface IValidable {
    ValidationResult validate();
}
```

Implement on form components and call `validate()` before saving.

---

## Typical save flow

```java
private void onSave() {
    ValidationResult vr = validateForm();
    validation.apply(formPane, vr);

    if (vr.hasErrors())   // implement hasErrors() by checking ValidationResultLevel
        return;

    service.save(model);
    dirty.set(false);
}
```
