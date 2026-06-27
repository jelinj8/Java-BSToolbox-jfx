# TreeEditor

`cz.bliksoft.javautils.fx.controls.editors.multivalue.TreeEditor<N>`

A reusable tree editor built on `VBox` for managing hierarchical, typed node structures. Node types are defined via `ITreeNodeType<N>`. Each type controls what child types are allowed, how nodes are displayed, and whether inline rename or dialog editing is supported.

## Layout

```
┌─────────────────────────────────────────────────────┐
│ [Title]  <spacer>  [+] [−] [...] [⎆] [👁]           │  ← toolbar
├─────────────────────────────────────────────────────┤
│ ▼ Root Node                                         │
│   ├─ Child A                                        │
│   │  └─ Grandchild 1                                │
│   ├─ Child B  ← selected                            │
│   └─ Child C                                        │
└─────────────────────────────────────────────────────┘
```

Toolbar buttons adapt dynamically based on the selected node's type:

| Button | Icon | Behavior |
|---|---|---|
| Add (simple) | `editor/add` | Shown when selected node allows exactly 1 child type |
| Add (split) | `editor/add` | `SplitMenuButton` shown when selected node allows 2+ child types |
| Remove | `editor/remove` | Always visible; disabled when nothing selected |
| Dialog ("...") | `editor/edit` | Auto-shown when selected node's type supports dialog editing |
| Item Action | (bound from action) | Shown when `setItemAction(IUIAction)` is called |
| Preview | `editor/preview` | Shown when `setPreviewAction(Runnable)` is called |


## Constructor

```java
new TreeEditor<>(typeResolver)
```

The `typeResolver` is a `Function<N, ITreeNodeType<N>>` that maps each node in the tree to its type definition. This is the central extension point — all behavior is delegated to the resolved `ITreeNodeType`.


## ITreeNodeType Interface

Every node in the tree is governed by an `ITreeNodeType<N>` implementation. This interface drives all behavior:

```java
public interface ITreeNodeType<N> extends ITitleProvider {

    // --- Display ---
    String getTypeName();                         // label in add-child menu
    String getDisplayText(N node);                // text in the tree cell
    Node createIcon(N node);                      // optional cell icon (default: checks IIconSpecPropertyProvider)

    // --- Type identification ---
    boolean matches(N node);                      // true if this type owns the node

    // --- Structure ---
    List<? extends ITreeNodeType<N>> childTypes(N parent);  // allowed child types (empty = leaf)
    List<N> getChildren(N node);                             // current children
    void addChild(N parent, N child);                        // model-level add
    void removeChild(N parent, N child);                     // model-level remove
    N create();                                              // factory for new nodes

    // --- Editing (all optional) ---
    IValueEditorProvider<N> inlineEditor();        // inline editor (null = no inline)
    boolean supportsDialog();                      // dialog support (default: delegates to inlineEditor)
    void showDialog(Window owner, N node);         // open dialog editor
    void onEditCommitted(N node);                  // hook after edit/dialog commit
}
```

### Minimal Implementation

At minimum, implement the required methods:

```java
public class FolderNodeType implements ITreeNodeType<MyNode> {

    @Override
    public String getTypeName() { return "Folder"; }

    @Override
    public String getDisplayText(MyNode node) { return node.getName(); }

    @Override
    public boolean matches(MyNode node) { return node.isFolder(); }

    @Override
    public List<? extends ITreeNodeType<MyNode>> childTypes(MyNode parent) {
        return List.of(new FolderNodeType(), new FileNodeType());
    }

    @Override
    public List<MyNode> getChildren(MyNode node) { return node.getChildren(); }

    @Override
    public void addChild(MyNode parent, MyNode child) { parent.getChildren().add(child); }

    @Override
    public void removeChild(MyNode parent, MyNode child) { parent.getChildren().remove(child); }

    @Override
    public MyNode create() { return new MyNode("New Folder", true); }
}
```

### Inline Editing

Override `inlineEditor()` to enable inline rename in the tree cell:

```java
@Override
public IValueEditorProvider<MyNode> inlineEditor() {
    return new IValueEditorProvider<>() {
        @Override
        public Node createEditor(ObjectProperty<MyNode> prop) {
            TextField tf = new TextField(prop.get().getName());
            tf.textProperty().addListener((obs, o, n) -> prop.get().setName(n));
            return tf;
        }

        @Override
        public String toDisplayString(MyNode value) {
            return value != null ? value.getName() : "";
        }

        @Override
        public MyNode fromString(String s) { return null; }

        @Override
        public void applyEdit(ObjectProperty<MyNode> prop) {
            // already applied via live binding
        }
    };
}
```

### Dialog Editing

For dialog-only types (no inline editor), override `supportsDialog()` and `showDialog()` directly:

```java
@Override
public IValueEditorProvider<MyNode> inlineEditor() {
    return null;  // no inline editing
}

@Override
public boolean supportsDialog() {
    return true;
}

@Override
public void showDialog(Window owner, MyNode node) {
    MyNodeDialog dialog = new MyNodeDialog(owner, node);
    dialog.showAndWait();
    // node properties are updated by the dialog
}
```

### Icons

By default, `createIcon()` checks if the node implements `IIconSpecPropertyProvider` and resolves the icon spec. Override for custom icons:

```java
@Override
public Node createIcon(MyNode node) {
    return ImageUtils.getIconNode("my-custom-icon");
}
```


## Configuration

### Setting the Root

```java
editor.setRoot(rootNode);
```

Clears the tree and recursively builds `TreeItem` nodes from the root using the type resolver. Children are discovered via `ITreeNodeType.getChildren(node)`.

### Title

```java
editor.setTitle("Document Structure");
editor.titleProperty().bind(someStringProperty);
```

Hidden automatically when empty.

### Preview Action

```java
editor.setPreviewAction(() -> {
    MyNode sel = editor.getSelectedItem();
    if (sel != null) showPreview(sel);
});
```

Shows the Preview button (also triggered by F3).

### Item Action (IUIAction)

```java
editor.setItemAction(myAction);
```

Binds a full `IUIAction` triggered by double-clicking a node. Shows a toolbar button with the action's icon, text, tooltip, and enabled state.

### Custom Toolbar Node

```java
editor.setLeadingToolbarNode(myNode);
```

Inserts a custom `Node` into the toolbar after the title label, before the spacer.


## Data Access

### Selection

```java
// Currently selected node (null if nothing selected)
MyNode node = editor.getSelectedItem();

// Path from root to selected node (empty list if nothing selected)
List<MyNode> path = editor.getSelectedPath();

// Select a specific node by reference
editor.setSelectedItem(myNode);

// Bind to selection changes
editor.selectedItemProperty().addListener((obs, old, newVal) -> { ... });
editor.selectedPathProperty().addListener((obs, old, newPath) -> { ... });
```

The `selectedPath` is an unmodifiable list built by walking `TreeItem.getParent()` from the selected item up to the root. Useful for breadcrumb displays or context-dependent actions.


## Keyboard Shortcuts

| Action | Key binding path | Default key |
|---|---|---|
| Add child | `multivalue-editors/add` | `Insert` |
| Remove node | `multivalue-editors/remove` | `Delete` |
| Preview | `multivalue-editors/preview` | `F3` |
| Activate node (edit/dialog) | (hardcoded) | `Enter` |
| Commit inline edit | (hardcoded) | `Enter` |
| Cancel inline edit | (hardcoded) | `Escape` |

Shortcuts are configurable via XmlFilesystem key bindings at `core/key-bindings/multivalue-editors/`.

**Note:** The Insert shortcut for adding a child only fires when the selected node's type allows exactly one child type. When multiple child types are available, use the SplitMenuButton in the toolbar.


## Dynamic Add Button

The add button adapts automatically when the selection changes:

| Allowed child types | Button state |
|---|---|
| 0 (leaf node) | Add button hidden |
| 1 | Simple add button — clicking creates a child of that type |
| 2+ | `SplitMenuButton` — first child type is the primary action, rest appear as menu items with type names and optional graphics |


## Editing Behavior

### Double-Click / Enter on a Node

1. If the node's type has an `inlineEditor()` → starts inline edit in the tree cell
2. Else if the node's type `supportsDialog()` → opens the dialog
3. Else if `itemAction` is set → fires the item action

### Inline Edit Mode

- Double-click or Enter starts editing
- Enter commits the edit (calls `IValueEditorProvider.applyEdit()` then `ITreeNodeType.onEditCommitted()`)
- Escape cancels the edit and reverts

### Dialog Button ("...")

Appears in the toolbar when the selected node's type supports dialog editing. Clicking it calls `ITreeNodeType.showDialog(owner, node)` and refreshes the tree.


## CSS Classes

- `ui-title` — applied to the title label
- `object-status-<name>` — dynamically applied to tree cells when nodes implement `IObjectStatusProvider`
- Custom CSS classes from nodes implementing `ICSSClassesProvider` are synchronized to cell style classes


## Complete Example

```java
// Define node types
ITreeNodeType<DocNode> folderType = new FolderNodeType();
ITreeNodeType<DocNode> documentType = new DocumentNodeType();
ITreeNodeType<DocNode> sectionType = new SectionNodeType();

// Type resolver
Function<DocNode, ITreeNodeType<DocNode>> resolver = node -> {
    if (folderType.matches(node)) return folderType;
    if (documentType.matches(node)) return documentType;
    if (sectionType.matches(node)) return sectionType;
    return null;
};

// Create editor
TreeEditor<DocNode> editor = new TreeEditor<>(resolver);
editor.setTitle("Document Structure");
editor.setRoot(rootFolder);

// React to selection
editor.selectedItemProperty().addListener((obs, old, node) -> {
    if (node != null) updateDetailPanel(node);
});

// Preview support
editor.setPreviewAction(() -> {
    DocNode sel = editor.getSelectedItem();
    if (sel != null) showPreview(sel);
});

parent.getChildren().add(editor);
```

```java
// Inline-editable tree with dialog support for complex nodes
public class ItemNodeType implements ITreeNodeType<Item> {

    @Override
    public IValueEditorProvider<Item> inlineEditor() {
        return new ItemNameEditor();  // simple rename
    }

    @Override
    public boolean supportsDialog() {
        return true;  // full property dialog
    }

    @Override
    public void showDialog(Window owner, Item node) {
        new ItemDialog(owner, node).showAndWait();
    }

    @Override
    public void onEditCommitted(Item node) {
        node.setModified(true);
    }

    // ... other required methods
}
```
