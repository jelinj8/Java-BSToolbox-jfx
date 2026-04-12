package cz.bliksoft.javautils.fx.binding;

import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import javafx.scene.Node;

public enum ObjectStatus implements IIconNodeProvider {
	INITIAL, NEW, SAVED, MODIFIED, CHILD_MODIFIED, DETACHED, DELETED, DELETED_SAVED;

	/**
	 * SVG path data for this status badge glyph, designed on a 16×16 grid. Hollow
	 * shapes (INITIAL, DELETED_SAVED) use opposite sub-path windings so the inner
	 * region becomes a hole under the non-zero fill rule. Returns an empty string
	 * for SAVED (no badge shown) and as a safe default.
	 */
	public String getSVGPath() {
		switch (this) {
		case INITIAL:
			// Hollow diamond — outer CW (+1), inner CCW (−1) → centre hole
			return "M8,0 L16,8 L8,16 L0,8 Z M8,4 L4,8 L8,12 L12,8 Z";
		case NEW:
			// Plus sign — newly created, not yet persisted
			return "M6,0 L10,0 L10,6 L16,6 L16,10 L10,10 L10,16 L6,16 L6,10 L0,10 L0,6 L6,6 Z";
		case SAVED:
			// Clean/persisted — no badge
			return "";
		case MODIFIED:
			// Diagonal band (pencil stroke) — object directly edited
			return "M12,0 L16,4 L4,16 L0,12 Z";
		case CHILD_MODIFIED:
			// Small pencil + three square dots — only children changed
			return "M12,0 L16,4 L10,10 L6,6 Z M1,12 L4,12 L4,15 L1,15 Z M5,12 L8,12 L8,15 L5,15 Z M9,12 L12,12 L12,15 L9,15 Z";
		case DETACHED:
			// Split bar (broken link) — unlinked from the system
			return "M0,6 L5,6 L5,10 L0,10 Z M11,6 L16,6 L16,10 L11,10 Z";
		case DELETED:
			// X mark — marked for deletion, not yet persisted
			return "M2,0 L8,6 L14,0 L16,2 L10,8 L16,14 L14,16 L8,10 L2,16 L0,14 L6,8 L0,2 Z";
		case DELETED_SAVED:
			// X inside a ring — deletion persisted, object will be forgotten
			// Three sub-paths: outer octagon CW (+1), inner octagon CCW (−1) → ring,
			// then X CW (+1) re-fills the centre hole with a cross
			return "M5,0 L11,0 L16,5 L16,11 L11,16 L5,16 L0,11 L0,5 Z"
					+ " M7,2 L3,6 L3,10 L7,14 L9,14 L13,10 L13,6 L9,2 Z"
					+ " M5,4 L8,7 L11,4 L12,5 L9,8 L12,11 L11,12 L8,9 L5,12 L4,11 L7,8 L4,5 Z";
		default:
			return "";
		}
	}

	@Override
	public Node getImageNode() {
		if (this == ObjectStatus.SAVED)
			return null;
		String spec = "icons/overlay/status/" + this.name().toLowerCase().replace('_', '-') + ".png";
		return ImageUtils.getIconNode(spec);
	}

}
