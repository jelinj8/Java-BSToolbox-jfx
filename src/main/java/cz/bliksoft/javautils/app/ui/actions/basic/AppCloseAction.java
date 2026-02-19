package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.events.TryCloseEvent;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;

public class AppCloseAction implements IUIAction {

	@Override
	public void execute() {
		TryCloseEvent.fire("AppClose action");
	}

	private static final ReadOnlyStringProperty CONST_TEXT = new ReadOnlyStringWrapper("konec");
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
