package cz.bliksoft.javautils.fx.controls.graph;

import cz.bliksoft.dataflow.engine.GraphExecutor;
import cz.bliksoft.javautils.app.ui.actions.ActionBinder;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.UIActions;
import cz.bliksoft.javautils.app.ui.builder.AcceleratorManager;
import cz.bliksoft.javautils.app.ui.utils.state.FxWindowState;
import cz.bliksoft.javautils.context.Context;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GraphEditorDialog {

	private static final String WINDOW_KEY = "graphEditor";

	private final Stage stage;
	private final GraphEditorPanel panel;
	private final Context dialogContext;

	public GraphEditorDialog() {
		this(null);
	}

	public GraphEditorDialog(GraphExecutor executor) {
		panel = new GraphEditorPanel(executor);

		dialogContext = new Context("GraphEditorDialog");
		panel.registerInContext(dialogContext);

		MenuBar menuBar = buildMenuBar();
		ToolBar fileToolBar = panel.getFileToolBar();
		ToolBar editToolBar = panel.getEditToolBar();
		ToolBar graphToolBar = panel.getGraphToolBar();

		HBox toolBars = new HBox(4, fileToolBar, editToolBar, graphToolBar);
		HBox.setHgrow(graphToolBar, Priority.ALWAYS);

		VBox topArea = new VBox(menuBar, toolBars);

		BorderPane root = new BorderPane();
		root.setTop(topArea);
		root.setCenter(panel);

		Scene scene = new Scene(root, 1200, 800);
		stage = new Stage();
		stage.setScene(scene);
		stage.titleProperty().bind(javafx.beans.binding.Bindings.concat(panel.titleProperty(), " - Graph Editor"));

		AcceleratorManager accel = new AcceleratorManager();
		accel.attach(scene);
		bindAccelerators(accel);

		stage.setOnShowing(e -> Context.setCurrentContext(dialogContext));
		stage.focusedProperty().addListener((obs, o, n) -> {
			if (Boolean.TRUE.equals(n))
				Context.setCurrentContext(dialogContext);
		});

		stage.setOnHidden(e -> {
			FxWindowState.persistWindow(stage, root, WINDOW_KEY);
			panel.unregisterFromContext();
			try {
				cz.bliksoft.javautils.app.BSApp.saveLocalProperties();
			} catch (Exception ex) {
				org.apache.logging.log4j.LogManager.getLogger(GraphEditorDialog.class).warn("Failed to save properties",
						ex);
			}
		});

		FxWindowState.restoreWindow(stage, root, WINDOW_KEY);
	}

	public void show() {
		stage.show();
		panel.getCanvas().requestFocus();
	}

	public GraphEditorPanel getPanel() {
		return panel;
	}

	public Stage getStage() {
		return stage;
	}

	// =========================================================================
	// Menu bar
	// =========================================================================

	private MenuBar buildMenuBar() {
		Menu fileMenu = new Menu("File", null, actionMenuItem("New"), actionMenuItem("Open"), new SeparatorMenuItem(),
				actionMenuItem("Save"), actionMenuItem("GraphSaveAs"));

		Menu editMenu = new Menu("Edit", null, actionMenuItem("Undo"), actionMenuItem("Redo"), new SeparatorMenuItem(),
				actionMenuItem("GraphDelete"), new SeparatorMenuItem(), actionMenuItem("GraphGroup"),
				actionMenuItem("GraphUngroup"));

		Menu viewMenu = new Menu("View", null, actionMenuItem("GraphZoomToFit"), actionMenuItem("GraphZoom100"));

		Menu runMenu = new Menu("Run", null, actionMenuItem("GraphExecute"), actionMenuItem("GraphStepDebug"),
				new SeparatorMenuItem(), actionMenuItem("GraphStepOver"), actionMenuItem("GraphStepAll"),
				actionMenuItem("GraphResume"), actionMenuItem("GraphRunToEnd"), new SeparatorMenuItem(),
				actionMenuItem("GraphPause"), actionMenuItem("GraphStop"), new SeparatorMenuItem(),
				actionMenuItem("GraphToggleBP"), new SeparatorMenuItem(), actionMenuItem("GraphAttach"));

		return new MenuBar(fileMenu, editMenu, viewMenu, runMenu);
	}

	private MenuItem actionMenuItem(String actionKey) {
		MenuItem mi = new MenuItem(actionKey);
		IUIAction action = UIActions.getAction(actionKey);
		if (action != null)
			ActionBinder.bind(mi, action);
		return mi;
	}

	private void bindAccelerators(AcceleratorManager accel) {
		String[] keys = { "New", "Open", "Save", "GraphSaveAs", "Undo", "Redo", "GraphDelete", "GraphGroup",
				"GraphUngroup", "GraphExecute", "GraphStepDebug", "GraphStepOver", "GraphStepAll", "GraphResume",
				"GraphPause", "GraphStop", "GraphToggleBP" };
		for (String key : keys) {
			IUIAction action = UIActions.getAction(key);
			if (action != null)
				accel.bind(action);
		}
	}
}
