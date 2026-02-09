package cz.bliksoft.javautils.app.ui.actions;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuItem;

public final class ActionBinder {
	private ActionBinder() {
	}

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
	}

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
	}

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
	}
}
