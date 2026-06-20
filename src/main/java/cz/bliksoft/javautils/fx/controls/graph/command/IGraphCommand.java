package cz.bliksoft.javautils.fx.controls.graph.command;

public interface IGraphCommand {

	void execute();

	void undo();

	void redo();

	String getDescription();
}
