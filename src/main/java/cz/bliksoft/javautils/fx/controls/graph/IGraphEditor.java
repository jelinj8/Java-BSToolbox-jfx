package cz.bliksoft.javautils.fx.controls.graph;

import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphMonitorController;
import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;

public interface IGraphEditor {

	GraphCanvas getCanvas();

	GraphRuntimeBridge getRuntimeBridge();

	/**
	 * @return the controller that attaches the editor to a managed graph run for
	 *         read-only monitoring/inspection
	 */
	GraphMonitorController getMonitorController();

	BooleanProperty notRunningProperty();

	BooleanProperty runningProperty();

	BooleanProperty pausedProperty();

	BooleanProperty hasSelectionProperty();

	BooleanProperty singleSelectionProperty();

	void saveDocumentAs();
}
