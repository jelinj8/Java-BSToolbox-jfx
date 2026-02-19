package cz.bliksoft.javautils.app.ui.utils.state;

import javafx.scene.Node;
import javafx.stage.Stage;

import java.util.Objects;

import cz.bliksoft.javautils.app.ui.utils.state.binders.StageStateBinder;

public final class FxWindowState {

	private FxWindowState() {
	}

	/** Obnoví Stage + uložené komponenty v podstromu. */
	public static void restoreWindow(Stage stage, Node root, String windowKey) {
		Objects.requireNonNull(stage);
		Objects.requireNonNull(root);
		Objects.requireNonNull(windowKey);

		// 1) Stage
		StageStateBinder.restore(stage, windowKey + ".@stage");

		// 2) Podstrom (dědičný ctx/key uvnitř rootu)
		FxStateManager sm = new FxStateManager(windowKey);
		sm.restoreState(root);
	}

	/**
	 * Uloží Stage + stav komponent v podstromu (on-demand, typicky při zavření
	 * okna).
	 */
	public static void persistWindow(Stage stage, Node root, String windowKey) {
		Objects.requireNonNull(stage);
		Objects.requireNonNull(root);
		Objects.requireNonNull(windowKey);

		// 1) Podstrom
		FxStateManager sm = new FxStateManager(windowKey);
		sm.persistState(root);

		// 2) Stage
		StageStateBinder.save(stage, windowKey + ".@stage");
	}
}
