package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IAdd;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

/**
 * Context-aware action for {@link IAdd}: adds a new item to the current context
 * object. Visible and enabled while an {@link IAdd} object is present in the
 * context.
 */
public class AddAction extends BasicContextUIAction<IAdd> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("AddAction.text")); //$NON-NLS-1$

	/** Creates the action and registers it with the current context. */
	public AddAction() {
		super(IAdd.class);
	}

	@Override
	protected void execute(IAdd current) {
		current.add();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IAdd current) {
		return current.getAddEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IAdd current) {
		return current.getAddIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/add"); //$NON-NLS-1$
	}

	@Override
	protected String getBaseMenuIconSpec() {
		return IconspecUtils.getMenuIconspec("action/add"); //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "Add";
	}
}
