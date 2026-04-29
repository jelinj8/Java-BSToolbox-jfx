package cz.bliksoft.javautils.fx.binding;

public interface IStatusBean extends IStatusProvider {

	default void touched() {
		switch (getStatus().getValue()) {
		case INITIAL:
		case CHILD_MODIFIED:
		case SAVED:
			getStatus().setValue(ObjectStatus.MODIFIED);
			break;
		case DETACHED:
			break;
		case MODIFIED:
			break;
		case NEW:
			break;
		case DELETED:
		case DELETED_SAVED:
			break;
		}

		if (this instanceof IParentedStatusBean) {
			IStatusBean parent = ((IParentedStatusBean<?>) this).getParent();
			if (parent != null)
				parent.childTouched();
		}
	}

	default void childTouched() {
		switch (getStatus().getValue()) {
		case CHILD_MODIFIED:
			break;
		case DETACHED:
			break;
		case MODIFIED:
			break;
		case NEW:
			break;
		case DELETED:
		case DELETED_SAVED:
			break;
		case INITIAL:
		case SAVED:
			getStatus().setValue(ObjectStatus.CHILD_MODIFIED);
			break;
		}

		if (this instanceof IParentedStatusBean) {
			IStatusBean parent = ((IParentedStatusBean<?>) this).getParent();
			if (parent != null)
				parent.childTouched();
		}
	}

	/**
	 * Enables or disables status tracking. While {@code false} property changes do
	 * not transition the bean to {@code MODIFIED}.
	 *
	 * @param watch {@code true} to start tracking changes
	 */
	void setWatched(boolean watch);

	/**
	 * Returns {@code true} if this bean is currently tracking property changes for
	 * status transitions.
	 *
	 * @return {@code true} if change tracking is active
	 */
	boolean isWatched();

}
