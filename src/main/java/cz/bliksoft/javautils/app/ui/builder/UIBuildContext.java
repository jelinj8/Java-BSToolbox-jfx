package cz.bliksoft.javautils.app.ui.builder;

import cz.bliksoft.javautils.app.ui.actions.IUIAction;

public class UIBuildContext {
	public SlotResolver slotResolver = new SlotResolver();
	
	private final AcceleratorManager accelerators = new AcceleratorManager();
	public AcceleratorManager accelerators() { return accelerators; }
	
	public IUIAction getAction(String key) {
//		Object obj = registry.getRaw(key);
//		IUIAction a = (obj instanceof IUIAction ua) ? ua
//		          : (obj instanceof org.controlsfx.control.action.Action cfx) ? UIActions.fromControlsFx(cfx)
//		          : throw ...

		return null;
	}
}