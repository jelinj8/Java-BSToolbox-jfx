package cz.bliksoft.javautils.app.ui.actions;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;

/**
 * An {@link IUIAction} configured by its {@code core/actions} node - so one
 * class can serve several actions, each an XML entry naming the class in a
 * {@code class} attribute (the node name is then free, e.g. the action's key):
 *
 * <pre>
 * &lt;file name="SMPrintObjectTinyLabel"&gt;
 *   &lt;attribute name="class" value="com.example.ObjectPrintTaskAction"/&gt;
 *   &lt;attribute name="task" value="printObjectTinyLabel"/&gt;
 * &lt;/file&gt;
 * </pre>
 *
 * {@link UIActions} calls {@link #configure} right after creating the action,
 * before applying its shortcut and registering it.
 */
public interface IConfigurableUIAction extends IUIAction {

	/** Configures the action from its {@code core/actions} node. */
	void configure(FileObject node);
}
