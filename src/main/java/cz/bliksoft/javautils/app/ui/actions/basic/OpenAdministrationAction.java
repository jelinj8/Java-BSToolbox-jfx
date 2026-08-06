package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.permissions.UserInfo;
import cz.bliksoft.javautils.app.permissions.basic.PermissionOpenAdministration;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.app.ui.administration.AdministrationPanel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Extends {@link cz.bliksoft.javautils.app.ui.actions.UIActionBase} (via
 * {@link BasicContextUIAction}), so a {@code keys} attribute on the action's
 * {@code core/actions} XmlFilesystem node is applied as a keyboard accelerator,
 * and {@link #textProperty()} — combined with that accelerator by
 * {@code ActionBinder.bindHint} — supplies the button/menu-item tooltip.
 */
public class OpenAdministrationAction extends BasicContextUIAction<UserInfo> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppAdministrationMessages.getString("OpenAdministrationAction.text")); //$NON-NLS-1$

	public OpenAdministrationAction() {
		super(UserInfo.class);
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	protected void execute(UserInfo current) {
		if (AdministrationPanel.isOpen())
			return;
		BSAppUI.pushUI(new AdministrationPanel());
	}

	@Override
	protected BooleanProperty getEnabledProperty(UserInfo current) {
		return new SimpleBooleanProperty(current.isAllowed(PermissionOpenAdministration.class));
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/administration"); //$NON-NLS-1$
	}

	@Override
	protected String getBaseMenuIconSpec() {
		return IconspecUtils.getMenuIconspec("action/administration"); //$NON-NLS-1$
	}

	@Override
	public String getKey() {
		return "OpenAdministration";
	}
}
