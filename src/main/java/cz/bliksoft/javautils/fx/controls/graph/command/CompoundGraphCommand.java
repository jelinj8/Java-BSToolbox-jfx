package cz.bliksoft.javautils.fx.controls.graph.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompoundGraphCommand implements IGraphCommand {

	private final String description;
	private final List<IGraphCommand> commands;

	public CompoundGraphCommand(String description, List<IGraphCommand> commands) {
		this.description = description;
		this.commands = new ArrayList<>(commands);
	}

	@Override
	public void execute() {
		for (IGraphCommand cmd : commands)
			cmd.execute();
	}

	@Override
	public void undo() {
		List<IGraphCommand> reversed = new ArrayList<>(commands);
		Collections.reverse(reversed);
		for (IGraphCommand cmd : reversed)
			cmd.undo();
	}

	@Override
	public void redo() {
		for (IGraphCommand cmd : commands)
			cmd.redo();
	}

	@Override
	public String getDescription() {
		return description;
	}
}
