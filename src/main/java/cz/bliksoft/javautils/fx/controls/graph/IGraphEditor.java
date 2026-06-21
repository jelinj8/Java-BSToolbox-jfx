package cz.bliksoft.javautils.fx.controls.graph;

import cz.bliksoft.javautils.fx.controls.graph.runtime.GraphRuntimeBridge;
import javafx.beans.property.BooleanProperty;

public interface IGraphEditor {

	GraphCanvas getCanvas();

	GraphRuntimeBridge getRuntimeBridge();

	BooleanProperty notRunningProperty();

	BooleanProperty runningProperty();

	BooleanProperty pausedProperty();

	BooleanProperty hasSelectionProperty();

	BooleanProperty singleSelectionProperty();

	void saveDocumentAs();
}
