package cz.bliksoft.javautils.app.ui.interfaces;

import cz.bliksoft.javautils.app.permissions.Permission;
import javafx.scene.Node;

public interface IAdministrationProvider {

	String getKey();

	String getTreeTitle();

	default String getPanelTitle() {
		return getTreeTitle();
	}

	default Node getSmallIcon() {
		return null;
	}

	default Node getLargeIcon() {
		return null;
	}

	Node getAdministrationComponent();

	/**
	 * Permission required to access this provider. null = always visible (no
	 * restriction).
	 */
	default Class<? extends Permission> getRequiredPermission() {
		return null;
	}
}
