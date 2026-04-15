package cz.bliksoft.javautils.app.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import cz.bliksoft.javautils.context.holders.SingleContextHolder;
import cz.bliksoft.javautils.context.holders.StackedContextHolder;
import cz.bliksoft.javautils.fx.VersionInfo;
import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import cz.bliksoft.javautils.fx.tools.Styling;
import cz.bliksoft.javautils.modules.ModuleBase;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class BSAppUI extends ModuleBase {
	static Logger log = null;

	public static final String CTX_MAIN_COMPONENT = "MainDesktopComponent"; //$NON-NLS-1$

	public static final String FS_UI_ROOT = "/AppUI"; //$NON-NLS-1$

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
	 * inicializace aplikačního frameworku s UI
	 * 
	 * @throws Exception
	 */
	public static void init(Application app, Stage stage) {
		setStage(stage);
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

//	private static WorkingWheelDialog workingWheelDialog = null;
//
//	/**
//	 * zobrazí modální otáčecí kolečko s poznámkou
//	 * 
//	 * @param comment
//	 */
//	public static void showWorkingWheel(final String comment) {
//		if (workingWheelDialog == null) {
//			workingWheelDialog = new WorkingWheelDialog();
//		}
//		SwingUtilities.invokeLater(() -> {
//			workingWheelDialog.setLabel(comment);
//			workingWheelDialog.setLocationRelativeTo(BSFrameworkUI.getMainForm());
//			workingWheelDialog.setModal(true);
//			workingWheelDialog.setVisible(true);
//		});
//	}
//
//	public static void setWorkingWheelTitle(final String comment) {
//		SwingUtilities.invokeLater(() -> {
//			workingWheelDialog.setLabel(comment);
//			workingWheelDialog.setLocationRelativeTo(BSFrameworkUI.getMainForm());
//		});
//	}
//
//	public static void setWorkingWheelSubtitle(final String comment) {
//		SwingUtilities.invokeLater(() -> {
//			workingWheelDialog.setSubLabel(comment);
//			workingWheelDialog.setLocationRelativeTo(BSFrameworkUI.getMainForm());
//		});
//	}
//
//	public static void setWorkingWheelProgress(final int percents) {
//		SwingUtilities.invokeLater(() -> workingWheelDialog.setProgress(percents));
//	}
//
//	/**
//	 * zobrazí modální otáčecí kolečko s poznámkou
//	 * 
//	 * @param comment
//	 */
//	public static void showWorkingWheelWaitForSwingWorker(SwingWorker<?, ?> sw, final String comment) {
//		if (workingWheelDialog == null) {
//			workingWheelDialog = new WorkingWheelDialog();
//		}
//		sw.addPropertyChangeListener(workingWheelDialog);
//		sw.execute();
//		workingWheelDialog.setLabel(comment);
//		workingWheelDialog.setLocationRelativeTo(BSFrameworkUI.getMainForm());
//		workingWheelDialog.setModal(true);
//		workingWheelDialog.setVisible(true);
//	}
//
//	/**
//	 * zobrazí nemodální otáčecí kolečko s poznámkou
//	 * 
//	 * @param comment
//	 */
//	public static void showWorkingWheelDontWaitForSwingWorker(SwingWorker<?, ?> sw, final String comment) {
//		if (workingWheelDialog == null) {
//			workingWheelDialog = new WorkingWheelDialog();
//		}
//		sw.addPropertyChangeListener(workingWheelDialog);
//		sw.execute();
//		workingWheelDialog.setLabel(comment);
//		workingWheelDialog.setLocationRelativeTo(BSFrameworkUI.getMainForm());
//		workingWheelDialog.setModal(false);
//		workingWheelDialog.setVisible(true);
//	}
//
//	/**
//	 * skryje čekací dialog
//	 */
//	public static void hideWorkingWheel() {
//		SwingUtilities.invokeLater(() -> workingWheelDialog.setVisible(false));
//	}
//
//	/**
//	 * zobrazí otáčecí kolečko, spustí akci na pozadí a po jejím dokončení kolečko
//	 * skryje
//	 * 
//	 * @param toRun
//	 * @param comment
//	 */
//	public static void executeWaiting(final Runnable toRun, String comment) {
//		showWorkingWheel(comment);
//		final SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {
//
//			@Override
//			protected Object doInBackground() throws Exception {
//				toRun.run();
//				return null;
//			}
//
//			@Override
//			protected void done() {
//				try {
//					this.get();
//				} catch (InterruptedException | ExecutionException e) {
//					log.error("Exception in \"executeWaiting\"", e);
//				} finally {
//					super.done();
//					hideWorkingWheel();
//				}
//			}
//		};
//		SwingUtilities.invokeLater(() -> sw.execute());
//		// sw.execute();
//	}
//
//	public static boolean isClipped(Rectangle rec) {
//
//		boolean isClipped = false;
//		int recArea = rec.width * rec.height;
//		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//		GraphicsDevice sd[] = ge.getScreenDevices();
//		Rectangle bounds;
//		int boundsArea = 0;
//
//		for (GraphicsDevice gd : sd) {
//			bounds = gd.getDefaultConfiguration().getBounds();
//			if (bounds.intersects(rec)) {
//				bounds = bounds.intersection(rec);
//				boundsArea = boundsArea + (bounds.width * bounds.height);
//			}
//		}
//		if (boundsArea != recArea) {
//			isClipped = true;
//		}
//		return isClipped;
//	}

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
