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
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/**
 * Toggles fullscreen mode on the primary application window.
 *
 * <p>
 * Extends {@link UIActionBase} so a {@code keys} attribute on the action's
 * {@code core/actions} XmlFilesystem node is applied as a keyboard accelerator.
 *
 * <p>
 * Instantiating this action (i.e. the app has linked it into
 * {@code core/actions}) disables OpenJFX's built-in Esc-to-exit-fullscreen
 * shortcut on the primary stage — with the toggle bound to a deliberate key
 * such as {@code F11}, Esc becomes an easy way to accidentally drop out of
 * fullscreen. The exit hint overlay is replaced accordingly instead of showing
 * the (now wrong) default "Press Esc" text.
 */
public class FullscreenToggleAction extends UIActionBase {

	/** Creates a new fullscreen toggle action. */
	public FullscreenToggleAction() {
		setIconSpec(IconspecUtils.getIconspec("action/window-fullscreen"));

		Stage stage = BSAppUI.getStage();
		stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
		stage.setFullScreenExitHint(BSAppJFXMessages.getString("FullscreenToggleAction.exitHint"));
	}

	@Override
	public void execute() {
		javafx.stage.Stage stage = BSAppUI.getStage();
		stage.setFullScreen(!stage.isFullScreen());
	}

	private static final ReadOnlyStringProperty CONST_TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("FullscreenToggleAction.text"));
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
		return "FullscreenToggle";
	}

}
