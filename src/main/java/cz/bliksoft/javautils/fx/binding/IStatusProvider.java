package cz.bliksoft.javautils.fx.binding;

import javafx.beans.property.Property;

/**
 * Provides the lifecycle status of a bean as an observable {@link ObjectStatus}
 * property.
 */
public interface IStatusProvider {
	/**
	 * Returns the mutable status property of this bean.
	 *
	 * @return non-null property holding the current {@link ObjectStatus}
	 */
	Property<ObjectStatus> getStatus();
}
