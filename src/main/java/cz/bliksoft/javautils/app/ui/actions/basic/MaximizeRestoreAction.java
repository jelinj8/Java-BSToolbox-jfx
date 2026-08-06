package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.UIActionBase;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.stage.Stage;

/**
 * Toggles the primary application window between maximized and its previous
 * bounds. Text and icon reflect the current window state.
 *
 * <p>
 * Extends {@link UIActionBase} so a {@code keys} attribute on the action's
 * {@code core/actions} XmlFilesystem node is applied as a keyboard accelerator.
 */
public class MaximizeRestoreAction extends UIActionBase {

	private final ReadOnlyStringWrapper text = new ReadOnlyStringWrapper();

	/** Creates a new maximize/restore toggle action. */
	public MaximizeRestoreAction() {
		Stage stage = BSAppUI.getStage();
		text.bind(Bindings.createStringBinding(() -> stage.isMaximized() ? restoreText() : maximizeText(),
				stage.maximizedProperty()));
		setIconSpec(IconspecUtils.getIconspec("action/window-maximize"));
		stage.maximizedProperty().addListener((obs, o, isMaximized) -> setIconSpec(
				IconspecUtils.getIconspec(isMaximized ? "action/window-restore" : "action/window-maximize")));
	}

	@Override
	public void execute() {
		Stage stage = BSAppUI.getStage();
		stage.setMaximized(!stage.isMaximized());
	}

	private static String maximizeText() {
		return BSAppJFXMessages.getString("MaximizeRestoreAction.maximize.text");
	}

	private static String restoreText() {
		return BSAppJFXMessages.getString("MaximizeRestoreAction.restore.text");
	}

	private static final ReadOnlyBooleanProperty CONST_ENABLED = new ReadOnlyBooleanWrapper(true);

	@Override
	public ObservableBooleanValue enabledProperty() {
		return CONST_ENABLED;
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return text.getReadOnlyProperty();
	}

	@Override
	public String getKey() {
		return "MaximizeRestore";
	}

}
