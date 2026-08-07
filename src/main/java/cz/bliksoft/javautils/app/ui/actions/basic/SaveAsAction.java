package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.ISaveAs;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

public class SaveAsAction extends BasicContextUIAction<ISaveAs> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("SaveAsAction.text")); //$NON-NLS-1$

	public SaveAsAction() {
		super(ISaveAs.class);
	}

	@Override
	protected void execute(ISaveAs current) {
		current.saveAs();
	}

	@Override
	protected BooleanProperty getEnabledProperty(ISaveAs current) {
		return current.getSaveAsEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(ISaveAs current) {
		return current.getSaveAsIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/save-as"); //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "SaveAs";
	}

}
