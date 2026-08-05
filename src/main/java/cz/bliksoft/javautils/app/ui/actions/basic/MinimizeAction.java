package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.interfaces.IIconSpecPropertyProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;

/** Minimizes the primary application window. */
public class MinimizeAction implements IUIAction, IIconSpecPropertyProvider {

	private final Property<String> iconSpec = new SimpleStringProperty(
			IconspecUtils.getIconspec("action/window-minimize"));

	/** Creates a new minimize action. */
	public MinimizeAction() {
	}

	@Override
	public void execute() {
		BSAppUI.getStage().setIconified(true);
	}

	private static final ReadOnlyStringProperty CONST_TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("MinimizeAction.text"));
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
	public Property<String> iconSpecProperty() {
		return iconSpec;
	}

	@Override
	public String getKey() {
		return "Minimize";
	}

}
