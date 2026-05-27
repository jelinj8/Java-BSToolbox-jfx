package cz.bliksoft.javautils.app.ui.interfaces;

public interface IStackedComponent {
	default void afterPush() {
	};

	default void beforePop() {
	};
}
