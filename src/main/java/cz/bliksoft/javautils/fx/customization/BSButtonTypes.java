package cz.bliksoft.javautils.fx.customization;

import cz.bliksoft.javautils.app.BSAppMessages;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

public final class BSButtonTypes {

	public static final ButtonType OK = new ButtonType(BSAppMessages.getString("button.ok"), ButtonData.OK_DONE);
	public static final ButtonType CANCEL = new ButtonType(BSAppMessages.getString("button.cancel"),
			ButtonData.CANCEL_CLOSE);
	public static final ButtonType YES = new ButtonType(BSAppMessages.getString("button.yes"), ButtonData.YES);
	public static final ButtonType NO = new ButtonType(BSAppMessages.getString("button.no"), ButtonData.NO);
	public static final ButtonType CLOSE = new ButtonType(BSAppMessages.getString("button.close"),
			ButtonData.CANCEL_CLOSE);
	public static final ButtonType APPLY = new ButtonType(BSAppMessages.getString("button.apply"), ButtonData.APPLY);
	public static final ButtonType FINISH = new ButtonType(BSAppMessages.getString("button.finish"), ButtonData.FINISH);
	public static final ButtonType NEXT = new ButtonType(BSAppMessages.getString("button.next"),
			ButtonData.NEXT_FORWARD);
	public static final ButtonType PREVIOUS = new ButtonType(BSAppMessages.getString("button.previous"),
			ButtonData.BACK_PREVIOUS);
	public static final ButtonType BACK = new ButtonType(BSAppMessages.getString("button.back"),
			ButtonData.BACK_PREVIOUS);
	public static final ButtonType EXIT = new ButtonType(BSAppMessages.getString("button.exit"), ButtonData.OTHER);

	private BSButtonTypes() {
	}

	/**
	 * Replaces standard JavaFX {@link ButtonType} singletons in an {@link Alert}
	 * with the localized equivalents from this class.
	 *
	 * <p>
	 * Call immediately after constructing the Alert, before showing it.
	 */
	public static void localizeAlert(Alert alert) {
		alert.getButtonTypes().replaceAll(bt -> {
			if (bt == ButtonType.OK)
				return OK;
			if (bt == ButtonType.CANCEL)
				return CANCEL;
			if (bt == ButtonType.YES)
				return YES;
			if (bt == ButtonType.NO)
				return NO;
			if (bt == ButtonType.CLOSE)
				return CLOSE;
			return bt;
		});
	}
}
