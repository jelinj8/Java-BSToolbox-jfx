package cz.bliksoft.javautils.app.ui.actions;

import javafx.collections.ObservableList;

/**
 * Extension of {@link IUIAction} for actions that expose a list of sub-actions,
 * typically rendered as a dropdown or split-button in the toolbar.
 *
 * <p>
 * The list is observable so that the UI can react to options being added or
 * removed at runtime. The default {@link #execute()} of the parent action (i.e.
 * the left-click of a split-button) should run the first sub-action when
 * available.
 */
public interface IUIActionWithSubactions extends IUIAction {
	/**
	 * Returns the live list of sub-actions for this action. UI components bound to
	 * this action should observe the list for changes.
	 *
	 * @return non-null observable list of sub-actions
	 */
	ObservableList<IUIAction> getSubactions();
}
