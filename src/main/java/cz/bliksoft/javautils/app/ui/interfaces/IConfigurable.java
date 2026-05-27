package cz.bliksoft.javautils.app.ui.interfaces;

public interface IConfigurable {

	void configure();

	default boolean isConfigurable() {
		return true;
	}
}
