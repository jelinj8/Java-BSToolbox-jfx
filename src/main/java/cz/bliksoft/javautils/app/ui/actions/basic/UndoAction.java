package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IUndo;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class UndoAction extends BasicContextUIAction<IUndo> {

	public UndoAction() {
		super(IUndo.class);
	}

	@Override
	protected void execute(IUndo current) {
		current.undo();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IUndo current) {
		return current.getUndoEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IUndo current) {
		return current.getUndoIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/undo"); //$NON-NLS-1$
	}

	@Override
	public String getKey() {
		return "Undo";
	}
}
