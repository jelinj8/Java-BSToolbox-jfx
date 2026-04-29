package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.events.TryCloseEvent;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;

/**
 * Application close action. Fires a {@code TryCloseEvent} which gives modules
 * the opportunity to veto the close (e.g. to prompt for unsaved changes).
 */
public class AppCloseAction implements IUIAction {

	/** Creates a new application close action. */
	public AppCloseAction() {
	}

	@Override
	public void execute() {
		TryCloseEvent.fire("AppClose action");
	}

	private static final ReadOnlyStringProperty CONST_TEXT = new ReadOnlyStringWrapper("Exit");
	private static final ReadOnlyBooleanProperty CONST_ENABLED = new ReadOnlyBooleanWrapper(true);

	@Override
	public ObservableBooleanValue enabledProperty() {
		return CONST_ENABLED;
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return CONST_TEXT;
	}

	@Override
	public String getKey() {
		return "AppClose";
	}

}
