package cz.bliksoft.javautils.app.ui.actions;

import cz.bliksoft.javautils.app.ui.interfaces.ICSSClassesProvider;
import cz.bliksoft.javautils.app.ui.interfaces.IGraphicsProvider;
import cz.bliksoft.javautils.app.ui.interfaces.IIconSpecPropertyProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.Property;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tooltip;

/**
 * Wires {@link IUIAction} properties to JavaFX controls. All {@code bind}
 * methods are one-way: after calling them the control's
 * enabled/visible/text/icon/tooltip state tracks the action's observable
 * properties for the lifetime of the control.
 */
public final class ActionBinder {
	private ActionBinder() {
	}

	/**
	 * Binds an {@link IUIAction} to a {@link ButtonBase} (Button, ToggleButton,
	 * etc.).
	 *
	 * @param btn the button to wire
	 * @param a   the action to bind
	 */
	public static void bind(ButtonBase btn, IUIAction a) {
		btn.setOnAction(e -> a.execute());

		var enabled = a.enabledProperty();
		if (enabled != null) {
			// enabled -> disable
			btn.disableProperty().bind(Bindings.not(a.enabledProperty()));
		}

		var visible = a.visibleProperty();
		if (visible != null) {
			// visible -> visible + managed (avoid layout gaps)
			btn.visibleProperty().bind(a.visibleProperty());
			btn.managedProperty().bind(a.visibleProperty());
		}

		// text/graphic (bind only if you want action to own them)
		if (a.textProperty() != null)
			btn.textProperty().bind(a.textProperty());

		if (a instanceof IIconSpecPropertyProvider p) {
			IconBinder.bindToolbarIcon(btn, p, IconspecUtils.getIconspecSize("toolbar-size", 24)); //$NON-NLS-1$
		} else if (a instanceof IGraphicsProvider g) {
			g.graphicsProperty().addListener((obs, o, n) -> btn.setGraphic(n));
			btn.setGraphic(g.graphicsProperty().getValue());
		}

		bindCssClasses(btn.getStyleClass(), a);
		bindHint(btn, a);
	}

	/**
	 * Binds an {@link IUIAction} to a {@link MenuItem}.
	 *
	 * @param mi the menu item to wire
	 * @param a  the action to bind
	 */
	public static void bind(MenuItem mi, IUIAction a) {
		mi.setOnAction(e -> a.execute());

		var enabled = a.enabledProperty();
		if (enabled != null) {
			mi.disableProperty().bind(Bindings.not(a.enabledProperty()));
		}

		var visible = a.visibleProperty();
		if (visible != null) {
			// MenuItem has visibleProperty in modern JavaFX; if you target very old
			// versions, guard it.
			try {
				mi.visibleProperty().bind(a.visibleProperty());
			} catch (Throwable ignored) {
				// If running on an older JavaFX where MenuItem has no visibleProperty,
				// you can approximate by disabling, or handle via parent menu rebuild.
				mi.disableProperty().bind(Bindings.not(a.visibleProperty()).or(Bindings.not(a.enabledProperty())));
			}
		}

		if (a.textProperty() != null)
			mi.textProperty().bind(a.textProperty());

		if (a instanceof IIconSpecPropertyProvider p) {
			double menuSize = IconspecUtils.getIconspecSize("menu-icon-size", 16); //$NON-NLS-1$
			Property<String> menuSpec = p.menuIconSpecProperty();
			if (menuSpec != null)
				IconBinder.bindIcon(node -> mi.setGraphic(node), menuSpec, menuSize);
			else
				IconBinder.bindMenuIcon(mi, p, menuSize);
		} else if (a instanceof IGraphicsProvider g) {
			g.graphicsProperty().addListener((obs, o, n) -> mi.setGraphic(n));
			mi.setGraphic(g.graphicsProperty().getValue());
		}

		if (a.acceleratorProperty() != null && mi.acceleratorProperty() != null) {
			mi.acceleratorProperty().bind(a.acceleratorProperty());
		}

		bindCssClasses(mi.getStyleClass(), a);

		// MenuItem has no native tooltip; bind to the graphic node if it is a Control.
		if (a.hintProperty() != null && mi.getGraphic() instanceof Control gc) {
			bindHint(gc, a);
		}
	}

	/**
	 * Binds an {@link IUIActionWithSubactions} to a {@link MenuButton}.
	 *
	 * <p>
	 * The button's items are kept in sync with
	 * {@link IUIActionWithSubactions#getSubactions()}. Each subaction becomes a
	 * {@link MenuItem} bound via the standard {@link #bind(MenuItem, IUIAction)}.
	 *
	 * @param mb the menu button to wire
	 * @param a  the action with subactions to bind
	 */
	public static void bind(MenuButton mb, IUIActionWithSubactions a) {
		bind((ButtonBase) mb, a);
		syncMenuItems(mb, a);
	}

	/**
	 * Binds an {@link IUIActionWithSubactions} to a {@link SplitMenuButton}.
	 *
	 * <p>
	 * The left (default) click fires {@link IUIAction#execute()}, which should run
	 * the first subaction. The dropdown lists all subactions.
	 *
	 * @param smb the split menu button to wire
	 * @param a   the action with subactions to bind
	 */
	public static void bind(SplitMenuButton smb, IUIActionWithSubactions a) {
		bind((ButtonBase) smb, a);
		syncMenuItems(smb, a);
	}

	private static void syncMenuItems(MenuButton mb, IUIActionWithSubactions a) {
		rebuildMenuItems(mb, a);
		a.getSubactions().addListener((ListChangeListener<IUIAction>) c -> rebuildMenuItems(mb, a));
	}

	private static void rebuildMenuItems(MenuButton mb, IUIActionWithSubactions a) {
		mb.getItems().clear();
		for (IUIAction action : a.getSubactions()) {
			MenuItem item = new MenuItem();
			bind(item, action);
			mb.getItems().add(item);
		}
	}

	/**
	 * Binds an {@link IUIAction} to a {@link Hyperlink}.
	 *
	 * @param hl the hyperlink to wire
	 * @param a  the action to bind
	 */
	public static void bind(Hyperlink hl, IUIAction a) {
		hl.setOnAction(e -> a.execute());
		hl.disableProperty().bind(Bindings.not(a.enabledProperty()));
		hl.visibleProperty().bind(a.visibleProperty());
		hl.managedProperty().bind(a.visibleProperty());
		if (a.textProperty() != null)
			hl.textProperty().bind(a.textProperty());
		if (a instanceof IIconSpecPropertyProvider p) {
			IconBinder.bindIcon(hl.graphicProperty()::setValue, p, IconspecUtils.getIconspecSize("tab-icon-size", 16)); //$NON-NLS-1$
		}
		bindCssClasses(hl.getStyleClass(), a);
		bindHint(hl, a);
	}

	private static void bindCssClasses(ObservableList<String> target, IUIAction a) {
		if (!(a instanceof ICSSClassesProvider cp))
			return;
		ObservableList<String> classes = cp.getCssClasses();
		target.addAll(classes);
		classes.addListener((ListChangeListener<String>) change -> {
			while (change.next()) {
				if (change.wasRemoved())
					target.removeAll(change.getRemoved());
				if (change.wasAdded())
					target.addAll(change.getAddedSubList());
			}
		});
	}

	private static void bindHint(Control c, IUIAction a) {
		ReadOnlyStringProperty hint = a.hintProperty();
		if (hint == null)
			return;
		Tooltip tt = new Tooltip();
		tt.textProperty().bind(hint);
		c.setTooltip(tt);
	}
}
