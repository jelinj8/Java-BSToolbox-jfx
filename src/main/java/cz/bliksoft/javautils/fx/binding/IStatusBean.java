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

	void setWatched(boolean watch);

	boolean isWatched();

}
