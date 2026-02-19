package cz.bliksoft.javautils.app.ui;

public interface IStackedComponent {
	default void afterPush() {
	};

	default void beforePop() {
	};
}
