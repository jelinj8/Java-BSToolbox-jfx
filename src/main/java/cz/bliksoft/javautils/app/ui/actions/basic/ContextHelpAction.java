package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IContextHelp;
import cz.bliksoft.javautils.app.ui.help.BSAppHelpMessages;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public class ContextHelpAction extends BasicContextUIAction<IContextHelp> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppHelpMessages.getString("ContextHelpAction.text")); //$NON-NLS-1$

	public ContextHelpAction() {
		super(IContextHelp.class);
	}

	@Override
	protected void execute(IContextHelp current) {
		current.openHelp();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IContextHelp current) {
		return null;
	}

	@Override
	protected String getBaseIconSpec() {
		return "24/HELP.png"; //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "ContextHelp"; //$NON-NLS-1$
	}
}
