package cz.bliksoft.javautils.app.rights;

import cz.bliksoft.javautils.app.BSAppMessages;

public abstract class Right {
	public abstract String getName();

	public abstract String getAlias();

	public String getCategory() {
		return BSAppMessages.getString("Rights.UNCATEGORIZED");
	}

	public String getShortDescription() {
		return null;
	}

	public String getDescription() {
		return null;
	}
}
