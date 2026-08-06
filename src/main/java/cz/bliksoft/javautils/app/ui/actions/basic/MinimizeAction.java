package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.UIActionBase;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;

/**
 * Minimizes the primary application window.
 *
 * <p>
 * Extends {@link UIActionBase} so a {@code keys} attribute on the action's
 * {@code core/actions} XmlFilesystem node is applied as a keyboard accelerator.
 */
public class MinimizeAction extends UIActionBase {

	/** Creates a new minimize action. */
	public MinimizeAction() {
		setIconSpec(IconspecUtils.getIconspec("action/window-minimize"));
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
	public String getKey() {
		return "Minimize";
	}

}
