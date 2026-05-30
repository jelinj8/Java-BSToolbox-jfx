package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.help.BSAppHelpMessages;
import cz.bliksoft.javautils.app.ui.help.HelpAboutPane;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class ShowAboutAction implements IUIAction {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppHelpMessages.getString("ShowAboutAction.text")); //$NON-NLS-1$

	@Override
	public void execute() {
		Dialog<Void> dlg = new Dialog<>();
		dlg.setTitle(BSAppHelpMessages.getString("HelpAboutDialog.title")); //$NON-NLS-1$
		dlg.initOwner(BSAppUI.getStage());
		dlg.getDialogPane().setContent(createAboutPane());
		dlg.getDialogPane().setPrefSize(700, 500);
		dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		dlg.showAndWait();
	}

	/**
	 * Creates the pane shown inside the About dialog. Override in a subclass to
	 * supply a {@link java.util.ResourceBundle} for template translations:
	 *
	 * <pre>
	 * protected HelpAboutPane createAboutPane() {
	 * 	return new HelpAboutPane(ResourceBundle.getBundle("com.myapp.HelpMessages"));
	 * }
	 * </pre>
	 */
	protected HelpAboutPane createAboutPane() {
		return new HelpAboutPane();
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "ShowAbout"; //$NON-NLS-1$
	}
}
