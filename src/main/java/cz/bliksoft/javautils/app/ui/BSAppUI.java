package cz.bliksoft.javautils.app.ui;

import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.dialog.ProgressDialog;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.events.MessageEvent;
import cz.bliksoft.javautils.app.events.TryCloseEvent;
import cz.bliksoft.javautils.app.ui.builder.UIComposer;
import cz.bliksoft.javautils.app.ui.utils.StageAutoSizer;
import cz.bliksoft.javautils.app.ui.utils.state.binders.StageStateBinder;
import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ContextChangedEvent;
import cz.bliksoft.javautils.context.ContextSearchResult;
import cz.bliksoft.javautils.context.IContextProvider;
import cz.bliksoft.javautils.context.holders.StackedContextHolder;
import cz.bliksoft.javautils.fx.VersionInfo;
import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import cz.bliksoft.javautils.fx.tools.Styling;
import cz.bliksoft.javautils.modules.ModuleBase;
import cz.bliksoft.javautils.modules.Modules;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class BSAppUI extends ModuleBase {
	static Logger log = null;

	public static final String CTX_MAIN_COMPONENT = "MainDesktopComponent"; //$NON-NLS-1$

	public static final String FS_UI_ROOT = "/AppUI"; //$NON-NLS-1$

	public static final String PROP_THEME = "ui.theme"; //$NON-NLS-1$

	private static Context uiContext = new Context("global UI context");

	private static StackedContextHolder uiContextHolder = null;

	public static Context getUIContext() {
		return uiContext;
	}

//	public static Context getUIContextHolder() {
//		return uiContextHolder;
//	}

	private static Stage mainStage = null;

//	public SimpleObjectProperty<Node> rootNode = new SimpleObjectProperty<Node>();
	public static BorderPane mainPane = null;

	public static void setStage(Stage stage) {
		mainStage = stage;
	}

	public static Stage getStage() {
		return mainStage;
	}

	@Override
	public int getModuleLoadingOrder() {
		return 10000;
	}

	/**
	 * inicializace aplikačního frameworku s UI. Vynutí si import jako BSAppUI
	 * modul.
	 * 
	 * @throws Exception
	 */
	public static void init(Application app, Stage stage) {
		setStage(stage);
		Modules.autoloadModule(BSAppUI.class);
		BSApp.init(app);
	}

	@Override
	public void init() {
		mainStage.setOnCloseRequest(event -> {
			TryCloseEvent.fire("Window closing");
			event.consume();
		});

		log = LogManager.getLogger();

		FileObject f = FileSystem.getRoot().getFile(FS_UI_ROOT);
		if (f != null) {
			String rootName = f.getAttribute("root", null);
			if (StringUtils.hasLength(rootName)) {
				f = f.getFile(rootName);

				if (f != null) {
					String theme = f.getAttribute("theme");
					if (theme != null) {
						switch (theme.toUpperCase()) {
						case "LIGHT":
							Styling.setThemeMode(Styling.ThemeMode.LIGHT);
							break;
						case "DARK":
							Styling.setThemeMode(Styling.ThemeMode.DARK);
							break;
						case "SYSTEM":
							Styling.setThemeMode(Styling.ThemeMode.SYSTEM);
							break;
						}

					} else {

					}
					Object localThemeObj = BSApp.getProperty(PROP_THEME);
					if (localThemeObj != null) {
						switch (localThemeObj.toString().toUpperCase()) {
						case "LIGHT":
							Styling.setThemeMode(Styling.ThemeMode.LIGHT);
							break;
						case "DARK":
							Styling.setThemeMode(Styling.ThemeMode.DARK);
							break;
						case "SYSTEM":
							Styling.setThemeMode(Styling.ThemeMode.SYSTEM);
							break;
						case "NONE":
							Styling.setThemeMode(Styling.ThemeMode.NONE);
							break;
						}
					}
					Styling.installGlobalCss();
					UIComposer.buildUI(f, mainStage);
					StageAutoSizer.install(mainStage);
				}
			}
		}

		uiContextHolder = new StackedContextHolder("UI StackedContextHolder");
		uiContextHolder.push(new Context("UI root context"));
		uiContext.addContext(uiContextHolder);

//		Context.getRoot().addContext(uiContext);
		Context.setCurrentContext(uiContext);

		if (mainPane != null) {
			Context.getCurrentContext().addContextListener(
					new AbstractContextListener<Node>(CTX_MAIN_COMPONENT, "Main component switcher") {

						@Override
						public void fired(ContextChangedEvent<Node> event) {
							mainPane.setCenter(null);
							if (event.isNewNotNull()) {
								mainPane.setCenter(event.getNewValue());
							}
							StageAutoSizer.autoSize();
						}
					});
		}

		StageStateBinder.restore(mainStage, "@main");
	}

	@Override
	public void install() {
		super.install();
		mainStage.show();
	}

	public static void showStatusMessage(String text) {
		MessageEvent e = new MessageEvent(text);
		Context.getCurrentContext().fireEvent(e);
	}

	public static void showStatusMessage(String text, String icon) {
		MessageEvent e = new MessageEvent(text, ImageUtils.getIconView(icon));
		Context.getCurrentContext().fireEvent(e);
	}

	public static void showStatusMessage(String text, String icon, String fxClass) {
		MessageEvent e = new MessageEvent(text, icon != null ? ImageUtils.getIconView(icon) : null, fxClass);
		Context.getCurrentContext().fireEvent(e);
	}

	/** Base task that exposes the protected update methods as public. */
	private abstract static class ProgressTask extends Task<Void> {
		@Override
		public void updateTitle(String title) {
			super.updateTitle(title);
		}

		@Override
		public void updateMessage(String msg) {
			super.updateMessage(msg);
		}

		@Override
		public void updateProgress(double done, double total) {
			super.updateProgress(done, total);
		}
	}

	private static final class WorkingTask extends ProgressTask {
		private final CountDownLatch latch = new CountDownLatch(1);

		WorkingTask(String title) {
			updateTitle(title);
		}

		@Override
		protected Void call() throws Exception {
			latch.await();
			return null;
		}

		void complete() {
			latch.countDown();
		}
	}

	private static volatile ProgressTask workingTask = null;

	/**
	 * Shows a modal indeterminate progress dialog. Returns immediately; call
	 * hideWorkingWheel() to close it.
	 */
	public static void showWorkingWheel(String comment) {
		WorkingTask task = new WorkingTask(comment);
		workingTask = task;
		Thread t = new Thread(task, "working-wheel");
		t.setDaemon(true);
		t.start();
		Platform.runLater(() -> {
			ProgressDialog dlg = new ProgressDialog(task);
			dlg.initOwner(mainStage);
			dlg.setTitle(comment);
			dlg.show();
		});
	}

	public static void setWorkingWheelTitle(String comment) {
		ProgressTask t = workingTask;
		if (t != null)
			t.updateTitle(comment);
	}

	public static void setWorkingWheelSubtitle(String comment) {
		ProgressTask t = workingTask;
		if (t != null)
			t.updateMessage(comment);
	}

	/**
	 * Updates progress on the active working wheel. Pass total=0 for indeterminate.
	 */
	public static void setWorkingWheelProgress(int done, int total) {
		ProgressTask t = workingTask;
		if (t != null)
			t.updateProgress(total == 0 ? -1 : done, total == 0 ? 1 : total);
	}

	/** Closes the dialog opened by showWorkingWheel(). */
	public static void hideWorkingWheel() {
		ProgressTask t = workingTask;
		workingTask = null;
		if (t instanceof WorkingTask)
			((WorkingTask) t).complete();
	}

	/**
	 * Runs toRun on a background thread behind a modal blocking progress dialog.
	 * Must be called from the FX thread.
	 */
	public static void executeWaiting(Runnable toRun, String comment) {
		executeWaiting(toRun, null, comment);
	}

	/**
	 * Runs toRun on a background thread behind a modal blocking progress dialog
	 * with a separate subtitle. Progress starts indeterminate; call
	 * setWorkingWheelProgress / setWorkingWheelSubtitle from the Runnable to update
	 * it. Must be called from the FX thread.
	 */
	public static void executeWaiting(Runnable toRun, String title, String subtitle) {
		ProgressTask task = new ProgressTask() {

			@Override
			protected Void call() throws Exception {
				toRun.run();
				return null;
			}
		};

		ProgressDialog dlg = new ProgressDialog(task);
		dlg.initOwner(mainStage);
		dlg.setTitle(title);
		dlg.getDialogPane().getButtonTypes().clear();
		dlg.setHeaderText(subtitle != null ? subtitle : title);
		dlg.setOnCloseRequest(Event::consume);
		// dlg.getDialogPane().setPadding(new Insets(12, 16, 16, 16));

		Object loopKey = new Object();
		task.setOnSucceeded(e -> Platform.exitNestedEventLoop(loopKey, null));
		task.setOnFailed(e -> {
			log.error("Exception in executeWaiting", task.getException());
			Platform.exitNestedEventLoop(loopKey, null);
		});
		task.setOnCancelled(e -> Platform.exitNestedEventLoop(loopKey, null));

		workingTask = task;
		Thread t = new Thread(task, "executeWaiting");
		t.setDaemon(true);
		t.start();

		dlg.show();
		Platform.enterNestedEventLoop(loopKey);
		// Dialog.hide() silently fails when no button types are registered; go
		// directly to the window to guarantee the dialog closes.
		if (dlg.isShowing())
			dlg.getDialogPane().getScene().getWindow().hide();
		workingTask = null;
	}

	/**
	 * Returns true if any part of rec falls outside all screen bounds.
	 */
	public static boolean isClipped(Rectangle2D rec) {
		double recArea = rec.getWidth() * rec.getHeight();
		if (recArea <= 0)
			return false;
		double visibleArea = 0;
		for (Screen screen : Screen.getScreens()) {
			Rectangle2D bounds = screen.getBounds();
			if (bounds.intersects(rec)) {
				double ix = Math.max(bounds.getMinX(), rec.getMinX());
				double iy = Math.max(bounds.getMinY(), rec.getMinY());
				double ix2 = Math.min(bounds.getMaxX(), rec.getMaxX());
				double iy2 = Math.min(bounds.getMaxY(), rec.getMaxY());
				visibleArea += (ix2 - ix) * (iy2 - iy);
			}
		}
		return visibleArea < recArea;
	}

	public static void openMainWindow() {
		mainStage.show();
	}

	public static void pushUI(Context ctx) {
		pushUI(ctx, null);
	}

	public static void pushUI(Node uiComponent) {
		pushUI(null, uiComponent);
	}

	/**
	 * At least one parameter should be specified, the other will be defaulted. When
	 * both are specified, the component is registered in the provided context as a
	 * context GUI component.
	 * 
	 * When there is no ctx specified, a context provided by the component will be
	 * used or a default one will be created.
	 * 
	 * When there is no component and there is no component registered in context,
	 * no component will be used (only other registered object will be used, e.g.
	 * actions, listeners...).
	 * 
	 * @param ctx
	 * @param uiComponent
	 */
	public static void pushUI(Context ctx, Node uiComponent) {
		Node component = null;
		Context lvlCtx = null;
		if (uiComponent == null) {
			if (ctx == null) {
				log.error("No CTX nor Component specified");
				return;
			}
			lvlCtx = ctx;
			ContextSearchResult c = ctx.getValue(CTX_MAIN_COMPONENT);
			if (c == null || !c.isValid())
				log.debug(
						"Pushed context doesn't contain a component to display (key:[{}]) and no component was specified",
						CTX_MAIN_COMPONENT);
			else
				component = (Node) c.getResult();
		} else {
			component = uiComponent;
			if (ctx == null) {
				if (uiComponent instanceof IContextProvider)
					lvlCtx = ((IContextProvider) uiComponent).getItemContext();
				else {
					lvlCtx = new Context("Default context for pushed UI component");
				}
			} else {
				lvlCtx = ctx;
			}

			lvlCtx.put(CTX_MAIN_COMPONENT, uiComponent);
		}

		lvlCtx.setLevelContext();

		try {
			uiContextHolder.push(lvlCtx);
		} catch (Exception e) {
			log.error("Failed to get UI stacked context from switched context PUSH operation.");
		}
		if (component instanceof IStackedComponent) {
			((IStackedComponent) component).afterPush();
		}
	}

	public static Context popUI() {
		try {
			ContextSearchResult c = uiContextHolder.peek().getValue(CTX_MAIN_COMPONENT);
			if (c.isValid()) {
				Node component = (Node) c.getResult();
				if (component instanceof IStackedComponent) {
					((IStackedComponent) component).beforePop();
				}
			}
			Context ctx = uiContextHolder.pop();
			return ctx;
		} catch (Exception e) {
			log.error("Failed to get UI stacked context from switched context for POP operation.");
		}
		return null;
	}

	@Override
	public String getModuleName() {
		return "BSAppUI";
	}

	@Override
	public String getVersionInfo() {
		return new VersionInfo().getDisplayVersion();
	}
}
