package cz.bliksoft.javautils.app.ui.builder;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;

public class UIBuildContext {
	public SlotResolver slotResolver = new SlotResolver();

	public ToggleGroupResolver toggleGoupResolver = new ToggleGroupResolver();

	private final AcceleratorManager accelerators = new AcceleratorManager();

	public AcceleratorManager accelerators() {
		return accelerators;
	}

	public FileObject currentBuildObject = null;
}