package cz.bliksoft.javautils.app.permissions;

import cz.bliksoft.javautils.app.BSAppMessages;

public abstract class Permission {
	public abstract String getName();

	public abstract String getAlias();

	public String getCategory() {
		return BSAppMessages.getString("Permissions.UNCATEGORIZED");
	}

	public String getShortDescription() {
		return null;
	}

	public String getDescription() {
		return null;
	}
}
