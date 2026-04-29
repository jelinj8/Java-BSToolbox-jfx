package cz.bliksoft.javautils.fx.customization;

import cz.bliksoft.javautils.app.BSAppMessages;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

public final class BSButtonTypes {

	private BSButtonTypes() {
	}

	public static ButtonType ok() {
		return new ButtonType(BSAppMessages.getString("button.ok"), ButtonData.OK_DONE);
	}

	public static ButtonType cancel() {
		return new ButtonType(BSAppMessages.getString("button.cancel"), ButtonData.CANCEL_CLOSE);
	}

	public static ButtonType yes() {
		return new ButtonType(BSAppMessages.getString("button.yes"), ButtonData.YES);
	}

	public static ButtonType no() {
		return new ButtonType(BSAppMessages.getString("button.no"), ButtonData.NO);
	}

	public static ButtonType back() {
		return new ButtonType(BSAppMessages.getString("button.back"), ButtonData.BACK_PREVIOUS);
	}

	public static ButtonType previous() {
		return new ButtonType(BSAppMessages.getString("button.previous"), ButtonData.BACK_PREVIOUS);
	}

	public static ButtonType next() {
		return new ButtonType(BSAppMessages.getString("button.next"), ButtonData.NEXT_FORWARD);
	}

	public static ButtonType finish() {
		return new ButtonType(BSAppMessages.getString("button.finish"), ButtonData.FINISH);
	}

	public static ButtonType apply() {
		return new ButtonType(BSAppMessages.getString("button.apply"), ButtonData.APPLY);
	}

	public static ButtonType exit() {
		return new ButtonType(BSAppMessages.getString("button.exit"), ButtonData.OTHER);
	}

	public static ButtonType close() {
		return new ButtonType(BSAppMessages.getString("button.close"), ButtonData.CANCEL_CLOSE);
	}
}
