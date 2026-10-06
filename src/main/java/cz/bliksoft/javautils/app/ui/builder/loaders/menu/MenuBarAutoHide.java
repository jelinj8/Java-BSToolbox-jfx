package cz.bliksoft.javautils.app.ui.builder.loaders.menu;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
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
 * shows it on demand: Alt (also Alt+mnemonic), F10 (Ctrl+F10 on mac/linux) or
 * the mouse at the top edge of the scene. Enabled by the {@code MenuBar}
 * attribute {@code autoHideInFullScreen}.
 *
 * <p>
 * In full screen the bar is unmanaged (its siblings take its space) and painted
 * over them when shown. It keeps its size and position while hidden: menu
 * popups are anchored to the laid-out menu buttons and mnemonics only fire for
 * visible nodes, so the bar is shown already when Alt goes down. The key
 * handling mirrors {@code MenuBarSkin} (menu mode on a lone Alt tap, left by
 * Alt, Esc, a click elsewhere or losing focus).
 */
public final class MenuBarAutoHide {

	/** Mouse at most this far from the top of the scene shows the bar. */
	private static final double EDGE = 2;

	private final MenuBar bar;
	private final boolean ctrlF10;

	private Scene scene;
	private Window window;
	private Parent parent;

	private boolean active;
	private boolean altDown;
	private boolean altAlone;
	private boolean menuMode;
	private boolean hoverReveal;

	private final ChangeListener<Boolean> fullScreenListener = (obs, o, n) -> update();
	private final ChangeListener<Boolean> focusListener = (obs, o, n) -> {
		if (!n) {
			altDown = false;
			menuMode = false;
			hoverReveal = false;
			update();
		}
	};
	private final ChangeListener<Bounds> parentBoundsListener = (obs, o, n) -> relocate();
	private final ChangeListener<Boolean> menuShowingListener = (obs, o, n) -> {
		if (n)
			update();
		else
			// moving between menus hides one and shows the next - check afterwards
			Platform.runLater(() -> {
				if (!anyMenuShowing()) {
					menuMode = false;
					update();
				}
			});
	};

	private final EventHandler<KeyEvent> keyFilter = this::onKey;
	private final EventHandler<MouseEvent> mouseMovedFilter = this::onMouseMoved;
	private final EventHandler<MouseEvent> mouseClickedFilter = this::onMouseClicked;

	private MenuBarAutoHide(MenuBar bar) {
		this.bar = bar;
		// as MenuBarSkin: F10 on Windows, Ctrl+F10 elsewhere
		this.ctrlF10 = !System.getProperty("os.name", "").toLowerCase().contains("win");
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

	private final ChangeListener<Window> windowListener = (obs, o, n) -> setWindow(n);

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
		if (!active || e.isConsumed())
			return;
		if (e.getEventType() == KeyEvent.KEY_PRESSED) {
			if (e.getCode() == KeyCode.ALT) {
				altDown = true;
				// the skin leaves menu mode (also a menu opened by mouse)
				if (menuMode || anyMenuShowing()) {
					menuMode = false;
					altAlone = false;
				} else {
					altAlone = true;
				}
			} else {
				altAlone = false;
				if (e.getCode() == KeyCode.ESCAPE) {
					menuMode = false;
				} else if (e.getCode() == KeyCode.F10 && e.isControlDown() == ctrlF10) {
					menuMode = !menuMode;
				}
			}
			update();
		} else if (e.getEventType() == KeyEvent.KEY_RELEASED && e.getCode() == KeyCode.ALT) {
			altDown = false;
			if (altAlone)
				menuMode = true;
			altAlone = false;
			update();
		}
	}

	private void onMouseMoved(MouseEvent e) {
		if (!active)
			return;
		if (e.getSceneY() <= EDGE) {
			if (!hoverReveal) {
				hoverReveal = true;
				update();
			}
		} else if (hoverReveal && !anyMenuShowing() && e.getSceneY() > barBottom()) {
			hoverReveal = false;
			update();
		}
	}

	private void onMouseClicked(MouseEvent e) {
		if (!active || !menuMode)
			return;
		Bounds b = bar.localToScene(bar.getLayoutBounds());
		if (b == null || !b.contains(e.getSceneX(), e.getSceneY())) {
			menuMode = false;
			update();
		}
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

	/** Applies the active (full screen) or normal layout and the visibility. */
	private void update() {
		boolean fs = window instanceof Stage st && st.isFullScreen();
		if (fs != active) {
			active = fs;
			altDown = false;
			altAlone = false;
			menuMode = false;
			hoverReveal = false;
			bar.setManaged(!active);
			bar.setViewOrder(active ? -1 : 0);
		}
		relocate();
		bar.setVisible(!active || altDown || menuMode || hoverReveal || anyMenuShowing());
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
