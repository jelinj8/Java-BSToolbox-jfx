package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IAdd;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

/**
 * Context-aware action for {@link IAdd}: adds a new item to the current context
 * object. Visible and enabled while an {@link IAdd} object is present in the
 * context.
 */
public class AddAction extends BasicContextUIAction<IAdd> {

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
		return "/icons/base/ADD_16.png";
	}

	@Override
	public String getKey() {
		return "Add";
	}
}
