package cz.bliksoft.javautils.app.ui.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.IStackedComponent;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IClose;
import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ContextChangedEvent;
import cz.bliksoft.javautils.context.IContextProvider;
import cz.bliksoft.javautils.context.holders.MapContextHolder;
import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * A {@link TabPane} backed by a {@link MapContextHolder}: each tab owns its
 * own {@link Context}, and switching tabs changes the active context in the
 * hierarchy so that context-aware actions automatically track the selected tab.
 *
 * <p>Usage mirrors {@code pushUI}/{@code popUI}:
 * <pre>
 *   ContextTabbedPane pane = new ContextTabbedPane();
 *   pane.addContextTab("Editor 1", editor1Node);
 *   pane.addContextTab("Editor 2", editor2Node);
 *   BSAppUI.pushUI(pane);
 * </pre>
 *
 * <p>The pane registers itself as {@link IClose} in the MapContextHolder so
 * {@code CloseAction} closes the active tab rather than propagating to any
 * parent close handler.
 *
 * <p>A tab's title and graphic can be driven reactively by the tab's context:
 * <ul>
 *   <li>{@link #CTX_TAB_TITLE} — {@code String} (static) or
 *       {@code StringProperty} (reactive); falls back to the default title
 *       passed to {@code addContextTab}</li>
 *   <li>{@link #CTX_TAB_GRAPHIC} — {@code Node}, {@code ObjectProperty<Node>},
 *       {@code String} (icon spec), or {@code StringProperty} (reactive spec)</li>
 * </ul>
 */
public class ContextTabbedPane extends TabPane implements IContextProvider, IStackedComponent {

	private static final Logger log = LogManager.getLogger();

	/** Context key for the tab header title. Value: {@code String} or {@code StringProperty}. */
	public static final String CTX_TAB_TITLE   = "TabTitle";

	/** Context key for the tab header graphic. Value: {@code Node}, {@code ObjectProperty<Node>},
	 *  {@code String} (icon spec), or {@code StringProperty} (reactive icon spec). */
	public static final String CTX_TAB_GRAPHIC = "TabGraphic";

	/** Context key for the active tab's content node — scoped to this pane's context,
	 *  intentionally separate from {@link BSAppUI#CTX_MAIN_COMPONENT} so that tab
	 *  selection does not propagate up and replace the pane in BSAppUI's main area. */
	public static final String CTX_TAB_CONTENT = "TabContent";

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
		contextHolder.put(IClose.class, tabClose);

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
		return contextHolder;
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private void setupTabListeners(Tab tab, Context tabCtx, String defaultTitle) {

		// Title: String (static) or StringProperty (reactive)
		tabCtx.addContextListener(new AbstractContextListener<Object>(CTX_TAB_TITLE, "tab title: " + defaultTitle) {
			private StringProperty boundProp = null;

			@Override
			public void fired(ContextChangedEvent<Object> event) {
				if (boundProp != null) {
					tab.textProperty().unbind();
					boundProp = null;
				}
				Object value = event.getNewValue();
				if (value instanceof StringProperty sp) {
					tab.textProperty().bind(sp);
					boundProp = sp;
				} else if (value instanceof String s) {
					tab.setText(s);
				} else {
					tab.setText(defaultTitle);
				}
			}
		}, true);

		// Graphic: Node, ObjectProperty<Node>, String spec, or StringProperty spec
		tabCtx.addContextListener(new AbstractContextListener<Object>(CTX_TAB_GRAPHIC, "tab graphic: " + defaultTitle) {
			private boolean graphicBound           = false;
			private ChangeListener<String> specListener = null;
			private StringProperty         specProperty = null;

			@Override
			public void fired(ContextChangedEvent<Object> event) {
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
					tab.setGraphic(null);
				} else if (value instanceof Node node) {
					tab.setGraphic(node);
				} else if (value instanceof ObjectProperty) {
					tab.graphicProperty().bind((ObjectProperty<Node>) value);
					graphicBound = true;
				} else if (value instanceof StringProperty sp) {
					tab.setGraphic(ImageUtils.getIconView(sp.get()));
					specProperty = sp;
					specListener = (obs, o, n) -> tab.setGraphic(ImageUtils.getIconView(n));
					sp.addListener(specListener);
				} else if (value instanceof String s) {
					tab.setGraphic(ImageUtils.getIconView(s));
				} else {
					log.warn("Unsupported {} value type: {}", CTX_TAB_GRAPHIC, value.getClass().getSimpleName());
				}
			}
		}, true);
	}
}
