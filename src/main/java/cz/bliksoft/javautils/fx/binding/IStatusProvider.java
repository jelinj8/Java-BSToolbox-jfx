package cz.bliksoft.javautils.fx.binding;

import javafx.beans.property.Property;

/**
 * provide item status
 */
public interface IStatusProvider {
	public Property<ObjectStatus> getStatus();
}
