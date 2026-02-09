package cz.bliksoft.javautils.app.ui.actions.basic;

import java.util.Optional;

import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class AppClose implements IUIAction {

	@Override
	public void execute() {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ukončení aplikace");
        alert.setHeaderText("Opravdu chcete ukončit aplikaci?");
        alert.setContentText("Neuložené změny mohou být ztraceny.");

        alert.initOwner(BSAppUI.getStage());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
        }
	}

	private static final ReadOnlyStringProperty CONST_TEXT =
	        new ReadOnlyStringWrapper("konec");
	private static final ReadOnlyBooleanProperty CONST_ENABLED =
	        new ReadOnlyBooleanWrapper(true);

	
	@Override
	public ObservableBooleanValue enabledProperty() {
		return CONST_ENABLED;
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return CONST_TEXT;
	}

	@Override
	public ReadOnlyStringProperty iconSpecProperty() {
		return null;
	}

	@Override
	public String getKey() {
		return "AppClose";
	}

}
