package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class GraphCommandHistory {

	private final List<IGraphCommand> history = new ArrayList<>();
	private int position = -1;
	private final int maxDepth;

	private final BooleanProperty canUndo = new SimpleBooleanProperty(false);
	private final BooleanProperty canRedo = new SimpleBooleanProperty(false);

	public GraphCommandHistory() {
		this(200);
	}

	public GraphCommandHistory(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public void execute(IGraphCommand command) {
		command.execute();

		while (history.size() > position + 1)
			history.removeLast();

		history.add(command);
		position = history.size() - 1;

		if (history.size() > maxDepth) {
			history.removeFirst();
			position--;
		}

		updateProperties();
	}

	public void undo() {
		if (position < 0)
			return;
		history.get(position).undo();
		position--;
		updateProperties();
	}

	public void redo() {
		if (position >= history.size() - 1)
			return;
		position++;
		history.get(position).redo();
		updateProperties();
	}

	public void clear() {
		history.clear();
		position = -1;
		updateProperties();
	}

	private void updateProperties() {
		canUndo.set(position >= 0);
		canRedo.set(position < history.size() - 1);
	}

	public ReadOnlyBooleanProperty canUndoProperty() {
		return canUndo;
	}

	public ReadOnlyBooleanProperty canRedoProperty() {
		return canRedo;
	}

	public boolean canUndo() {
		return canUndo.get();
	}

	public boolean canRedo() {
		return canRedo.get();
	}

	public String getUndoDescription() {
		return position >= 0 ? history.get(position).getDescription() : null;
	}

	public String getRedoDescription() {
		return position < history.size() - 1 ? history.get(position + 1).getDescription() : null;
	}
}
