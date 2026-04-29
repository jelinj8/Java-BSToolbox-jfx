package cz.bliksoft.javautils.app.ui.actions;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ListChangeListener;
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

		if (a.iconSpecProperty() != null) {
			IconBinder.bindToolbarIcon(btn, a, 24);
		} else if (a.graphicProperty() != null) {
			// Compatibility fallback (NOTE: Node may be shared; clone if you need)
			a.graphicProperty().addListener((obs, o, n) -> btn.setGraphic(n));
			btn.setGraphic(a.graphicProperty().get());
		}

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

		if (a.iconSpecProperty() != null) {
			IconBinder.bindMenuIcon(mi, a, 16);
		} else if (a.graphicProperty() != null) {
			a.graphicProperty().addListener((obs, o, n) -> mi.setGraphic(n));
			mi.setGraphic(a.graphicProperty().get());
		}

		if (a.acceleratorProperty() != null && mi.acceleratorProperty() != null) {
			mi.acceleratorProperty().bind(a.acceleratorProperty());
		}

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
		if (a.iconSpecProperty() != null) {
			IconBinder.bindIcon(hl.graphicProperty()::setValue, a, 16);
		}
		bindHint(hl, a);
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
