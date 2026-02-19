package cz.bliksoft.javautils.app.ui.utils.state;

import javafx.scene.Node;

public interface FxStateBinder {
	boolean supports(Node n);

	void save(Node n, String prefix);

	void restore(Node n, String prefix);
}
