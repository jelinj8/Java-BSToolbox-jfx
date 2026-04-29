package cz.bliksoft.javautils.app.ui.builder;

import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;

import java.util.IdentityHashMap;
import java.util.Map;

import cz.bliksoft.javautils.app.ui.actions.IUIAction;

/**
 * Manages keyboard accelerators for {@link IUIAction} instances on a
 * {@link Scene}.
 *
 * <p>
 * Each bound action gets its accelerator installed in the scene's accelerator
 * map. The binding respects the action's enabled and visible properties — the
 * shortcut only fires when both are {@code true}. Accelerator changes are
 * tracked live via the action's {@code acceleratorProperty()}.
 */
public final class AcceleratorManager {

	/**
	 * Creates an unattached accelerator manager; call {@link #attach(Scene)} before
	 * binding actions.
	 */
	public AcceleratorManager() {
	}

	private Scene scene;

	private static final class Binding {
		KeyCombination currentKey;
		final Runnable runnable;
		final ChangeListener<KeyCombination> keyListener;
		final ChangeListener<Boolean> gateListener;

		Binding(Runnable runnable, ChangeListener<KeyCombination> keyListener, ChangeListener<Boolean> gateListener) {
			this.runnable = runnable;
			this.keyListener = keyListener;
			this.gateListener = gateListener;
		}
	}

	// One binding per IUIAction instance
	private final Map<IUIAction, Binding> bindings = new IdentityHashMap<>();

	/**
	 * Attaches this manager to a scene and (re-)installs all existing bindings.
	 *
	 * @param scene the scene whose accelerator map will be managed
	 */
	public void attach(Scene scene) {
		this.scene = scene;
		// Re-apply any existing action bindings to the new scene
		bindings.forEach((a, b) -> installOrUpdate(a, b));
	}

	/**
	 * Registers an action and installs its accelerator in the current scene. Does
	 * nothing if the action has no {@code acceleratorProperty()}.
	 *
	 * @param action the action to register
	 */
	public void bind(IUIAction action) {
		if (action.acceleratorProperty() == null)
			return;

		ChangeListener<KeyCombination> keyListener = (obs, oldK, newK) -> refresh(action);
		ChangeListener<Boolean> gateListener = (obs, o, n) -> refresh(action);

		Binding binding = new Binding(() -> { // invoked by Scene accelerator
			if (action.visibleProperty().get() && action.enabledProperty().get()) {
				action.execute();
			}
		}, keyListener, gateListener);

		bindings.put(action, binding);

		action.acceleratorProperty().addListener(keyListener);
		action.enabledProperty().addListener(gateListener);
		action.visibleProperty().addListener(gateListener);

		installOrUpdate(action, binding);
	}

	/**
	 * Removes all accelerator bindings for the given action.
	 *
	 * @param action the action to deregister
	 */
	public void unbind(IUIAction action) {
		Binding b = bindings.remove(action);
		if (b == null)
			return;

		if (action.acceleratorProperty() != null) {
			action.acceleratorProperty().removeListener(b.keyListener);
		}
		action.enabledProperty().removeListener(b.gateListener);
		action.visibleProperty().removeListener(b.gateListener);

		if (scene != null && b.currentKey != null) {
			scene.getAccelerators().remove(b.currentKey, b.runnable);
		}
	}

	private void refresh(IUIAction action) {
		Binding b = bindings.get(action);
		if (b != null)
			installOrUpdate(action, b);
	}

	private void installOrUpdate(IUIAction action, Binding b) {
		if (scene == null)
			return;

		KeyCombination newKey = action.acceleratorProperty().get();

		// Remove old key mapping if changed
		if (b.currentKey != null && (newKey == null || !b.currentKey.equals(newKey))) {
			scene.getAccelerators().remove(b.currentKey, b.runnable);
			b.currentKey = null;
		}

		// Install if present
		if (newKey != null && b.currentKey == null) {
			b.currentKey = newKey;
			scene.getAccelerators().put(newKey, b.runnable);
		}
	}
}
