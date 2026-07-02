package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFX;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.help.BSAppHelpMessages;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;

public class ShowHelpAction implements IUIAction {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppHelpMessages.getString("ShowHelpAction.text")); //$NON-NLS-1$

	private final String url;
	private final ReadOnlyBooleanProperty available;

	public ShowHelpAction() {
		FileObject ui = FileSystem.getFile(BSAppJFX.CORE_CONFIG_FOLDER, "ui"); //$NON-NLS-1$
		FileObject helpCfg = ui != null ? ui.getFile("help") : null; //$NON-NLS-1$
		String resolved = helpCfg != null ? helpCfg.getAttribute("url", null) : null; //$NON-NLS-1$
		url = (resolved != null && !resolved.isBlank()) ? resolved : null;
		available = new ReadOnlyBooleanWrapper(url != null);
	}

	@Override
	public void execute() {
		BSAppJFX.getApplication().getHostServices().showDocument(url);
	}

	@Override
	public ObservableBooleanValue enabledProperty() {
		return available;
	}

	@Override
	public ObservableBooleanValue visibleProperty() {
		return available;
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "ShowHelp"; //$NON-NLS-1$
	}
}
