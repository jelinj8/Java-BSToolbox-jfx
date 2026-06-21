package cz.bliksoft.javautils.fx.controls.graph;

import java.util.UUID;

import cz.bliksoft.dataflow.engine.GraphExecutor;
import cz.bliksoft.dataflow.engine.GraphInstance;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.javautils.app.ui.actions.ActionBinder;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.UIActions;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IClose;
import cz.bliksoft.javautils.app.ui.actions.interfaces.INew;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IOpen;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IRedo;
import cz.bliksoft.javautils.app.ui.actions.interfaces.ISave;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IUndo;
import cz.bliksoft.javautils.app.ui.interfaces.IStackedComponent;
import cz.bliksoft.javautils.app.ui.interfaces.ITitleProvider;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateMeta;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateManager;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.fx.binding.ObjectStatus;
import cz.bliksoft.javautils.fx.controls.graph.actions.GraphEditorActions;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupEditingPane;
import cz.bliksoft.javautils.fx.controls.graph.palette.GraphPalette;
import cz.bliksoft.javautils.fx.controls.graph.persistence.GraphDocumentState;
import cz.bliksoft.javautils.fx.controls.graph.persistence.GraphFileManager;
import cz.bliksoft.javautils.fx.controls.graph.properties.GraphPropertyPanel;
import cz.bliksoft.javautils.fx.controls.graph.runtime.ContextInspectorPanel;
import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.SetChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class GraphEditorPanel extends BorderPane
		implements INew, ISave, IOpen, IUndo, IRedo, IClose, ITitleProvider, IStackedComponent, IGraphEditor {

	private final GraphCanvas canvas;
	private final GraphPalette palette;
	private final GraphPropertyPanel propertyPanel;
	private final ContextInspectorPanel inspectorPanel;
	private final GraphDocumentState documentState;
	private final GraphFileManager fileManager;
	private final GroupEditingPane editingPane;
	private final GraphRuntimeBridge runtimeBridge;
	private final FxStateManager stateManager;
	private SplitPane rightPanels;

	private final ToolBar fileToolBar;
	private final ToolBar editToolBar;
	private final ToolBar graphToolBar;

	private final SimpleBooleanProperty newEnabled = new SimpleBooleanProperty(true);
	private final SimpleBooleanProperty saveEnabled = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty openEnabled = new SimpleBooleanProperty(true);
	private final SimpleBooleanProperty closeEnabled = new SimpleBooleanProperty(true);

	private final SimpleBooleanProperty notRunning = new SimpleBooleanProperty(true);
	private final SimpleBooleanProperty running = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty paused = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty hasSelection = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty singleSelection = new SimpleBooleanProperty(false);

	private Context editorContext;

	public GraphEditorPanel() {
		this(null);
	}

	public GraphEditorPanel(GraphExecutor executor) {
		GraphEditorActions.registerAll();

		canvas = new GraphCanvas();
		palette = new GraphPalette();
		propertyPanel = new GraphPropertyPanel();
		inspectorPanel = new ContextInspectorPanel();
		documentState = new GraphDocumentState();
		fileManager = new GraphFileManager(canvas, documentState);
		editingPane = new GroupEditingPane(new Graph("New Graph"), canvas);
		canvas.getGroupHandler().setEditingPane(editingPane);

		if (executor != null) {
			runtimeBridge = new GraphRuntimeBridge(canvas, executor);
			running.bind(runtimeBridge.runningProperty());
			notRunning.bind(runtimeBridge.runningProperty().not());
			paused.bind(runtimeBridge.pausedProperty());
		} else {
			runtimeBridge = null;
		}

		palette.setTargetCanvas(canvas);
		palette.refresh();
		propertyPanel.setCanvas(canvas);
		propertyPanel.setOnGraphNameChanged(() -> documentState.refreshTitle());

		wireSelectionModel();
		wireDocumentState();
		wireRuntimeInspector();

		stateManager = new FxStateManager("graphEditorPanel");
		buildLayout();
		fileToolBar = buildFileToolBar();
		editToolBar = buildEditToolBar();
		graphToolBar = buildGraphToolBar();

		fileManager.newDocument();
	}

	// =========================================================================
	// Layout
	// =========================================================================

	private void buildLayout() {
		palette.setPrefWidth(200);
		palette.setMinWidth(150);
		palette.setMaxWidth(300);
		propertyPanel.setPrefWidth(250);
		propertyPanel.setMinWidth(180);
		inspectorPanel.setPrefHeight(200);
		inspectorPanel.setMinHeight(100);

		rightPanels = FxStateMeta.key(new SplitPane(), "rightPanels");
		rightPanels.setOrientation(Orientation.VERTICAL);
		rightPanels.getItems().add(propertyPanel);
		rightPanels.setPrefWidth(250);
		rightPanels.setMinWidth(180);
		rightPanels.setMaxWidth(400);

		SplitPane mainSplit = FxStateMeta.key(new SplitPane(), "mainSplit");
		mainSplit.setOrientation(Orientation.HORIZONTAL);
		mainSplit.getItems().addAll(palette, editingPane, rightPanels);
		mainSplit.setDividerPositions(0.15, 0.78);

		SplitPane.setResizableWithParent(palette, false);
		SplitPane.setResizableWithParent(rightPanels, false);

		setCenter(mainSplit);
	}

	// =========================================================================
	// Toolbars
	// =========================================================================

	private ToolBar buildFileToolBar() {
		ToolBar tb = new ToolBar();
		tb.getItems().addAll(actionButton("New"), actionButton("Open"), actionButton("Save"),
				actionButton("GraphSaveAs"));
		return tb;
	}

	private ToolBar buildEditToolBar() {
		ToolBar tb = new ToolBar();
		tb.getItems().addAll(actionButton("Undo"), actionButton("Redo"));
		return tb;
	}

	private ToolBar buildGraphToolBar() {
		ToolBar tb = new ToolBar();
		tb.getItems().addAll(actionButton("GraphExecute"), actionButton("GraphStepDebug"), new Separator(),
				actionButton("GraphStepOver"), actionButton("GraphStepAll"), actionButton("GraphResume"),
				actionButton("GraphRunToEnd"), new Separator(), actionButton("GraphPause"), actionButton("GraphStop"),
				new Separator(), actionButton("GraphToggleBP"));
		return tb;
	}

	private Button actionButton(String actionKey) {
		Button btn = new Button();
		IUIAction action = UIActions.getAction(actionKey);
		if (action != null)
			ActionBinder.bind(btn, action);
		return btn;
	}

	public ToolBar getFileToolBar() {
		return fileToolBar;
	}

	public ToolBar getEditToolBar() {
		return editToolBar;
	}

	public ToolBar getGraphToolBar() {
		return graphToolBar;
	}

	// =========================================================================
	// Wiring
	// =========================================================================

	private void wireSelectionModel() {
		canvas.getSelectionModel().observableSelection()
				.addListener((SetChangeListener.Change<? extends UUID> change) -> {
					var sel = canvas.getSelectionModel().getSelection();
					hasSelection.set(!sel.isEmpty());
					singleSelection.set(sel.size() == 1);
					refreshInspector();
				});
	}

	private void wireDocumentState() {
		canvas.getCommandHistory().canUndoProperty().addListener((obs, o, n) -> documentState.markModified());
		documentState.statusProperty().addListener((obs, o, n) -> {
			saveEnabled.set(n == ObjectStatus.MODIFIED || n == ObjectStatus.NEW);
		});
	}

	private void wireRuntimeInspector() {
		if (runtimeBridge == null)
			return;
		runtimeBridge.setOnPauseInspector(this::refreshInspector);
		runtimeBridge.runningProperty().addListener((obs, wasRunning, isRunning) -> {
			if (Boolean.TRUE.equals(isRunning))
				showInspectorPanel();
		});
	}

	private void refreshInspector() {
		if (runtimeBridge == null)
			return;
		GraphInstance instance = runtimeBridge.getLastInstance();
		if (instance == null) {
			inspectorPanel.showEmpty();
			return;
		}
		var sel = canvas.getSelectionModel().getSelection();
		if (sel.size() == 1) {
			UUID id = sel.iterator().next();
			if (isEdge(id))
				inspectorPanel.showEdgeContext(id, instance);
			else
				inspectorPanel.showNodeContext(id, instance);
		} else {
			inspectorPanel.showFlowContext(instance);
		}
	}

	private boolean isEdge(UUID id) {
		if (canvas.getGraph() == null)
			return false;
		for (var edge : canvas.getGraph().getEdges()) {
			if (edge.getId().equals(id))
				return true;
		}
		return false;
	}

	private void showInspectorPanel() {
		if (!rightPanels.getItems().contains(inspectorPanel)) {
			rightPanels.getItems().add(inspectorPanel);
			rightPanels.setDividerPositions(0.6);
		}
	}

	// =========================================================================
	// Context
	// =========================================================================

	public void registerInContext(Context context) {
		this.editorContext = context;
		context.addValue(this);
	}

	public void unregisterFromContext() {
		if (editorContext != null) {
			editorContext.removeValue(this);
			editorContext = null;
		}
	}

	// =========================================================================
	// INew
	// =========================================================================

	@Override
	public void newDocument() {
		fileManager.newDocument();
		editingPane.showRoot();
	}

	@Override
	public BooleanProperty getNewEnabled() {
		return newEnabled;
	}

	// =========================================================================
	// ISave
	// =========================================================================

	@Override
	public void save() {
		fileManager.save(getWindow());
	}

	@Override
	public BooleanProperty getSaveEnabled() {
		return saveEnabled;
	}

	// =========================================================================
	// IOpen
	// =========================================================================

	@Override
	public void open() {
		fileManager.open(getWindow());
	}

	@Override
	public BooleanProperty getOpenEnabled() {
		return openEnabled;
	}

	// =========================================================================
	// IUndo
	// =========================================================================

	@Override
	public void undo() {
		canvas.getCommandHistory().undo();
		canvas.refreshGraph();
	}

	@Override
	public BooleanProperty getUndoEnabled() {
		return wrapReadOnly(canvas.getCommandHistory().canUndoProperty());
	}

	// =========================================================================
	// IRedo
	// =========================================================================

	@Override
	public void redo() {
		canvas.getCommandHistory().redo();
		canvas.refreshGraph();
	}

	@Override
	public BooleanProperty getRedoEnabled() {
		return wrapReadOnly(canvas.getCommandHistory().canRedoProperty());
	}

	// =========================================================================
	// IClose
	// =========================================================================

	@Override
	public void close() {
	}

	@Override
	public BooleanProperty getCloseEnabled() {
		return closeEnabled;
	}

	// =========================================================================
	// ITitleProvider
	// =========================================================================

	@Override
	public Property<String> titleProperty() {
		return documentState.titleProperty();
	}

	@Override
	public String getTitle() {
		return documentState.getTitle();
	}

	// =========================================================================
	// IStackedComponent
	// =========================================================================

	@Override
	public void afterPush() {
		stateManager.restoreState(this);
	}

	@Override
	public void beforePop() {
		stateManager.persistState(this);
	}

	// =========================================================================
	// IGraphEditor
	// =========================================================================

	@Override
	public GraphCanvas getCanvas() {
		return canvas;
	}

	@Override
	public GraphRuntimeBridge getRuntimeBridge() {
		return runtimeBridge;
	}

	@Override
	public BooleanProperty notRunningProperty() {
		return notRunning;
	}

	@Override
	public BooleanProperty runningProperty() {
		return running;
	}

	@Override
	public BooleanProperty pausedProperty() {
		return paused;
	}

	@Override
	public BooleanProperty hasSelectionProperty() {
		return hasSelection;
	}

	@Override
	public BooleanProperty singleSelectionProperty() {
		return singleSelection;
	}

	@Override
	public void saveDocumentAs() {
		fileManager.saveAs(getWindow());
	}

	// =========================================================================
	// Accessors
	// =========================================================================

	public GraphDocumentState getDocumentState() {
		return documentState;
	}

	public GraphFileManager getFileManager() {
		return fileManager;
	}

	public GraphPalette getPalette() {
		return palette;
	}

	public GraphPropertyPanel getPropertyPanel() {
		return propertyPanel;
	}

	// =========================================================================
	// Internal
	// =========================================================================

	private Window getWindow() {
		return getScene() != null ? getScene().getWindow() : null;
	}

	private static SimpleBooleanProperty wrapReadOnly(javafx.beans.property.ReadOnlyBooleanProperty readOnly) {
		SimpleBooleanProperty writable = new SimpleBooleanProperty(readOnly.get());
		writable.bind(readOnly);
		return writable;
	}
}
