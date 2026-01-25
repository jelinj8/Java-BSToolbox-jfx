package cz.bliksoft.javautils.app.ui.builder;

import javafx.scene.Node;

public interface ISlotPublisher {
	void publishSlots(SlotResolver slots, Node root);
}
