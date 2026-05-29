package cz.bliksoft.javautils.app.ui.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.interfaces.IStackedComponent;
import cz.bliksoft.javautils.app.ui.interfaces.ITabTitleProvider;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IClose;
import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ContextChangedEvent;
import cz.bliksoft.javautils.context.IContextProvider;
import cz.bliksoft.javautils.context.holders.MapContextHolder;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * A {@link TabPane} backed by a {@link MapContextHolder}: each tab owns its own
 * {@link Context}, and switching tabs changes the active context in the
 * hierarchy so that context-aware actions automatically track the selected tab.
 *
 * <p>
 * Usage mirrors {@code pushUI}/{@code popUI}:
 *
 * <pre>
 * ContextTabbedPane pane = new ContextTabbedPane();
 * pane.addContextTab("Editor 1", editor1Node);
 * pane.addContextTab("Editor 2", editor2Node);
 * BSAppUI.pushUI(pane);
 * </pre>
 *
 * <p>
 * The pane registers itself as {@link IClose} in the MapContextHolder so
 * {@code CloseAction} closes the active tab rather than propagating to any
 * parent close handler.
 *
 * <p>
 * A tab's title and graphic are resolved from three sources in descending
 * priority:
 * <ol>
 * <li><b>Explicit context keys</b> — {@link #CTX_TAB_TITLE} and
 * {@link #CTX_TAB_GRAPHIC} placed in the tab's context</li>
 * <li><b>{@link ITabTitleProvider}</b> — registered in the tab's context under
 * {@code ITabTitleProvider.class}; automatically wired when the content node
 * implements the interface</li>
 * <li><b>Defaults</b> — the {@code defaultTitle} string passed to
 * {@code addContextTab}, {@code null} graphic</li>
 * </ol>
 *
 * <p>
 * Supported value types per key:
 * <ul>
 * <li>{@link #CTX_TAB_TITLE} — {@code String} (static) or
 * {@code StringProperty} (reactive)</li>
 * <li>{@link #CTX_TAB_GRAPHIC} — {@code Node}, {@code ObjectProperty<Node>},
 * {@code String} (icon spec), or {@code StringProperty} (reactive spec)</li>
 * </ul>
 */
public class ContextTabbedPane extends TabPane implements IContextProvider, IStackedComponent {

	private static final Logger log = LogManager.getLogger();

	/**
	 * Context key for the tab header title. Value: {@code String} or
	 * {@code StringProperty}.
	 */
	public static final String CTX_TAB_TITLE = "TabTitle";

	/**
	 * Context key for the tab header graphic. Value: {@code Node},
	 * {@code ObjectProperty<Node>}, {@code String} (icon spec), or
	 * {@code StringProperty} (reactive icon spec).
	 */
	public static final String CTX_TAB_GRAPHIC = "TabGraphic";

	/**
	 * Context key for the active tab's content node — scoped to this pane's
	 * context, intentionally separate from {@link BSAppUI#CTX_MAIN_COMPONENT} so
	 * that tab selection does not propagate up and replace the pane in BSAppUI's
	 * main area.
	 */
	public static final String CTX_TAB_CONTENT = "TabContent";

	private final Context paneContext = new Context("ContextTabbedPane");
	private final MapContextHolder<Tab, Context> contextHolder = new MapContextHolder<>("ContextTabbedPane");

	private final BooleanProperty closeEnabled = new SimpleBooleanProperty(false);

	private final IClose tabClose = new IClose() {
		@Override
		public void close() {
			Tab tab = getSelectionModel().getSelectedItem();
			if (tab != null)
				getTabs().remove(tab);
		}

		@Override
		public BooleanProperty getCloseEnabled() {
			return closeEnabled;
		}
	};

	public ContextTabbedPane() {
		paneContext.addContext(contextHolder);
		paneContext.put(IClose.class, tabClose);

		getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
			if (now != null)
				contextHolder.select(now);
			else
				contextHolder.deselect();
			closeEnabled.set(now != null);
		});

		getTabs().addListener((ListChangeListener<Tab>) c -> {
			while (c.next())
				c.getRemoved().forEach(contextHolder::removeKey);
		});
	}

	/**
	 * Adds a tab using a context provided by the component (if it implements
	 * {@link IContextProvider}) or a fresh {@link Context}.
	 */
	public Tab addContextTab(String defaultTitle, Node content) {
		return addContextTab(defaultTitle, null, content);
	}

	/**
	 * Adds a tab with an explicit context. When {@code ctx} is {@code null} the
	 * context is resolved from the component (if {@link IContextProvider}) or
	 * created fresh — the same logic as {@code pushUI(Node)}.
	 */
	public Tab addContextTab(String defaultTitle, Context ctx, Node content) {
		Tab tab = new Tab(defaultTitle, content);

		Context tabCtx;
		if (ctx != null) {
			tabCtx = ctx;
		} else if (content instanceof IContextProvider cp) {
			tabCtx = cp.getItemContext();
		} else {
			tabCtx = new Context("Tab: " + defaultTitle);
		}

		tabCtx.put(CTX_TAB_CONTENT, content);
		if (content instanceof ITabTitleProvider provider) {
			tabCtx.put(ITabTitleProvider.class, provider);
		}
		contextHolder.put(tab, tabCtx);
		setupTabListeners(tab, tabCtx, defaultTitle);
		getTabs().add(tab);
		return tab;
	}

	/** Removes a tab and its associated context. */
	public void removeContextTab(Tab tab) {
		getTabs().remove(tab);
	}

	/** Returns the {@link MapContextHolder} backing this pane. */
	public MapContextHolder<Tab, Context> getContextHolder() {
		return contextHolder;
	}

	@Override
	public Context getItemContext() {
		return paneContext;
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private void setupTabListeners(Tab tab, Context tabCtx, String defaultTitle) {

		// --- Shared state (final arrays for lambda capture) ---

		final ITabTitleProvider[] provider = { null };

		// Whether an explicit CTX_TAB_TITLE / CTX_TAB_GRAPHIC value is active
		final boolean[] hasExplicitTitle = { false };
		final boolean[] hasExplicitGraphic = { false };

		// Provider-title binding tracking
		final boolean[] providerTitleBound = { false };

		// Provider-graphic listener tracking
		final ChangeListener<Node>[] providerGraphicListener = new ChangeListener[1];
		final ObservableValue<Node>[] providerGraphicObs = new ObservableValue[1];

		// --- Provider-title helpers ---

		Runnable applyProviderTitle = () -> {
			if (providerTitleBound[0]) {
				tab.textProperty().unbind();
				providerTitleBound[0] = false;
			}
			if (provider[0] != null) {
				var p = provider[0].tabTitleProperty();
				if (p != null) {
					tab.textProperty().bind(p);
					providerTitleBound[0] = true;
					return;
				}
			}
			tab.setText(defaultTitle);
		};

		// --- Provider-graphic helpers ---

		Runnable cleanupProviderGraphic = () -> {
			if (providerGraphicListener[0] != null) {
				providerGraphicObs[0].removeListener(providerGraphicListener[0]);
				providerGraphicListener[0] = null;
				providerGraphicObs[0] = null;
			}
		};

		Runnable applyProviderGraphic = () -> {
			cleanupProviderGraphic.run();
			if (provider[0] != null) {
				ObservableValue<Node> g = provider[0].tabGraphicProperty();
				if (g != null) {
					ChangeListener<Node> l = (obs, o, n) -> tab.setGraphic(n);
					g.addListener(l);
					tab.setGraphic(g.getValue());
					providerGraphicListener[0] = l;
					providerGraphicObs[0] = g;
					return;
				}
			}
			tab.setGraphic(null);
		};

		// --- Listener 1: ITabTitleProvider ---

		tabCtx.addContextListener(new AbstractContextListener<ITabTitleProvider>(ITabTitleProvider.class,
				"tab provider: " + defaultTitle) {
			@Override
			public void fired(ContextChangedEvent<ITabTitleProvider> event) {
				provider[0] = event.getNewValue();
				if (!hasExplicitTitle[0])
					applyProviderTitle.run();
				if (!hasExplicitGraphic[0])
					applyProviderGraphic.run();
			}
		}, true);

		// --- Listener 2: CTX_TAB_TITLE (String or StringProperty) ---

		tabCtx.addContextListener(new AbstractContextListener<Object>(CTX_TAB_TITLE, "tab title: " + defaultTitle) {
			private StringProperty boundProp = null;

			@Override
			public void fired(ContextChangedEvent<Object> event) {
				// Unbind any previous explicit binding
				if (boundProp != null) {
					tab.textProperty().unbind();
					boundProp = null;
				}
				// Also release any provider-title binding we may have applied
				if (providerTitleBound[0]) {
					tab.textProperty().unbind();
					providerTitleBound[0] = false;
				}

				Object value = event.getNewValue();
				if (value instanceof StringProperty sp) {
					hasExplicitTitle[0] = true;
					tab.textProperty().bind(sp);
					boundProp = sp;
				} else if (value instanceof String s) {
					hasExplicitTitle[0] = true;
					tab.setText(s);
				} else {
					// No explicit title — fall back to provider or defaultTitle
					hasExplicitTitle[0] = false;
					applyProviderTitle.run();
				}
			}
		}, true);

		// --- Listener 3: CTX_TAB_GRAPHIC (Node, ObjectProperty<Node>, String,
		// StringProperty) ---

		tabCtx.addContextListener(new AbstractContextListener<Object>(CTX_TAB_GRAPHIC, "tab graphic: " + defaultTitle) {
			private boolean graphicBound = false;
			private ChangeListener<String> specListener = null;
			private StringProperty specProperty = null;

			@Override
			public void fired(ContextChangedEvent<Object> event) {
				// Cleanup previous explicit graphic state
				if (graphicBound) {
					tab.graphicProperty().unbind();
					graphicBound = false;
				}
				if (specListener != null) {
					specProperty.removeListener(specListener);
					specListener = null;
					specProperty = null;
				}

				Object value = event.getNewValue();
				if (value == null) {
					hasExplicitGraphic[0] = false;
					applyProviderGraphic.run();
				} else if (value instanceof Node node) {
					hasExplicitGraphic[0] = true;
					cleanupProviderGraphic.run();
					tab.setGraphic(node);
				} else if (value instanceof ObjectProperty) {
					hasExplicitGraphic[0] = true;
					cleanupProviderGraphic.run();
					tab.graphicProperty().bind((ObjectProperty<Node>) value);
					graphicBound = true;
				} else if (value instanceof StringProperty sp) {
					hasExplicitGraphic[0] = true;
					cleanupProviderGraphic.run();
					tab.setGraphic(ImageUtils.getIconView(sp.get()));
					specProperty = sp;
					specListener = (obs, o, n) -> tab.setGraphic(ImageUtils.getIconView(n));
					sp.addListener(specListener);
				} else if (value instanceof String s) {
					hasExplicitGraphic[0] = true;
					cleanupProviderGraphic.run();
					tab.setGraphic(ImageUtils.getIconView(s));
				} else {
					hasExplicitGraphic[0] = false;
					log.warn("Unsupported {} value type: {}", CTX_TAB_GRAPHIC, value.getClass().getSimpleName());
					applyProviderGraphic.run();
				}
			}
		}, true);
	}
}
