package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.ISave;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

public class SaveAction extends BasicContextUIAction<ISave> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("SaveAction.text")); //$NON-NLS-1$

	public SaveAction() {
		super(ISave.class);
	}

	@Override
	protected void execute(ISave current) {
		current.save();
	}

	@Override
	protected BooleanProperty getEnabledProperty(ISave current) {
		return current.getSaveEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(ISave current) {
		return current.getSaveIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/save"); //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "Save";
	}

}
