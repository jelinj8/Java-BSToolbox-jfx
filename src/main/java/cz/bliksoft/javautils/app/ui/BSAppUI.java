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
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
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

	private static Stage mainStage = null;

//	public SimpleObjectProperty<Node> rootNode = new SimpleObjectProperty<Node>();
	public static BorderPane mainPane = null;

	/**
	 * Sets the primary stage. Called once by the application entry point before
	 * {@link #init}.
	 */
	public static void setStage(Stage stage) {
		mainStage = stage;
	}

	/** Returns the primary application stage. */
	public static Stage getStage() {
		return mainStage;
	}

	@Override
	public int getModuleLoadingOrder() {
		return 10000;
	}

	/**
	 * Initialises the UI application framework. Sets the primary stage, registers
	 * BSAppUI as a module, and delegates to {@link BSApp#init}. Must be called once
	 * from {@code Application.start()} before any other BSAppUI call.
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

	/**
	 * Fires a {@link MessageEvent} with the given text into the current UI context
	 * (status bar).
	 */
	public static void showStatusMessage(String text) {
		MessageEvent e = new MessageEvent(text);
		Context.getCurrentContext().fireEvent(e);
	}

	/**
	 * Fires a {@link MessageEvent} with an icon into the current UI context (status
	 * bar).
	 *
	 * @param icon icon resource name passed to {@link ImageUtils#getIconView}
	 */
	public static void showStatusMessage(String text, String icon) {
		MessageEvent e = new MessageEvent(text, ImageUtils.getIconView(icon));
		Context.getCurrentContext().fireEvent(e);
	}

	/**
	 * Fires a {@link MessageEvent} with an icon and an additional CSS style class.
	 *
	 * @param icon    icon resource name, or {@code null} for no icon
	 * @param fxClass CSS style class applied to the status bar entry
	 */
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

	private static volatile Task<?> workingTask = null;
	private static volatile Dialog<?> workingDlg = null;

	/**
	 * Shows a modal indeterminate progress dialog and returns immediately. Call
	 * {@link #hideWorkingWheel()} to close it. While the dialog is open,
	 * {@link #setWorkingWheelTitle}, {@link #setWorkingWheelMessage},
	 * {@link #setWorkingWheelDetail}, and {@link #setWorkingWheelProgress} can be
	 * called from any thread to update it.
	 *
	 * @param title    window title bar text, or {@code null} to leave it empty
	 * @param subtitle initial dialog header text, live-updatable via
	 *                 {@link #setWorkingWheelMessage}
	 */
	public static void showWorkingWheel(String title, String subtitle) {
		WorkingTask task = new WorkingTask(subtitle);
		workingTask = task;
		Thread t = new Thread(task, "working-wheel");
		t.setDaemon(true);
		t.start();
		Platform.runLater(() -> {
			ProgressDialog dlg = new ProgressDialog(task);
			workingDlg = dlg;
			dlg.initOwner(mainStage);
			dlg.setTitle(title);
			dlg.getDialogPane().headerTextProperty().bind(task.titleProperty());
			dlg.show();
		});
	}

	/**
	 * Convenience overload; uses {@code subtitle} as the dialog header with no
	 * window title. See {@link #showWorkingWheel(String, String)}.
	 */
	public static void showWorkingWheel(String subtitle) {
		showWorkingWheel(null, subtitle);
	}

	/**
	 * Updates the window title bar of the active working-wheel dialog. Safe to call
	 * from any thread.
	 */
	public static void setWorkingWheelTitle(String title) {
		runOnFx(() -> {
			if (workingDlg != null)
				workingDlg.setTitle(title);
		});
	}

	/**
	 * Updates the header text of the active working-wheel dialog. Safe to call from
	 * any thread.
	 */
	public static void setWorkingWheelMessage(String message) {
		runOnFx(() -> {
			if (workingTask instanceof ProgressTask t)
				t.updateTitle(message);
		});
	}

	/**
	 * Updates the detail text (below the header, above the progress bar) of the
	 * active working-wheel dialog. Safe to call from any thread.
	 */
	public static void setWorkingWheelDetail(String detail) {
		runOnFx(() -> {
			if (workingTask instanceof ProgressTask t)
				t.updateMessage(detail);
		});
	}

	/**
	 * Updates the progress bar of the active working-wheel dialog. Pass
	 * {@code total=0} to revert to an indeterminate (spinning) bar. Safe to call
	 * from any thread.
	 */
	public static void setWorkingWheelProgress(int done, int total) {
		runOnFx(() -> {
			if (workingTask instanceof ProgressTask t)
				t.updateProgress(total == 0 ? -1 : done, total == 0 ? 1 : total);
		});
	}

	private static void runOnFx(Runnable r) {
		if (Platform.isFxApplicationThread())
			r.run();
		else
			Platform.runLater(r);
	}

	/**
	 * Closes the dialog opened by {@link #showWorkingWheel}. No-op if no dialog is
	 * active.
	 */
	public static void hideWorkingWheel() {
		Task<?> t = workingTask;
		workingTask = null;
		workingDlg = null;
		if (t instanceof WorkingTask wt)
			wt.complete();
	}

	/**
	 * Runs {@code toRun} on a background thread behind a modal blocking progress
	 * dialog, then returns once the task finishes. {@code comment} is used as the
	 * dialog header (subtitle); the window title is left empty. Must be called from
	 * the FX thread.
	 *
	 * @see #executeWaiting(Runnable, String, String, String)
	 */
	public static void executeWaiting(Runnable toRun, String comment) {
		executeWaiting(toRun, null, comment, null);
	}

	/**
	 * Runs {@code toRun} on a background thread behind a modal blocking progress
	 * dialog, then returns once the task finishes. Must be called from the FX
	 * thread.
	 * <p>
	 * The dialog has three independently settable areas:
	 * <ul>
	 * <li><b>title</b> — window title bar; set once from this parameter.</li>
	 * <li><b>subtitle</b> — header text inside the dialog; set initially from this
	 * parameter and live-updatable via {@link #setWorkingWheelTitle}.</li>
	 * <li><b>message</b> — text above the progress bar; set initially from this
	 * parameter and live-updatable via {@link #setWorkingWheelMessage}.</li>
	 * </ul>
	 * Progress starts indeterminate and can be updated via
	 * {@link #setWorkingWheelProgress}. All three setters are safe to call from
	 * {@code toRun} (i.e. from the background thread). Any parameter may be
	 * {@code null} to leave that area empty initially.
	 *
	 * @param title    window title bar text
	 * @param subtitle initial dialog header text
	 * @param message  initial message text above the progress bar
	 */
	public static void executeWaiting(Runnable toRun, String title, String subtitle, String message) {
		ProgressTask task = new ProgressTask() {
			{
				if (subtitle != null)
					updateTitle(subtitle);
				if (message != null)
					updateMessage(message);
			}

			@Override
			protected Void call() throws Exception {
				toRun.run();
				return null;
			}
		};
		executeWaiting(task, title);
	}

	/**
	 * Runs a pre-built {@link Task} on a background thread behind a modal blocking
	 * progress dialog, then returns the task's result once it finishes. Must be
	 * called from the FX thread.
	 * <p>
	 * The task drives the dialog directly:
	 * <ul>
	 * <li>{@code task.titleProperty()} — dialog header text; call
	 * {@code updateTitle} to change it live.</li>
	 * <li>{@code task.messageProperty()} — message area above the progress bar;
	 * call {@code updateMessage} from within the task.</li>
	 * <li>{@code task.progressProperty()} — progress bar; call
	 * {@code updateProgress} from within the task (negative value =
	 * indeterminate).</li>
	 * </ul>
	 * <p>
	 * The dialog header is bound to {@code task.titleProperty()}, so
	 * {@code updateTitle()} is reflected immediately whether called from the task
	 * constructor or from within {@link Task#call()}.
	 *
	 * @param windowTitle window title bar text
	 * @return the value returned by {@link Task#call()}, or {@code null} if the
	 *         task failed or was cancelled
	 */
	public static <T> T executeWaiting(Task<T> task, String windowTitle) {
		ProgressDialog dlg = new ProgressDialog(task);
		dlg.initOwner(mainStage);
		dlg.setTitle(windowTitle);
		dlg.getDialogPane().headerTextProperty().bind(task.titleProperty());
		dlg.getDialogPane().getButtonTypes().clear();
		dlg.setOnCloseRequest(Event::consume);

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
		return task.getValue();
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

	/**
	 * Makes the primary stage visible. Used to restore a minimised or hidden
	 * window.
	 */
	public static void openMainWindow() {
		mainStage.show();
	}

	/**
	 * Pushes a context onto the UI stack. Equivalent to
	 * {@link #pushUI(Context, Node) pushUI(ctx, null)}.
	 */
	public static void pushUI(Context ctx) {
		pushUI(ctx, null);
	}

	/**
	 * Pushes a UI component onto the UI stack. Equivalent to
	 * {@link #pushUI(Context, Node) pushUI(null, component)}.
	 */
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

	/**
	 * Pops the top context from the UI stack, calling
	 * {@link IStackedComponent#beforePop()} on its main component if applicable.
	 * Returns the popped context, or {@code null} if the operation fails.
	 */
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
