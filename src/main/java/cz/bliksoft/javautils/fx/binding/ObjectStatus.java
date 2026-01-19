package cz.bliksoft.javautils.fx.binding;

import java.util.List;

import javafx.scene.Node;

public enum ObjectStatus implements IIconNodeProvider, IClassesProvider {
	INITIAL, NEW, SAVED, MODIFIED, CHILD_MODIFIED, DETACHED, DELETED, DELETED_SAVED;

	public String getSVGPath() {
		switch (this) {
		default:
			return "";
		}
	}

	@Override
	public Node getImageNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getItemClasses() {
		return null;
	}
}
