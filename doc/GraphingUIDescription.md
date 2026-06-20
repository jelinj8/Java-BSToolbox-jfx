# Graph Editor Framework — Requirements

Requirements and design concepts for a visual graph editing framework.

---

## 1. Overview & Vision

A two-library framework for visual graph editing and executable workflow processing:

- **BSDataFlow** — new library. Contains the graph data model (pure data classes, JAXB-serializable) and a headless processing engine. No JavaFX dependency. Depends on BSToolbox (`common-java-utils`) and JDK 21. Can run standalone, in Spring Boot, or as a JMS consumer.
- **BSToolbox-jfx** — graph canvas, node/edge renderers, property editors, node palette, interaction layer. Depends on BSDataFlow for the model classes.

### Dependency graph

```
BSToolbox (common-java-utils)
    ↑               ↑
BSDataFlow          BSToolbox-jfx
(model +            (graph UI, canvas,
 processing          palette, editors)
 engine)                 ↑
    ↑        ┌───optional─┘
    ↑        ↓
    └── Application ──┘
```

BSToolbox-jfx declares BSDataFlow as an **optional Maven dependency**. The graph editor components require BSDataFlow on the classpath (it provides the model classes). All other BSToolbox-jfx controls (editors, codebook fields, images, etc.) work without BSDataFlow. BSToolbox itself has no dependency on either library.

### Two usage modes

**Mode 1 — Visual Diagramming.** UML charts, workflow documentation, decision trees, communication diagrams, mind maps. The graph model is the deliverable. No execution semantics.

**Mode 2 — Executable Processing.** Visual designer for data processing workflows (like Tibco BusinessWorks, Talend, Apache Camel). Nodes are linked to `INodeProcessor` implementations. The graph model is input to a runtime engine (`GraphExecutor`).

### Architectural principle

The graph model is the shared contract, owned by BSDataFlow. BSToolbox-jfx owns visual representation and interaction. BSDataFlow owns the model, execution semantics, and runtime. The model is fully serializable/deserializable with only BSDataFlow present (no JFX needed).

---

## 2. Core Terminology

| Term | Definition |
|---|---|
| **Graph** | A collection of Nodes connected by Edges. Has a UUID, name, version, and metadata. |
| **Node** | A positioned element with a type, join points, and properties. Primary interactive element. |
| **Edge** | A connection between two JoinPoints on (possibly the same) Node. Carries directionality, visual type, and properties. |
| **JoinPoint** | A connection port on a Node where Edges attach. Has position, direction (IN/OUT/INOUT), and cardinality limits. Synonym: Port, Anchor. |
| **Group** | A subset of Nodes and Edges treated as a unit. Has its own JoinPoints (interface). Can be displayed expanded or collapsed. Synonym: Subgraph, Compound Node. |
| **Canvas** | The infinite scrollable/zoomable surface hosting the visual graph. One canvas displays one graph scope (root or group interior). |
| **Property** | A typed key-value pair on a Node, Edge, JoinPoint, or Group. Extensibility mechanism: types define their property schema, editors bind to properties. |
| **NodeType** | A registered definition describing a category of Node: visual representation, available JoinPoints, property schema, and optionally an associated `INodeProcessor`. |
| **EdgeType** | A registered definition describing a category of Edge: visual representation, allowed cardinality, directionality, and property schema. |
| **Palette** | A categorized panel listing available NodeTypes, draggable onto the Canvas. |

---

## 3. Library Split

### BSDataFlow (new library)

**Graph model** — pure data classes with JAXB annotations:
- `Graph`, `Node`, `Edge`, `JoinPoint`, `Group`
- Enums: `Direction`, `Directionality`, `JoinPointPosition`
- `PropertySchema`, `PropertyDefinition` — type metadata for node/edge properties
- No JavaFX properties, no observable patterns

**Processing engine** — headless workflow execution:
- `INodeProcessor` — per-node-type processing logic
- `GraphExecutor` — traverses graph, invokes processors
- `ProcessingContext` — carries data, routing, error handling, state
- `GraphInstance` — runtime state of a running graph

**Deployment modes:**
- Standalone (main class, triggered programmatically)
- Spring Boot embedded (REST/SOAP endpoint)
- JMS consumer (triggered by incoming messages)
- Scheduled (cron-based execution)

### BSToolbox-jfx (graph UI additions, optional dependency on BSDataFlow)

BSDataFlow is an optional Maven dependency (`<optional>true</optional>`). The graph editor components require it at runtime. All other BSToolbox-jfx features work without BSDataFlow on the classpath.

**Canvas & rendering:**
- `GraphCanvas` — infinite scrollable/zoomable pane
- `INodeRenderer` — produces JavaFX visuals for a node type
- `IEdgeRenderer` — renders edge visuals (line styles, arrows)
- Default renderers for common shapes

**Editors & interaction:**
- Node/edge property panel (driven by `IValueEditorProvider`)
- Selection management, move/resize, alignment, undo/redo
- Connection creation (drag between JoinPoints)

**Palette:**
- Categorized, searchable panel of available NodeTypes
- Drag-and-drop to canvas

**Integration:**
- `IUIAction` implementations for graph operations
- `FxStateBinder` for canvas state persistence
- Context/Event integration for selection changes

---

## 4. Graph Data Model

All model classes live in BSDataFlow. Every element carries a stable UUID (v4 via `RandomUUIDCreator`), assigned at creation, never regenerated.

### Node

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Stable, immutable after creation |
| `typeId` | `String` | References a registered NodeType |
| `x`, `y` | `double` | Position in graph coordinate space |
| `width`, `height` | `double` | Size (may be constrained by NodeType) |
| `properties` | `Map<String, Object>` | Typed property bag, schema defined by NodeType |
| `joinPoints` | `List<JoinPoint>` | The node's connection ports |
| `parentGroupId` | `UUID` | Null for top-level nodes, non-null if inside a Group |
| `zOrder` | `int` | Layering within the canvas |

### Edge

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Stable identifier |
| `typeId` | `String` | References a registered EdgeType |
| `sourceJoinPointId` | `UUID` | Origin JoinPoint |
| `targetJoinPointId` | `UUID` | Destination JoinPoint |
| `directionality` | `Directionality` | `UNIDIRECTIONAL` or `BIDIRECTIONAL` |
| `waypoints` | `List<Point2D>` | Intermediate routing points for curves/bends |
| `properties` | `Map<String, Object>` | Typed property bag |

### JoinPoint

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Stable identifier |
| `name` | `String` | Human-readable label (e.g. "input", "output", "error") |
| `position` | `JoinPointPosition` | Enum (`TOP`, `BOTTOM`, `LEFT`, `RIGHT`, `TOP_LEFT`, etc.) or custom (`relativeX%`, `relativeY%`) |
| `direction` | `Direction` | `IN`, `OUT`, or `INOUT` |
| `maxConnections` | `int` | -1 for unlimited, 1 for exactly one, etc. |
| `compatibleEdgeTypes` | `String[]` | Which EdgeTypes can connect here (null = any) |

### Group

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Stable identifier |
| `name` | `String` | Human-readable label |
| `memberNodeIds` | `Set<UUID>` | Nodes belonging to this group |
| `memberEdgeIds` | `Set<UUID>` | Edges internal to this group |
| `exposedJoinPoints` | `List<JoinPoint>` | The group's external interface |
| `joinPointMapping` | `Map<UUID, UUID>` | Maps exposed JoinPoints to internal node JoinPoints |
| `collapsed` | `boolean` | Whether displayed as a single node |
| `x`, `y`, `width`, `height` | `double` | Position/size when collapsed |

### Graph (root)

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Stable identifier |
| `name` | `String` | Graph name |
| `version` | `int` | Schema version for migration support |
| `nodes` | `List<Node>` | All nodes |
| `edges` | `List<Edge>` | All edges |
| `groups` | `List<Group>` | All groups |
| `metadata` | `Map<String, Object>` | Graph-level properties (author, created date, description) |

### Design rules

- JoinPoints are **owned by Nodes**, not shared. Deleting a Node deletes its JoinPoints and disconnects (or deletes) connected Edges.
- Edges reference JoinPoints **by UUID**, not by Node+index. JoinPoint reordering does not break connections.
- Properties use `Map<String, Object>` at model level, constrained by NodeType/EdgeType property schema at editor level. Unknown properties are preserved (forward compatibility).
- The model is **pure data** — no JavaFX properties, no observable patterns. The UI layer wraps models in observable beans (`IStatusBean` / `SwitchableBeanHolder`).

---

## 5. Node System

### Type registry

NodeTypes are registered via three mechanisms (mirroring existing BSToolbox patterns):

1. **XmlFilesystem** — `FileObject` nodes under a defined path (e.g. `graph/nodeTypes/`) loaded by `GraphNodeTypeFileLoader`
2. **ServiceLoader** — `META-INF/services` SPI for `INodeTypeProvider`
3. **Programmatic** — `NodeTypeRegistry.register(nodeType)` during module `install()`

NodeTypes are immutable after registration. The registry is populated during the module `install()` phase.

### NodeType definition

Each NodeType provides:
- `String typeId` — unique identifier
- `String displayName` — human-readable name
- `String category` — for palette grouping (e.g. "Flowchart", "Control Flow", "Data")
- `String iconSpec` — for palette icon (using BSToolbox-jfx icon spec system)
- `INodeRenderer` — visual representation factory
- `List<JoinPointDefinition>` — default join points for new instances of this type
- `PropertySchema` — property definitions with types, defaults, constraints, and optionally custom `IValueEditorProvider` instances

### Extensible join points

- A NodeType defines **default JoinPoints** (positions, directions, cardinality).
- Individual Node instances can **dynamically add/remove** JoinPoints (e.g. a "Splitter" node adds output JoinPoints as needed).
- **Positions**: predefined enum values (edge midpoints, corners) computed relative to node bounds. Custom positions use `(percentX, percentY)` relative to node bounds.
- **Visual indicators**: small shapes (circles, squares) on node edges, rendered by the framework. Appearance configurable per direction type (color, shape, size).

### Visual representation

- Each NodeType provides an `INodeRenderer` that produces a JavaFX `Region` (or `Pane`).
- The renderer receives the node's model and a `RenderContext` (zoom level, selection state, theme).
- Renderers are responsible for: shape drawing, label rendering, icon placement. JoinPoint indicators are rendered by the framework, not the renderer.
- CSS-themeable — renderers use JavaFX CSS for styling so themes/skins apply globally.
- Framework provides base renderers:
  - `ShapeNodeRenderer` — simple geometric shape + label
  - `IconNodeRenderer` — icon + label
  - `CompositeNodeRenderer` — header bar / body / footer layout

### Built-in flowchart types

| Type | Shape | Default JoinPoints |
|---|---|---|
| Start/End | Rounded rectangle | 1 OUT (Start) or 1 IN (End) |
| Process | Rectangle | 1 IN (left), 1 OUT (right) |
| Decision | Diamond | 1 IN (top), 2+ OUT (sides, labeled "yes"/"no") |
| Connector | Circle | 1 IN, 1 OUT |
| IO | Parallelogram | 1 IN, 1 OUT |
| Annotation | Open bracket | 1 INOUT |
| Text Box | Rectangle (no border) | None |

Domain-specific standards (BPMN, UML activity) are implemented as extension modules, not built into the framework.

### Property editors

- When a node is selected, a property panel shows its editable properties.
- Uses the existing `IValueEditorProvider<V>` / `ValueEditorFactory` pattern.
- Standard types (String, Integer, Boolean, Enum, LocalDate, etc.) use built-in providers.
- NodeTypes can register **custom providers** for specific properties (e.g. an SQL editor for a JDBC node).
- NodeTypes can provide a completely **custom editor panel** (`INodeConfigEditor`) replacing the generic property form.

---

## 6. Edge/Connection System

### Edge types

| Visual type | Description |
|---|---|
| Straight line | Direct line between JoinPoints (default) |
| Orthogonal routing | Horizontal/vertical segments with right-angle bends |
| Bezier curve | Smooth curve with editable control points |
| Editable polyline | User-defined waypoints with straight segments |

Edge visual type is a **per-edge property**. EdgeType registrations can constrain which visual types are allowed.

### Cardinality

Enforced via `JoinPoint.maxConnections`:
- **One-to-one**: both source and target JoinPoints have `maxConnections=1`
- **One-to-many**: source unlimited (-1), target limited (1)
- **Many-to-many**: both unlimited

The canvas **prevents creating connections** that violate cardinality constraints.

### Directionality

- **Unidirectional**: arrow on target end. Source and target are semantically distinct.
- **Bidirectional**: arrows (or no arrows) on both ends. Connection is symmetric.

Visual properties (arrowheads, line style, color, thickness) configurable per EdgeType and overridable per Edge instance.

### Connection compatibility

- An `OUT` JoinPoint can connect to an `IN` JoinPoint.
- `INOUT` can connect to anything.
- Two `OUT` or two `IN` cannot connect (unless both are `INOUT`).
- Self-loops (both endpoints on the same node) allowed if the NodeType permits.

### Waypoints

Edge waypoints are **absolute coordinates**. They do not move when either endpoint node moves. An **auto-reroute** capability can recalculate waypoints after node movement (see Open Design Decisions).

### Edge properties and labels

- Edges can have properties (label, condition expression, weight, color).
- Edge labels rendered at a configurable position along the edge (percentage from source to target, or at a specific waypoint).
- Edited in the property panel when the edge is selected.

### Connection creation interaction

1. User clicks on a JoinPoint. A **rubber-band preview** line follows the cursor.
2. During drag, compatible target JoinPoints are **visually highlighted**. Incompatible ones are dimmed.
3. Release on a compatible JoinPoint — edge is created.
4. Release elsewhere — creation cancelled.

Alternative: a **connection mode** toggle in the toolbar. When active, click on source JoinPoint, then click on target.

---

## 7. Node Palette

A dockable panel listing available NodeTypes, organized for discovery and drag-to-canvas creation.

### Layout

- **Categories**: NodeTypes grouped by `category` (e.g. "Flowchart", "Control Flow", "Data Sources"). Collapsible category sections.
- **Search/filter**: Text field at top. Filters by `displayName` and `category`. Instant filtering as user types.
- **Node entries**: Each entry shows the NodeType's `iconSpec` icon and `displayName`. Optionally a tooltip with description.

### Interaction

- **Drag-and-drop**: Drag a NodeType entry from the palette onto the canvas. A new Node instance is created at the drop position with the NodeType's default JoinPoints and property defaults.
- **Double-click**: Creates a node at the center of the current viewport (alternative to drag-and-drop).

### Extensibility

- Modules register NodeTypes during `install()`. Registered types appear in the palette automatically.
- When BSDataFlow is on the classpath, its processing NodeTypes (JDBC, REST, JMS, etc.) appear alongside diagramming types in their own categories.
- Palette categories and ordering are configurable via the XmlFilesystem.

---

## 8. Canvas & Interaction

### Infinite scrollable canvas

- Canvas extends infinitely in all directions. Scroll position tracked as offset (translateX/translateY on content layer).
- **Scrolling**: mouse wheel (vertical), Shift+wheel (horizontal), middle-mouse drag (pan), scrollbars.
- **Background**: configurable — plain, dot grid, line grid, or custom pattern. Grid spacing configurable.
- **Snap-to-grid**: optional, configurable spacing.

### Zoom

- **Ctrl+mouse wheel** zooms in/out, centered on cursor position.
- Configurable range (e.g. 10%–400%).
- Actions: **Fit to content**, **Zoom 100%**, **Zoom in/out** (step-based).
- Applied via `scaleX`/`scaleY` on the content layer.
- Optional future: minimap/overview inset showing entire graph with viewport rectangle.

### Selection model

- **Single click** on element: selects it, clears previous selection.
- **Ctrl+click**: toggles element in/out of selection (additive).
- **Rubber-band**: click-drag on empty canvas creates a selection rectangle. Elements within are selected.
- Selection is a `Set<UUID>`. Selected elements are visually indicated (handles, highlight border).
- `GraphSelectionChangedEvent` fired via Context/Event system.

### Move and resize

- **Move**: drag selected nodes. Multi-selection: all move together, maintaining relative positions.
- **Anchored edge behavior**: moving a selected node moves edges connected to that node's JoinPoints. The other endpoint (on a non-selected node) stays fixed. When both endpoint nodes are selected, the entire edge moves rigidly.
- **Resize**: selected nodes show resize handles (corners and edge midpoints). Minimum size constrained by NodeType.
- **Snap to grid**: node positions/sizes snap to grid intervals.
- **Alignment guides**: when a moved node's edge aligns with another node's edge, a visual guide line appears.

### Alignment and distribution actions

- Align selected nodes: left, right, top, bottom, center horizontal, center vertical.
- Distribute: evenly space selected nodes horizontally or vertically.
- Implemented as `IUIAction` instances registered in the action framework.

### Undo/Redo

- **Command pattern**: each user action (move, resize, create, delete, property change) is recorded as an `IGraphCommand` with `execute()`, `undo()`, `redo()`.
- **Compound commands**: related changes (e.g. "delete node and all its edges") recorded as one atomic command.
- History depth configurable.

### Keyboard shortcuts

Configurable via `core/key-bindings/graph-canvas/` XmlFilesystem path:

| Default key | Action |
|---|---|
| `Delete` | Delete selected elements |
| `Ctrl+A` | Select all |
| `Ctrl+C` / `Ctrl+X` / `Ctrl+V` | Copy / Cut / Paste |
| `Ctrl+Z` / `Ctrl+Y` | Undo / Redo |
| `Ctrl+G` | Group selected nodes |
| `Ctrl+Shift+G` | Ungroup selected group |
| Arrow keys | Nudge selected nodes (1px or 1 grid unit) |
| `Escape` | Cancel current operation |

### Context menus

Assembled from `IUIAction` instances via the action framework:

- **Canvas**: paste, select all, zoom options, grid toggle
- **Node**: cut, copy, delete, bring to front/send to back, group, properties
- **Edge**: delete, reverse direction, properties
- **Group**: enter group, collapse/expand, ungroup, properties

---

## 9. Subgrouping

### Group creation

- Select multiple nodes, invoke "Group" action (Ctrl+G).
- The Group **auto-detects exposed JoinPoints**: JoinPoints on member nodes that have external connections (edges to non-member nodes) become the Group's interface.
- User can manually add/remove/rename exposed JoinPoints after creation.

### Group editing pane

- **Enter group**: double-click a group (or "Enter Group" action) opens the group's content in a new canvas pane/tab.
- Shows only member nodes and internal edges.
- Exposed JoinPoints appear as special **interface nodes** at the pane edges (similar to VHDL port maps or Simulink subsystem ports).
- Full canvas functionality inside the group pane (same interactions, same tools).
- **Breadcrumb navigation**: trail showing group hierarchy path. Click a breadcrumb to navigate up.
- **Shared undo stack**: undo/redo crosses pane boundaries (one history per graph document, not per pane).

### Collapse/Expand

- **Expanded**: member nodes shown inline in parent canvas, with a visual boundary (dashed rectangle or styled frame).
- **Collapsed**: group shown as a single node. Its JoinPoints are the group's exposed JoinPoints. External edges connect to these. Distinctive visual indicator (double border, sub-graph icon).

### Interface definition

- Each exposed JoinPoint maps to exactly one internal node's JoinPoint.
- Mapping maintained automatically when internal nodes change, manually overridable.
- In BSDataFlow mode, exposed JoinPoints define the processing interface — data flows in through IN ports and out through OUT ports.

### Nesting

- Groups can contain other groups. Unlimited nesting depth.
- Navigation is hierarchical — each level opens in its own pane, breadcrumbs for navigation.

---

## 10. Persistence

### XML format

Graph documents stored as standalone XML files, loadable from disk, database, or network. A `FileLoader` can load them from the XmlFilesystem when needed.

- Model classes use **JAXB annotations** (`@XmlRootElement`, `@XmlElement`, `@XmlAttribute`).
- Custom JAXB adapters (reuse BSToolbox's `LocalDateTimeAdapter`, `GenericMapAdapter`, etc.) for UUID, Point2D, property maps.
- File extension: **`.bsgraph`**

### GUID stability

- All UUIDs preserved across save/load round-trips.
- New elements get UUID at creation (via `RandomUUIDCreator.getRandomUuid()`), never regenerated.
- **Copy/paste** always creates new UUIDs for pasted elements (the original stays, the copy gets fresh IDs).
- **Cut/paste** prompts the user: preserve original UUIDs (typical intent — relocating) or regenerate (when pasting into the same graph where originals still exist).

### Deterministic sorting (git-friendly)

- All collections sorted **by UUID** (lexicographic) before serialization.
- Attribute elements within a node/edge sorted **by key name**.
- No timestamp-based ordering that changes on every save.
- Pretty-printed with **consistent indentation** (via JAXB `FORMATTED_OUTPUT`).
- Result: two saves of the same unmodified graph produce **byte-identical XML**.

### Schema versioning

- Root element carries a `version` attribute (integer, starting at 1).
- Migration mechanism transforms older versions to current on load.
- Unknown elements/attributes preserved on round-trip (forward compatibility).

### Conceptual XML example

```xml
<graph id="550e8400-e29b-41d4-a716-446655440000" name="Order Processing" version="1">
  <metadata>
    <property key="author" value="Jakub"/>
    <property key="created" value="2026-06-19"/>
  </metadata>
  <nodes>
    <node id="..." type="process" x="100" y="200" width="150" height="80" zOrder="0">
      <properties>
        <property key="label" value="Validate Order"/>
      </properties>
      <joinPoints>
        <joinPoint id="..." name="input" position="LEFT" direction="IN" maxConnections="1"/>
        <joinPoint id="..." name="output" position="RIGHT" direction="OUT" maxConnections="-1"/>
        <joinPoint id="..." name="error" position="BOTTOM" direction="OUT" maxConnections="1"/>
      </joinPoints>
    </node>
  </nodes>
  <edges>
    <edge id="..." type="default" source="jp-uuid-1" target="jp-uuid-2"
          directionality="UNIDIRECTIONAL">
      <waypoints>
        <point x="250" y="210"/>
      </waypoints>
      <properties>
        <property key="label" value="valid"/>
      </properties>
    </edge>
  </edges>
  <groups>
    <group id="..." name="Payment Subprocess" collapsed="false">
      <members>
        <nodeRef id="node-uuid-1"/>
        <nodeRef id="node-uuid-2"/>
        <edgeRef id="edge-uuid-1"/>
      </members>
      <exposedJoinPoints>
        <joinPoint id="..." name="in" direction="IN" mapsTo="internal-jp-uuid"/>
        <joinPoint id="..." name="out" direction="OUT" mapsTo="internal-jp-uuid"/>
      </exposedJoinPoints>
    </group>
  </groups>
</graph>
```

---

## 11. BSDataFlow Processing Engine

### Processing model

- `ProcessingGraph` loaded from the same XML format as the visual graph plus processing metadata per node.
- Each processing NodeType has an `INodeProcessor`: `void process(ProcessingContext ctx, NodeInstance node)`.
- `ProcessingContext` carries: input data (message payload + headers), output routing, error handling strategy, variables/state, logging, transaction context.
- `GraphExecutor` traverses the graph from entry points (nodes with no inbound edges, or explicitly marked as "start") and invokes processors.

### Data flow semantics

- Data flows along directed edges. Each edge carries a **message** (payload + headers/metadata).
- **Fan-out** (one-to-many from a single output JoinPoint): message cloned to all outbound edges. Parallel or sequential execution, configurable.
- **Fan-in** (many-to-one to a single input JoinPoint): requires an **aggregation strategy** (wait for all, first wins, merge).
- **Error handling**: each node can define an error output JoinPoint. Unhandled errors propagate to a graph-level error handler.

### Runtime state

`GraphInstance` tracks execution state per node:

| State | Meaning |
|---|---|
| `PENDING` | Not yet reached |
| `RUNNING` | Currently executing |
| `COMPLETED` | Finished successfully |
| `FAILED` | Finished with error |
| `SKIPPED` | Bypassed (conditional routing) |

Supports **parallel execution**: independent branches (no shared data dependency) execute concurrently.

### Built-in processor modules

| Module | Node types |
|---|---|
| `bsdataflow-core` | Conditional router, splitter, aggregator, loop, timer, logger, variable set/get |
| `bsdataflow-jdbc` | JDBC query, stored procedure call, batch insert |
| `bsdataflow-rest` | HTTP request (GET/POST/PUT/DELETE/PATCH) |
| `bsdataflow-soap` | SOAP client (WSDL-driven) |
| `bsdataflow-jms` | JMS publisher, JMS consumer |
| `bsdataflow-transform` | Data mapping (XPath, JSONPath, expression-based), format conversion |

### Deployment modes

- **Standalone**: `GraphExecutor` loaded from XML, triggered by main class. No Spring, no JFX.
- **Spring Boot**: `GraphExecutor` as Spring bean. Graph loaded from classpath/config. Exposed as REST or SOAP endpoint.
- **JMS consumer**: `GraphExecutor` triggered by incoming JMS messages. Message payload = initial graph data.
- **Scheduled**: cron-based execution.

---

## 12. Visual-Runtime Bridge

Integration between BSToolbox-jfx (UI) and BSDataFlow (engine) when both are present.

### Runtime visualization

- **Status badges** on nodes (using the existing `ObjectStatus` / SVG badge pattern) showing execution state.
- **Data flow animation**: visual pulse along edges as data flows.
- **Execution trace highlighting**: completed path highlighted, current node emphasized.

### Event-based bridge

`GraphExecutor` fires events: `NodeStartedEvent`, `NodeCompletedEvent`, `NodeFailedEvent`, `EdgeTraversedEvent`. The UI layer subscribes via the Context/Event system and updates visuals.

### Debugging

- Set **breakpoints** on nodes. Executor pauses before/after the node processor runs.
- **Step-by-step execution**: pause, step over (execute one node), resume.
- **Context inspection**: when paused, UI shows `ProcessingContext` contents (message payload, headers, variables) in an inspector panel.

---

## 13. Infrastructure Reuse

Mapping to existing BSToolbox / BSToolbox-jfx patterns:

| Need | Existing pattern | How to use |
|---|---|---|
| NodeType/EdgeType registration | `FileLoader` + `ServiceLoader` + `Modules` | Register graph types during module `install()`, FileLoader dispatch for XML-defined types |
| Graph events | `Context.fireEvent()` + `EventListener<T>` | `GraphSelectionChangedEvent`, `GraphModelChangedEvent`, `GraphSavedEvent` dispatched through context hierarchy |
| Element IDs | `RandomUUIDCreator` / `HashUUIDCreator` | v4 for new elements; SHA1-based for deterministic IDs (auto-generated JoinPoints from NodeType) |
| XML persistence | JAXB + `XmlUtils` + adapters | Model classes with `@XmlRootElement`; reuse `LocalDateTimeAdapter`, `GenericMapAdapter` |
| Graph canvas actions | `IUIAction` + `ActionBinder` + `AcceleratorManager` | All operations (delete, copy, undo, zoom, group, align) as `IUIAction`; shortcuts via `core/key-bindings/graph-canvas/` |
| Property editing | `IValueEditorProvider` + `ValueEditorFactory` | Node/edge properties edited via existing provider framework; custom providers for domain-specific properties |
| Bean selection switching | `SwitchableBeanHolder` | Property panel uses `setBean()` when selection changes; snapshot/reset for undo/cancel |
| Lifecycle tracking | `IStatusBean` + `IParentedStatusBean` | Graph document and its elements track SAVED/MODIFIED state; child modifications propagate up |
| Toolbar composition | `ContributionManager` | Canvas toolbar assembled from ordered contributions; extension modules add tools at specific priorities |
| UI state persistence | `FxStateManager` + `FxStateBinder` | Canvas zoom, scroll position, active tool persisted across sessions |
| Status badges | `ObjectStatus` + SVG rendering | Node execution state rendered as overlay badges (reuse existing SVG badge pattern) |

---

## 14. Design Decisions

| Decision | Resolution |
|---|---|
| Edge waypoint coordinates | **Absolute** with auto-reroute capability |
| Undo strategy | **Command pattern** (`IGraphCommand` with `execute`/`undo`/`redo`) for efficiency; memento as fallback for complex ops |
| Clipboard format | **XML fragment** (consistent with persistence, enables inter-application paste) |
| BSDataFlow threading | **`ExecutorService`-based** with configurable parallelism |
| Collapsed group visual | **Distinctive** (double border + sub-graph icon) |
| Copy/paste UUIDs | **Regenerate** — new UUIDs for copied elements, no duplicates within a graph |
| Cut/paste UUIDs | **Ask the user** — prompt whether to preserve original UUIDs or regenerate. Preserving is the common intent (moving, not cloning), but regeneration may be needed when pasting into the same graph. |
| Properties in XML | **JAXB** for graph content (richer structure), XmlFilesystem for type registration |

---

## 15. Advanced Processing Architecture

Requirements for the production-grade BSDataFlow processing engine beyond the basic Phase 11 framework.

### Data model abstraction

The engine must process various data types uniformly. A `DataPayload` interface abstracts the underlying representation:

| Payload type | Description | Streaming |
|---|---|---|
| `XmlPayload` | DOM or StAX-based XML | Yes (StAX) |
| `CsvPayload` | Parsed CSV rows | Yes (line-by-line) |
| `StreamPayload` | Raw `InputStream`/`Reader` | Yes |
| `ResultSetPayload` | JDBC `ResultSet` wrapper | Yes (row-by-row) |
| `TablePayload` | In-memory row/column table | No (preloaded) |
| `ObjectPayload` | Arbitrary Java object | No |

Streaming payloads implement `Iterable<DataRow>` for row-by-row processing. Non-streaming payloads can be converted to streaming via adapters.

### Data source framework

A generic `IDataSource` base component provides the pull-based data feeding pattern:

```
IDataSource
├── hasNext(): boolean
├── next(): Message
├── reset(): void
├── close(): void
└── setTriggerCallback(Consumer<TriggerSignal>): void
```

**Trigger mechanism**: a data source can be configured in two modes:
- **Auto-push**: iterates all items automatically, pushing each downstream
- **Triggered**: waits for a trigger signal before pushing the next item. The trigger comes from a downstream node (e.g., a completion callback, a semaphore permit)

Built-in data sources (each a separate processor module):
- **FileSource**: reads a file as XML (`XmlPayload`), CSV (`CsvPayload`), or lines (`StreamPayload`). File path from node properties or context variable.
- **JdbcSource**: executes a SELECT query, wraps `ResultSet` as `ResultSetPayload`. Connection pool reference from properties. Supports parameterized queries via context variables.
- **ExternalInput**: receives data from the hosting environment (Spring controller request body, JMS message payload, programmatic API call). Acts as the graph entry point.

### Semaphore / flow control

A `SemaphoreProcessor` controls throughput and backpressure:

**Throughput limiting mode:**
- Issues N permits per time window (e.g., 100/second)
- Permit rate dynamically adjustable via context variable (e.g., linked to system load monitoring)
- Queue depth limit — rejects/blocks when queue exceeds threshold

**Triggered source mode:**
- Acts as a `IDataSource` backed by another source
- Waits for a trigger signal on its trigger input JoinPoint before pulling the next item from the backing source
- Trigger can carry a count (how many items to release)
- "No more data" output when backing source is exhausted
- JoinPoints: `trigger-in` (IN), `data-out` (OUT), `exhausted` (OUT)

The semaphore integrates with the data source trigger mechanism — it is effectively a data source that gates another data source.

### Mapper / transformer

The `MapperProcessor` transforms data between formats:

**XSLT mode:**
- Input: `XmlPayload` (or auto-wrapped row/table data in a simple DOM envelope)
- Transformation: XSLT stylesheet (stored as node property or referenced file)
- Extension functions available:
  - Context variable access: `ctx:variable('name')`
  - Global variable access: `global:variable('name')`
  - Environment variable access: `env:variable('name')`
  - Processor instance variables: `proc:variable('name')`

**Row mapping mode:**
- Input: `CsvPayload` / `ResultSetPayload` / `TablePayload`
- Mapping definition: source column → target column with optional expression
- Auto-wraps rows in a minimal DOM for XSLT if needed

**UI editor** (future, in BSToolbox-jfx):
- Visual source→target field mapping with drag-and-drop
- XSLT template editor with syntax highlighting
- Extension function palette
- Preview/test with sample data

### Subprocess component

A `SubprocessProcessor` invokes another graph definition as a subroutine:

- References an external `.bsgraph` file (path in node property)
- Passes input message to the subprocess graph's entry point
- Collects the subprocess exit point's output message as its own output
- Subprocess has its own `GraphInstance` (isolated state)
- Variables can be passed in/out via message headers
- Similar to a group but with a standalone file-based definition (reusable across graphs)

### Processor instance context

Each `INodeProcessor` instance maintains its own context (`ProcessorInstanceContext`):

- Private variable store (not shared with other processors)
- Lifecycle: initialized when the graph is loaded, cleared on graph unload
- Supports stateful processors (counters, accumulators, caches, connection pools)
- Thread-safe — multiple messages can be processed concurrently by the same processor instance
- Accessible in XSLT extensions via `proc:variable('name')`

### Runtime hot-swapping

The `GraphManager` (new class) supports loading and unloading process definitions at runtime:

- `load(File)` / `load(InputStream)`: loads, validates, registers a graph definition
- `unload(UUID graphId)`: stops running instances, cleans up processor contexts, removes from registry
- `reload(UUID graphId, File)`: atomic unload + load (waits for in-flight executions to complete or timeout)
- Version tracking: each loaded graph has a version; running instances continue with the version they started with
- Deployment descriptor: optional XML/properties file listing graphs to auto-load on startup
- JMX or REST management interface (future) for runtime control

### Concurrency model

- Each `GraphInstance` is an independent execution (separate state, contexts)
- Multiple instances of the same graph can run concurrently
- Processor instances are shared across graph instances (singleton per graph definition) — must be thread-safe
- `ExecutorService` configuration: fixed pool, cached pool, or virtual threads (JDK 21)
- Back-pressure: when a processor's input queue exceeds a threshold, upstream propagation pauses

### Built-in processor modules (expanded)

| Module | Processors |
|---|---|
| `bsdataflow-core` | Conditional router, splitter, join/aggregator, loop, timer, logger, variable set/get, semaphore, subprocess |
| `bsdataflow-io` | FileSource (XML/CSV/lines), FileWriter, StreamReader |
| `bsdataflow-jdbc` | JdbcSource (SELECT), JdbcWriter (INSERT/UPDATE), StoredProcedure, BatchInsert |
| `bsdataflow-rest` | HTTP request (GET/POST/PUT/DELETE/PATCH), REST endpoint (inbound) |
| `bsdataflow-soap` | SOAP client (WSDL-driven), SOAP endpoint (inbound) |
| `bsdataflow-jms` | JMS publisher, JMS consumer (inbound) |
| `bsdataflow-transform` | XSLT mapper, row mapper, format converter (XML↔JSON↔CSV) |

---

## 16. Implementation Plan

Phased plan with testable increments. Phases 1–10 form the UI path; Phase 11 (processing engine) can be developed in parallel after Phase 2. Phase 12 merges both streams.

### Phase dependency graph

```
Phase 1 (Model)
  │
Phase 2 (Types)
  │         \
Phase 3     Phase 11 (Engine) ← can start after Phase 2
  │              │
Phase 4          │
  │              │
Phase 5          │
  │              │
Phase 6          │
  │              │
Phase 7          │
  │              │
Phase 8          │
  │              │
Phase 9          │
  │              │
Phase 10         │
  \             /
  Phase 12 (Bridge)
```

### Milestone timeline

| After phase | What you can see / do |
|---|---|
| 3 | Empty canvas with grid, zoom, pan |
| 4 | Nodes visible on canvas |
| 5 | Edges connecting nodes, click to select |
| 7 | Full editing loop — add from palette, connect, delete, undo. First usable tool. |
| 9 | Save/load `.bsgraph` files. First version suitable for real work. |

---

### Phase 1 — BSDataFlow Project Setup and Core Model

**What gets built:**
- New Maven project `BSDataFlow` (`cz.bliksoft.java:bsdataflow`), pom.xml following BSToolbox conventions (`${revision}`, flatten plugin, formatter plugin, git-commit-id plugin).
- Core model POJOs in `cz.bliksoft.dataflow.model` with JAXB annotations:
  - `Graph` (`@XmlRootElement`), `Node`, `Edge`, `JoinPoint`, `Group`
  - All fields per section 4 (UUID id, typeId, x/y/width/height, properties, joinPoints, etc.)
  - Constructor assigns `RandomUUIDCreator.getRandomUuid()`, `equals`/`hashCode` by UUID.
- Enums: `Direction`, `Directionality`, `JoinPointPosition`
- `PropertySchema`, `PropertyDefinition` in `cz.bliksoft.dataflow.model.schema`
- Custom JAXB adapters in `cz.bliksoft.dataflow.xml`: `UUIDAdapter`, `Point2DAdapter`, `PropertyMapAdapter`
- Deterministic serialization: collections sorted by UUID, `FORMATTED_OUTPUT` enabled.

**Test criteria:**
- JUnit: create a `Graph` with multiple nodes, edges, joinPoints, groups. Marshal to XML, unmarshal, assert all fields round-trip including UUIDs.
- JUnit: serialize same unmodified graph twice → byte-identical XML (deterministic sort).
- JUnit: marshal → unmarshal → marshal again → compare XML (UUID stability).

---

### Phase 2 — Type Registry and NodeType/EdgeType

**What gets built:**
- `NodeType` and `EdgeType` interfaces/records in `cz.bliksoft.dataflow.types`:
  - `NodeType`: typeId, displayName, category, iconSpec, `List<JoinPointDefinition>`, `PropertySchema`
  - `EdgeType`: typeId, displayName, allowed visual types, default directionality, `PropertySchema`
  - `JoinPointDefinition`: position, direction, name, maxConnections, compatibleEdgeTypes
- `NodeTypeRegistry`, `EdgeTypeRegistry`: `register()`, `get(typeId)`, `getAll()`, `getByCategory()`
- `INodeTypeProvider` SPI interface for ServiceLoader
- Built-in flowchart types in `cz.bliksoft.dataflow.types.flowchart`:
  - `FlowchartNodeTypes.START`, `END`, `PROCESS`, `DECISION`, `CONNECTOR`, `IO`, `ANNOTATION`, `TEXT_BOX` — each with correct default JoinPoints per section 5.
- Default `EdgeType` (straight line, unidirectional)

**Test criteria:**
- JUnit: register types, retrieve by ID, verify all fields.
- JUnit: create `Node` instances from each flowchart type with default JoinPoints, verify count and direction.
- JUnit: duplicate type ID registration → rejected.
- JUnit: ServiceLoader discovers `INodeTypeProvider` implementations.

---

### Phase 3 — Graph Canvas (Empty Scrollable/Zoomable Surface)

**What gets built:**
- New package `cz.bliksoft.javautils.fx.controls.graph` in BSToolbox-jfx.
- `GraphCanvas extends Region`:
  - Two layers: background layer (grid `Canvas`) + content layer (`Pane` for nodes/edges).
  - Scroll: mouse wheel (vertical), Shift+wheel (horizontal), middle-mouse drag (pan).
  - Zoom: Ctrl+wheel centered on cursor, range 10%–400%, via `scaleX`/`scaleY`.
  - Background: plain, dot grid, line grid. Configurable spacing.
  - `zoomProperty()`, `scrollXProperty()`, `scrollYProperty()` exposed.
  - CSS class `graph-canvas`.
- `GraphCanvasStateBinder` implementing `FxStateBinder` for zoom/scroll persistence.
- `graph-canvas.css` with default styling.

**Test criteria:**
- Manual test app: `Stage` with `GraphCanvas`. Verify scroll, pan, zoom, grid rendering, zoom range clamping.
- JUnit: `zoomProperty` clamps out-of-range values.

**Dependencies:** None from BSDataFlow (pure JFX).

---

### Phase 4 — Node Rendering on Canvas

**What gets built:**
- Optional Maven dependency on BSDataFlow in BSToolbox-jfx pom.xml (`<optional>true</optional>`).
- `INodeRenderer` interface in `cz.bliksoft.javautils.fx.controls.graph.render`:
  - `Region createNodeVisual(Node, NodeType, RenderContext)`
  - `RenderContext`: zoom level, selection state, theme
- Base renderers:
  - `ShapeNodeRenderer` — geometric shape (rect, rounded rect, diamond, ellipse, parallelogram) + centered label. Shape parameterized.
  - `IconNodeRenderer` — icon (via iconspec) + label below.
- `NodeRendererRegistry` — maps typeId → `INodeRenderer`.
- Flowchart renderers registered for all Phase 2 types.
- `GraphCanvas.setGraph(Graph)` — populates content layer, positions nodes at `(x, y)`.
- JoinPoint indicators: small circles at computed positions on node edges. Color by direction (IN=green, OUT=blue, INOUT=orange).

**Test criteria:**
- Manual test app: create graph with 5–8 flowchart nodes, call `setGraph()`. Verify correct shapes, positions, JoinPoint indicators, zoom/pan still work.
- JUnit: `ShapeNodeRenderer` produces non-null `Region` with correct preferred size per shape.

**Dependencies:** Phase 1+2 (model/types), Phase 3 (canvas).

---

### Phase 5 — Edge Rendering and Basic Selection

**What gets built:**
- `IEdgeRenderer` in `cz.bliksoft.javautils.fx.controls.graph.render`:
  - `Node createEdgeVisual(Edge, EdgeType, Point2D source, Point2D target, List<Point2D> waypoints, RenderContext)`
- Renderers: `StraightLineEdgeRenderer`, `OrthogonalEdgeRenderer`.
- Arrowhead rendering (SVG-path). Edge label positioning.
- Edge layer rendered below node layer.
- `GraphSelectionModel` in `cz.bliksoft.javautils.fx.controls.graph.interaction`:
  - `Set<UUID>` of selected IDs, `selectedProperty()` observable.
  - Single click selects (clears previous), Ctrl+click toggles, click empty deselects.
  - Rubber-band selection rectangle on empty canvas drag.
  - Visual feedback: CSS `graph-node-selected` class, edge color/thickness change.
- `GraphSelectionChangedEvent` fired through Context events.

**Test criteria:**
- Manual: graph with nodes and edges (including waypoints). Verify line rendering, arrowheads, labels, selection interactions, rubber-band.
- JUnit: `GraphSelectionModel` add/remove/toggle/clear produce correct state and fire events.

**Dependencies:** Phase 4.

---

### Phase 6 — Node Interaction (Move, Resize, Snap, Undo)

**What gets built:**
- Move: drag selected nodes, multi-select moves together, anchored edge behavior, snap-to-grid, alignment guides.
- Resize: 8 handles (corners + edge midpoints), min size from NodeType, snap-to-grid.
- Undo/redo framework in `cz.bliksoft.javautils.fx.controls.graph.command`:
  - `IGraphCommand`: `execute()`, `undo()`, `redo()`, `getDescription()`
  - `GraphCommandHistory`: stack-based, configurable depth, compound commands.
  - `CompoundGraphCommand`, `MoveNodesCommand`, `ResizeNodeCommand`.
  - Undo/redo as `IUIAction`.
- Keyboard shortcuts via `core/key-bindings/graph-canvas/`:
  - Arrow keys (nudge), Delete (delete), Ctrl+A (select all), Ctrl+Z/Y (undo/redo).

**Test criteria:**
- Manual: drag, multi-drag, resize, snap, alignment guides, undo/redo chains, arrow nudge, Delete.
- JUnit: `GraphCommandHistory` push/undo/redo. `MoveNodesCommand` updates and restores model coordinates.

**Dependencies:** Phase 5.

---

### Phase 7 — Connection Creation and Node Palette

**What gets built:**
- Connection creation in `cz.bliksoft.javautils.fx.controls.graph.interaction`:
  - Click JoinPoint → rubber-band preview → release on compatible target → `Edge` created.
  - Compatible targets highlighted, incompatible dimmed (direction + cardinality checks).
  - `CreateEdgeCommand` for undo.
- `GraphPalette` in `cz.bliksoft.javautils.fx.controls.graph.palette`:
  - Categorized `TitledPane` sections, search/filter `TextField`.
  - Icon + displayName per entry.
  - Drag-and-drop to canvas → `CreateNodeCommand`. Double-click → create at viewport center.
- Context menus (via `IUIAction`):
  - Canvas: paste, select all, zoom, grid toggle.
  - Node: cut, copy, delete, z-order, properties.
  - Edge: delete, reverse direction, properties.

**Test criteria:**
- Manual: palette search/filter, drag-to-canvas, connect JoinPoints, cardinality enforcement, context menus, all undoable.
- JUnit: connection compatibility logic (direction/cardinality accept/reject).

**Dependencies:** Phase 6.

---

### Phase 8 — Property Editor and Copy/Paste

**What gets built:**
- `GraphPropertyPanel` in `cz.bliksoft.javautils.fx.controls.graph.properties`:
  - `SwitchableBeanHolder` pattern: selection change → `setBean()` with observable wrapper.
  - Rows from `PropertySchema`, editors via `ValueEditorFactory`.
  - `INodeConfigEditor`: optional custom editor panel per NodeType.
  - `PropertyChangeCommand` for undo.
- Observable model wrappers in `cz.bliksoft.javautils.fx.controls.graph.beans`:
  - `ObservableNode`, `ObservableEdge`, `ObservableGraph` (with `IStatusBean`).
- Copy/Cut/Paste:
  - Clipboard = XML fragment (JAXB). Copy regenerates UUIDs on paste (offset position). Cut/paste prompts: preserve or regenerate UUIDs.
  - `PasteCommand`, `DeleteElementsCommand`.
  - Ctrl+C, Ctrl+X, Ctrl+V shortcuts.

**Test criteria:**
- Manual: select node → property panel shows correct editors → edit → model updates → undo works. Copy/paste with UUID regeneration. Cut/paste prompt.
- JUnit: `ObservableNode` setBean reflects model values, proxy changes update model. Copy/paste serialization round-trip.

**Dependencies:** Phase 7.

---

### Phase 9 — Persistence and Bezier/Polyline Edges

**What gets built:**
- `GraphFileManager` in `cz.bliksoft.javautils.fx.controls.graph.persistence`:
  - Save/load `.bsgraph` files via JAXB + `XmlUtils`.
  - `IStatusBean` integration: SAVED ↔ MODIFIED lifecycle.
  - Title bar modified indicator.
  - Save/Load as `IUIAction` (Ctrl+S, Ctrl+O). File chooser with `.bsgraph` filter.
- Additional edge renderers: `BezierEdgeRenderer` (smooth curve, draggable control points), `PolylineEdgeRenderer` (user-defined waypoints).
- Edge type switching via properties or context menu.
- Schema versioning: `Graph.version`, `IGraphMigrator`, unknown elements preserved on round-trip.

**Test criteria:**
- Manual: create graph → save → close → reopen → load → all preserved. Modified indicator. Bezier/polyline edge editing.
- JUnit: save → load → compare all fields including UUIDs. Save twice → byte-identical. Version migration.

**Dependencies:** Phase 8.

---

### Phase 10 — Subgrouping

**What gets built:**
- Group operations in `cz.bliksoft.javautils.fx.controls.graph.group`:
  - Group creation (Ctrl+G): auto-detect exposed JoinPoints. Ungroup (Ctrl+Shift+G).
  - `GroupCommand`, `UngroupCommand` for undo.
- Visualization: expanded (dashed boundary), collapsed (double border + sub-graph icon, exposed JoinPoints as ports).
- Group editing pane: double-click → open in tab with breadcrumb navigation. Interface nodes at pane edges. Full canvas functionality. Shared undo stack.
- Nested groups.

**Test criteria:**
- Manual: select nodes → Ctrl+G → group boundary appears, external edges connect to exposed JoinPoints. Collapse/expand. Enter group → breadcrumb → edit inside → navigate back. Nested groups. Undo group creation.
- JUnit: exposed JoinPoint auto-detection for various edge configurations.

**Dependencies:** Phase 9.

---

### Phase 11 — Processing Engine (BSDataFlow)

*Can start in parallel after Phase 2.*

**What gets built:**
- Processing framework in `cz.bliksoft.dataflow.engine`:
  - `INodeProcessor`: `void process(ProcessingContext, NodeInstance)`
  - `ProcessingContext`: message payload + headers, output routing, error handling, variables, logging.
  - `GraphInstance`: per-node state tracking (PENDING/RUNNING/COMPLETED/FAILED/SKIPPED).
  - `GraphExecutor`: graph traversal from entry points, `ExecutorService`-based parallel execution.
  - Fan-out (message cloning), fan-in (aggregation strategies), error routing.
- Built-in processors in `cz.bliksoft.dataflow.processors.core`: conditional router, splitter, aggregator, loop, timer, logger, variable set/get.
- `GraphExecutorEvents`: `NodeStartedEvent`, `NodeCompletedEvent`, `NodeFailedEvent`, `EdgeTraversedEvent`.

**Test criteria:**
- JUnit: linear graph (start → process → process → end) with mock processors → all invoked in order, all COMPLETED.
- JUnit: fan-out → both branches execute, messages cloned.
- JUnit: fan-in → aggregation waits for all inputs.
- JUnit: error routing → exception → error JoinPoint edge followed, node FAILED.
- JUnit: conditional routing → correct branch taken.
- JUnit: parallel branches execute concurrently (timing-based).

**Dependencies:** Phase 2 only. No JFX.

---

### Phase 12 (Future) — Visual-Runtime Bridge and Extensions

**What gets built:**
- Runtime visualization in `cz.bliksoft.javautils.fx.controls.graph.runtime`:
  - Status badges on nodes (execution state via `ObjectStatus`/SVG).
  - Event subscription to `GraphExecutor` events → real-time visual updates.
  - Execution trace highlighting, data flow animation.
- Step debugging: breakpoints (toggle via context menu), pause/step-over/resume actions, `ProcessingContext` inspector panel.
- Processor modules (separate Maven artifacts): `bsdataflow-jdbc`, `bsdataflow-rest`, `bsdataflow-soap`, `bsdataflow-jms`, `bsdataflow-transform`.
- Deployment integrations: Spring Boot, JMS consumer, scheduled execution.
- Alignment/distribution actions, minimap/overview inset.

**Test criteria:**
- Manual: execute graph in visual editor → watch node badges transition PENDING → RUNNING → COMPLETED. Set breakpoints, inspect context, step through.
- JUnit per processor module: JDBC against in-memory DB, REST against WireMock, etc.

**Dependencies:** Phase 10 + 11.
