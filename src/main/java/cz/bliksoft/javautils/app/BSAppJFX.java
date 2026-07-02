package cz.bliksoft.javautils.app;

import cz.bliksoft.javautils.fx.controls.images.AnyImageLoader;
import cz.bliksoft.javautils.fx.controls.images.ImageLoader;
import javafx.application.Application;
import javafx.application.Platform;

/**
 * JavaFX extension of {@link BSApp} that wires the modular framework to a
 * running JavaFX {@link Application}. Stores the application instance, installs
 * {@code Platform.exit()} as the shutdown hook, and sets {@link AnyImageLoader}
 * as the default image loader. Use {@link #init(Application)} from
 * {@link Application#start} instead of {@link BSApp#init()}.
 */
public class BSAppJFX extends BSApp {

	private static Application jFXApp = null;

	/**
	 * Returns the running JavaFX application instance.
	 *
	 * @return the application instance, or {@code null} before {@link #init} is
	 *         called
	 */
	public static Application getApplication() {
		return jFXApp;
	}

	/**
	 * Initializes the application framework for a JavaFX application.
	 *
	 * <p>
	 * Stores {@code app} as the running application instance, installs
	 * {@code Platform.exit()} as the before-exit hook, delegates to
	 * {@link BSApp#init()} to load modules and configure the framework, and sets
	 * {@link AnyImageLoader} as the default image loader. Must be called once from
	 * {@link Application#start} before any UI is shown.
	 *
	 * @param app the running JavaFX application instance
	 */
	public static void init(Application app) {
		jFXApp = app;
		BSApp.setBeforeExitHook(new Runnable() {
			@Override
			public void run() {
				Platform.exit();
			}
		});
		BSApp.init();

		ImageLoader.setDefault(new AnyImageLoader());
	}

}
