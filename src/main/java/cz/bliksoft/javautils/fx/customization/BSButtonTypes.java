package cz.bliksoft.javautils.fx.customization;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

public final class BSButtonTypes {

	public static final ButtonType OK = new ButtonType(BSAppJFXMessages.getString("button.ok"), ButtonData.OK_DONE);
	public static final ButtonType CANCEL = new ButtonType(BSAppJFXMessages.getString("button.cancel"),
			ButtonData.CANCEL_CLOSE);
	public static final ButtonType YES = new ButtonType(BSAppJFXMessages.getString("button.yes"), ButtonData.YES);
	public static final ButtonType NO = new ButtonType(BSAppJFXMessages.getString("button.no"), ButtonData.NO);
	public static final ButtonType CLOSE = new ButtonType(BSAppJFXMessages.getString("button.close"),
			ButtonData.CANCEL_CLOSE);
	public static final ButtonType APPLY = new ButtonType(BSAppJFXMessages.getString("button.apply"), ButtonData.APPLY);
	public static final ButtonType SAVE = new ButtonType(BSAppJFXMessages.getString("button.save"), ButtonData.OK_DONE);
	public static final ButtonType DISCARD = new ButtonType(BSAppJFXMessages.getString("button.discard"),
			ButtonData.OTHER);
	public static final ButtonType FINISH = new ButtonType(BSAppJFXMessages.getString("button.finish"),
			ButtonData.FINISH);
	public static final ButtonType NEXT = new ButtonType(BSAppJFXMessages.getString("button.next"),
			ButtonData.NEXT_FORWARD);
	public static final ButtonType PREVIOUS = new ButtonType(BSAppJFXMessages.getString("button.previous"),
			ButtonData.BACK_PREVIOUS);
	public static final ButtonType BACK = new ButtonType(BSAppJFXMessages.getString("button.back"),
			ButtonData.BACK_PREVIOUS);
	public static final ButtonType EXIT = new ButtonType(BSAppJFXMessages.getString("button.exit"), ButtonData.OTHER);

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
