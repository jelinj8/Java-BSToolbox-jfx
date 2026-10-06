package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Hides a {@link MenuBar} while its window is a full-screen {@link Stage} and
 * shows it on demand: Alt (also Alt+mnemonic), F10 or the mouse at the top edge
 * of the scene. Enabled by the {@code MenuBar} attribute
 * {@code autoHideInFullScreen}.
 *
 * <p>
 * In full screen the bar is unmanaged (its siblings take its space) and painted
 * over them when shown. It keeps its size and position while hidden: menu
 * popups are anchored to the laid-out menu buttons and mnemonics only fire for
 * visible nodes, so the bar is shown already when Alt goes down.
 *
 * <p>
 * The bar stays shown while {@code MenuBarSkin} is in its menu mode. That state
 * is private to the skin, but the skin marks the selected menu by hovering its
 * button (also when a menu is highlighted without being open, e.g. an empty
 * one) - so it is read from the buttons' hover, re-checked after every key,
 * click, focus and menu change once the skin has processed it.
 */
public final class MenuBarAutoHide {

	/** Mouse at most this far from the top of the scene shows the bar. */
	private static final double EDGE = 2;

	private final MenuBar bar;

	private Scene scene;
	private Window window;
	private Parent parent;

	private boolean active;
	private boolean altDown;
	private boolean hoverReveal;
	private boolean updateQueued;

	private final ChangeListener<Boolean> fullScreenListener = (obs, o, n) -> update();
	private final ChangeListener<Boolean> focusListener = (obs, o, n) -> {
		if (!n) {
			altDown = false;
			hoverReveal = false;
		}
		updateLater();
	};
	private final ChangeListener<Bounds> parentBoundsListener = (obs, o, n) -> relocate();
	private final ChangeListener<Boolean> menuShowingListener = (obs, o, n) -> updateLater();
	private final ChangeListener<Window> windowListener = (obs, o, n) -> setWindow(n);

	private final EventHandler<KeyEvent> keyFilter = this::onKey;
	private final EventHandler<MouseEvent> mouseMovedFilter = this::onMouseMoved;
	private final EventHandler<MouseEvent> mouseClickedFilter = e -> updateLater();

	private MenuBarAutoHide(MenuBar bar) {
		this.bar = bar;
	}

	/** Installs the full-screen auto-hide on {@code bar}. */
	public static void install(MenuBar bar) {
		MenuBarAutoHide h = new MenuBarAutoHide(bar);
		bar.sceneProperty().addListener((obs, o, n) -> h.setScene(n));
		bar.parentProperty().addListener((obs, o, n) -> h.setParent(n));
		bar.getMenus().addListener((ListChangeListener<Menu>) c -> {
			while (c.next()) {
				for (Menu m : c.getRemoved())
					m.showingProperty().removeListener(h.menuShowingListener);
				for (Menu m : c.getAddedSubList())
					m.showingProperty().addListener(h.menuShowingListener);
			}
		});
		for (Menu m : bar.getMenus())
			m.showingProperty().addListener(h.menuShowingListener);
		h.setParent(bar.getParent());
		h.setScene(bar.getScene());
	}

	private void setScene(Scene s) {
		if (scene != null) {
			scene.removeEventFilter(KeyEvent.ANY, keyFilter);
			scene.removeEventFilter(MouseEvent.MOUSE_MOVED, mouseMovedFilter);
			scene.removeEventFilter(MouseEvent.MOUSE_CLICKED, mouseClickedFilter);
			scene.windowProperty().removeListener(windowListener);
		}
		scene = s;
		if (scene != null) {
			scene.addEventFilter(KeyEvent.ANY, keyFilter);
			scene.addEventFilter(MouseEvent.MOUSE_MOVED, mouseMovedFilter);
			scene.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseClickedFilter);
			scene.windowProperty().addListener(windowListener);
		}
		setWindow(scene != null ? scene.getWindow() : null);
	}

	private void setWindow(Window w) {
		if (window != null) {
			window.focusedProperty().removeListener(focusListener);
			if (window instanceof Stage st)
				st.fullScreenProperty().removeListener(fullScreenListener);
		}
		window = w;
		if (window != null) {
			window.focusedProperty().addListener(focusListener);
			if (window instanceof Stage st)
				st.fullScreenProperty().addListener(fullScreenListener);
		}
		update();
	}

	private void setParent(Parent p) {
		if (parent != null)
			parent.layoutBoundsProperty().removeListener(parentBoundsListener);
		parent = p;
		if (parent != null)
			parent.layoutBoundsProperty().addListener(parentBoundsListener);
		relocate();
	}

	private void onKey(KeyEvent e) {
		if (!active)
			return;
		if (e.getCode() == KeyCode.ALT) {
			if (e.getEventType() == KeyEvent.KEY_PRESSED) {
				// shown right away - mnemonics (Alt+key) fire only for visible nodes
				altDown = true;
				update();
			} else if (e.getEventType() == KeyEvent.KEY_RELEASED) {
				altDown = false;
			}
		}
		// the skin's own key handling (menu mode on/off, arrows, Esc, F10) runs
		// in this same dispatch - check its result afterwards
		updateLater();
	}

	private void onMouseMoved(MouseEvent e) {
		if (!active)
			return;
		if (e.getSceneY() <= EDGE) {
			if (!hoverReveal) {
				hoverReveal = true;
				update();
			}
		} else if (hoverReveal && e.getSceneY() > barBottom()) {
			hoverReveal = false;
			update();
		}
		// a menu button's (mouse) hover may clear after this event
		if (bar.isVisible())
			updateLater();
	}

	private double barBottom() {
		Bounds b = bar.localToScene(bar.getLayoutBounds());
		return b != null ? b.getMaxY() : EDGE;
	}

	private boolean anyMenuShowing() {
		for (Menu m : bar.getMenus())
			if (m.isShowing())
				return true;
		return false;
	}

	/**
	 * Whether the skin is in its menu mode: one of its menu buttons (children of
	 * its {@code .container}) is hovered.
	 */
	private boolean skinMenuMode() {
		for (Node n : bar.getChildrenUnmodifiable()) {
			if (n instanceof Parent container && container.getStyleClass().contains("container")) {
				for (Node b : container.getChildrenUnmodifiable())
					if (b.isHover())
						return true;
			}
		}
		return false;
	}

	private void updateLater() {
		if (!active || updateQueued)
			return;
		updateQueued = true;
		Platform.runLater(() -> {
			updateQueued = false;
			update();
		});
	}

	/**
	 * The menus are in the macOS system menu bar - the in-window bar is empty and
	 * the system shows its own bar in full screen.
	 */
	private boolean systemMenuBar() {
		return bar.isUseSystemMenuBar() && MAC;
	}

	private static final boolean MAC = System.getProperty("os.name", "").toLowerCase().startsWith("mac");

	/** Applies the active (full screen) or normal layout and the visibility. */
	private void update() {
		boolean fs = window instanceof Stage st && st.isFullScreen() && !systemMenuBar();
		if (fs != active) {
			active = fs;
			altDown = false;
			hoverReveal = false;
			bar.setManaged(!active);
			bar.setViewOrder(active ? -1 : 0);
		}
		relocate();
		bar.setVisible(!active || altDown || hoverReveal || anyMenuShowing() || skinMenuMode());
	}

	/** Sizes the unmanaged bar over the top of its parent's content area. */
	private void relocate() {
		if (!active || parent == null)
			return;
		Bounds pb = parent.getLayoutBounds();
		Insets in = parent instanceof Region r ? r.getInsets() : Insets.EMPTY;
		double w = Math.max(0, pb.getWidth() - in.getLeft() - in.getRight());
		bar.resizeRelocate(in.getLeft(), in.getTop(), w, bar.prefHeight(w));
	}
}
