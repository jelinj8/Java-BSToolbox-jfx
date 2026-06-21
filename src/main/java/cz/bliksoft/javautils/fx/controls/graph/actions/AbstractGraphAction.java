package cz.bliksoft.javautils.fx.controls.graph.actions;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.fx.controls.graph.IGraphEditor;
import javafx.scene.input.KeyCombination;

public abstract class AbstractGraphAction extends BasicContextUIAction<IGraphEditor> {

	protected AbstractGraphAction() {
		super(IGraphEditor.class);
	}

	protected AbstractGraphAction(String accelerator) {
		super(IGraphEditor.class);
		if (accelerator != null)
			setAccelerator(KeyCombination.keyCombination(accelerator));
	}

	@Override
	protected String getBaseIconSpec() {
		return null;
	}
}
