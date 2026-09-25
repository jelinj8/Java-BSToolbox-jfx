package cz.bliksoft.javautils.fx.controls.editors.providers;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * {@link String} editor for values that may contain line breaks.
 *
 * <p>
 * The inline editor is a single-line {@link TextField}, which can't hold a
 * line break at all (JavaFX strips them from its text) - so there, and in the
 * display string, a line break is shown and typed as the two-character escape
 * {@code \n} (a tab as {@code \t}, a literal backslash as {@code \\}; any other
 * backslash sequence is kept as-is). The dialog ({@link #supportsDialog()}) is
 * a plain multi-line {@link TextArea} editing the real text.
 */
public class MultilineStringEditorProvider implements IValueEditorProvider<String> {

	private static final StringConverter<String> ESCAPING = new StringConverter<>() {
		@Override
		public String toString(String value) {
			return escape(value);
		}

		@Override
		public String fromString(String text) {
			return unescape(text);
		}
	};

	@Override
	public Node createEditor(ObjectProperty<String> prop) {
		TextField tf = new TextField();
		tf.setMaxWidth(Double.MAX_VALUE);
		Bindings.bindBidirectional(tf.textProperty(), prop, ESCAPING);
		return tf;
	}

	@Override
	public String toDisplayString(String value) {
		return escape(value);
	}

	@Override
	public String fromString(String s) {
		return unescape(s);
	}

	@Override
	public boolean supportsDialog() {
		return true;
	}

	@Override
	public void showDialog(Window owner, ObjectProperty<String> valueProperty) {
		TextArea area = new TextArea(valueProperty.get() != null ? valueProperty.get() : "");
		area.setPrefRowCount(8);
		area.setPrefColumnCount(40);

		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle(BSAppJFXMessages.getString("editor.button.edit"));
		dialog.initOwner(owner);
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(area);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		dialog.setResultConverter(bt -> bt == ButtonType.OK ? area.getText() : null);
		dialog.setOnShown(e -> area.requestFocus());

		dialog.showAndWait().ifPresent(valueProperty::set);
	}

	/** Line breaks/tabs/backslashes to their two-character escapes. */
	public static String escape(String value) {
		if (value == null)
			return "";
		StringBuilder sb = new StringBuilder(value.length() + 8);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '\\' -> sb.append("\\\\");
			case '\n' -> sb.append("\\n");
			case '\t' -> sb.append("\\t");
			case '\r' -> {
				// CRLF/CR line breaks are normalized to a single \n
				if (i + 1 >= value.length() || value.charAt(i + 1) != '\n')
					sb.append("\\n");
			}
			default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * The inverse of {@link #escape}; an unknown escape or a trailing lone
	 * backslash is kept literally.
	 */
	public static String unescape(String text) {
		if (text == null)
			return null;
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c != '\\' || i + 1 >= text.length()) {
				sb.append(c);
				continue;
			}
			char next = text.charAt(i + 1);
			switch (next) {
			case 'n' -> sb.append('\n');
			case 't' -> sb.append('\t');
			case '\\' -> sb.append('\\');
			default -> {
				sb.append(c).append(next);
			}
			}
			i++;
		}
		return sb.toString();
	}
}
