package cz.bliksoft.javautils.app.ui.actions;

import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

public final class IconBinder {
	private IconBinder() {
	}

	public static void bindToolbarIcon(ButtonBase btn, IUIAction a, double sizePx) {
		bindIcon(node -> btn.setGraphic(node), a, sizePx);
	}

	public static void bindMenuIcon(MenuItem mi, IUIAction a, double sizePx) {
		bindIcon(node -> mi.setGraphic(node), a, sizePx);
	}

	public static void bindIcon(java.util.function.Consumer<Node> setter, IUIAction a, double sizePx) {
		if (a.iconSpecProperty() == null)
			return;

		ChangeListener<String> l = (obs, oldSpec, newSpec) -> {
			if (newSpec == null || newSpec.isBlank()) {
				setter.accept(null);
				return;
			}

			if (newSpec.contains(ImageUtils.SCALE_PLACEHOLDER)) {
				Node icon = ImageUtils.getIconNode(newSpec);
				setter.accept(icon);
			} else {
				// IMPORTANT: must be a NEW node instance each time
				Node icon = ImageUtils.getIconNode(newSpec.trim());

				// Optional: enforce size at binder level (best-effort)
				enforceIconSize(icon, sizePx);
				setter.accept(icon);
			}

		};

		// initial + updates
		l.changed(a.iconSpecProperty(), null, a.iconSpecProperty().get());
		a.iconSpecProperty().addListener(l);
	}

	/** Best-effort sizing for common icon node types. */
	public static void enforceIconSize(Node icon, double sizePx) {
		if (icon == null)
			return;

		if (icon instanceof ImageView iv) {
			iv.setPreserveRatio(true);
			iv.setFitWidth(sizePx);
			iv.setFitHeight(sizePx);
			return;
		}

		if (icon instanceof Region r) {
			r.setMinSize(sizePx, sizePx);
			r.setPrefSize(sizePx, sizePx);
			r.setMaxSize(sizePx, sizePx);
			return;
		}

		if (icon instanceof SVGPath p) {
			// Scale SVGPath to fit sizePx based on its local bounds
			var b = p.getBoundsInLocal();
			double w = b.getWidth();
			double h = b.getHeight();
			if (w > 0 && h > 0) {
				double scale = sizePx / Math.max(w, h);
				p.setScaleX(scale);
				p.setScaleY(scale);
			}
		}
	}
}
