package cz.bliksoft.javautils.app.ui.actions;

import java.lang.reflect.Method;

import org.controlsfx.control.action.Action;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.shape.SVGPath;

public final class UIActions {
	private UIActions() {
	}

	/**
	 * Adapts ControlsFX {@link Action} to your {@link IUIAction}.
	 *
	 * Icon spec support:
	 * - preferred: a.getProperties().put("iconSpec", "res:/icons/save@{scale}x.png")
	 * - fallback: tries to derive from Action.graphicProperty() (ImageView url, SVGPath content)
	 */
	public static IUIAction fromControlsFx(Action a) {

		// Icon spec (string) used by your ImageUtils-based binding.
		// We keep it in a wrapper so we can update it from action properties / graphic.
		final ReadOnlyStringWrapper iconSpec = new ReadOnlyStringWrapper(null);

		// 1) Prefer action.getProperties()["iconSpec"] if available
		wireIconSpecFromActionProperties(a, iconSpec);

		// 2) Fallback: derive from graphic if iconSpec not set
		wireIconSpecFromGraphicFallback(a, iconSpec);

		return new IUIAction() {

			@Override
			public void execute() {
				// ControlsFX Action implements EventHandler<ActionEvent>
				a.handle(null);
			}

			@Override
			public ObservableBooleanValue enabledProperty() {
				// BooleanBinding is fine as ObservableBooleanValue
				return a.disabledProperty().not();
			}

			// ControlsFX Action may not have built-in visibility -> keep custom
			private final javafx.beans.property.BooleanProperty visible =
					new javafx.beans.property.SimpleBooleanProperty(true);

			@Override
			public ReadOnlyBooleanProperty visibleProperty() {
				return visible;
			}

			@Override
			public ReadOnlyStringProperty textProperty() {
				return a.textProperty();
			}

			@Override
			public ReadOnlyObjectProperty<Node> graphicProperty() {
				return a.graphicProperty();
			}

			@Override
			public ReadOnlyObjectProperty<KeyCombination> acceleratorProperty() {
				try {
					return a.acceleratorProperty();
				} catch (Throwable t) {
					return null;
				}
			}

			/**
			 * Icon spec for your ImageUtils binder.
			 *
			 * IMPORTANT: Do NOT annotate with @Override unless IUIAction definitely declares it.
			 * (This keeps the adapter usable even if some projects still compile with older IUIAction.)
			 */
			public ReadOnlyStringProperty iconSpecProperty() {
				return iconSpec.getReadOnlyProperty();
			}
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void wireIconSpecFromActionProperties(Action a, ReadOnlyStringWrapper iconSpec) {
		try {
			// ControlsFX Action typically has getProperties(): ObservableMap<Object,Object>
			Method m = a.getClass().getMethod("getProperties");
			Object propsObj = m.invoke(a);
			if (propsObj instanceof ObservableMap props) {
				// initial
				Object v = props.get("iconSpec");
				if (v instanceof String s && !s.isBlank()) {
					iconSpec.set(s.trim());
				}

				// live updates
				props.addListener((MapChangeListener) (MapChangeListener.Change ch) -> {
					if (!"iconSpec".equals(String.valueOf(ch.getKey()))) return;

					Object nv = ch.getMap().get("iconSpec");
					if (nv instanceof String s2 && !s2.isBlank()) {
						iconSpec.set(s2.trim());
					} else {
						// allow clearing; fallback may repopulate later
						iconSpec.set(null);
					}
				});
			}
		} catch (Throwable ignored) {
			// No properties map or not accessible -> ignore
		}
	}

	private static void wireIconSpecFromGraphicFallback(Action a, ReadOnlyStringWrapper iconSpec) {
		a.graphicProperty().addListener((obs, oldG, newG) -> {
			// Only derive if iconSpec isn't explicitly set (or was cleared)
			if (iconSpec.get() != null && !iconSpec.get().isBlank()) return;

			String derived = deriveIconSpecFromGraphic(newG);
			iconSpec.set((derived == null || derived.isBlank()) ? null : derived);
		});

		// initial
		if (iconSpec.get() == null || iconSpec.get().isBlank()) {
			String derived = deriveIconSpecFromGraphic(a.getGraphic());
			iconSpec.set((derived == null || derived.isBlank()) ? null : derived);
		}
	}

	private static String deriveIconSpecFromGraphic(Node g) {
		if (g == null) return null;

		// ImageView -> use URL if present (best we can do)
		if (g instanceof ImageView iv) {
			Image img = iv.getImage();
			if (img != null && img.getUrl() != null && !img.getUrl().isBlank()) {
				return img.getUrl(); // let ImageUtils interpret URLs if you support that
			}
			return null;
		}

		// SVGPath -> use your svgpath spec convention
		if (g instanceof SVGPath p) {
			String c = p.getContent();
			if (c != null && !c.isBlank()) {
				return "[P]:" + c; // matches your earlier svgpath prefix convention
			}
			return null;
		}

		// Unknown node type -> cannot derive
		return null;
	}
}
