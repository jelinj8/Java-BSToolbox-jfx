package cz.bliksoft.javautils.fx.controls.graph.actions;

import java.util.List;

import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.UIActions;

public final class GraphEditorActions {

	private static boolean registered;

	private GraphEditorActions() {
	}

	public static synchronized void registerAll() {
		if (registered)
			return;
		registered = true;

		for (IUIAction action : createAll())
			UIActions.registerAction(action.getKey(), action, "graph");
	}

	public static List<IUIAction> createAll() {
		return List.of(new GraphExecuteAction(), new GraphStepDebugAction(), new GraphStepOverAction(),
				new GraphStepAllAction(), new GraphResumeAction(), new GraphRunToEndAction(), new GraphPauseAction(),
				new GraphStopAction(), new GraphToggleBreakpointAction(), new GraphAttachAction(),
				new GraphGroupAction(), new GraphUngroupAction(), new GraphDeleteAction(), new GraphZoomToFitAction(),
				new GraphZoom100Action(), new GraphSaveAsAction());
	}
}
