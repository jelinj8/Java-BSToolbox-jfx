package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.interfaces.IIconSpecPropertyProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.stage.Stage;

/**
 * Toggles the primary application window between maximized and its previous
 * bounds. Text and icon reflect the current window state.
 */
public class MaximizeRestoreAction implements IUIAction, IIconSpecPropertyProvider {

	private final ReadOnlyStringWrapper text = new ReadOnlyStringWrapper();
	private final Property<String> iconSpec = new SimpleStringProperty();

	/** Creates a new maximize/restore toggle action. */
	public MaximizeRestoreAction() {
		Stage stage = BSAppUI.getStage();
		text.bind(Bindings.createStringBinding(() -> stage.isMaximized() ? restoreText() : maximizeText(),
				stage.maximizedProperty()));
		((SimpleStringProperty) iconSpec).bind(Bindings.createStringBinding(
				() -> IconspecUtils
						.getIconspec(stage.isMaximized() ? "action/window-restore" : "action/window-maximize"),
				stage.maximizedProperty()));
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
	public Property<String> iconSpecProperty() {
		return iconSpec;
	}

	@Override
	public String getKey() {
		return "MaximizeRestore";
	}

}
