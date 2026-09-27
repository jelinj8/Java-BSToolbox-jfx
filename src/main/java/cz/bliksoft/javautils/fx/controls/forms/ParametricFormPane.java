package cz.bliksoft.javautils.fx.controls.forms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import cz.bliksoft.javautils.CsvUtils;
import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.freemarker.utils.TemplateValueCoercion;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

/**
 * A form generated from a list of {@link FormField}s - e.g. the
 * {@code {var|...}} declarations of a template ({@link FormField#fromTemplate}),
 * but usable for any set of parameters. Title column + control column; field
 * types:
 * <ul>
 * <li>STRING - text field; MULTILINE - text area</li>
 * <li>INT - spinner ({@code parameters} = {@code min:max:step}, all optional),
 * value {@code Integer}</li>
 * <li>DECIMAL - text field accepting a decimal dot or comma, value
 * {@code Double} ({@code null} when empty)</li>
 * <li>BOOLEAN - check box</li>
 * <li>COMBO / FONT - choice of the {@linkplain #setOptionsResolver options};
 * FONT without options is a text field</li>
 * <li>CSVFILE - file chooser, value = the file's rows
 * ({@link CsvUtils#loadCsvList}, {@code ;}, header row)</li>
 * <li>INFO - read-only row showing the default; COMMENT - text over both
 * columns; neither has a value</li>
 * <li>HIDDEN - no row, value = the default</li>
 * </ul>
 * {@link #setFields} keeps the values already entered for fields of the same
 * name (COMBO/FONT only when still an option), so e.g. switching a template
 * doesn't lose them.
 */
public class ParametricFormPane extends GridPane {

	private static final Pattern DECIMAL_INPUT = Pattern.compile("-?[0-9]*([.,][0-9]*)?");

	private final List<FormField> fields = new ArrayList<>();
	private final Map<String, Node> controls = new LinkedHashMap<>();
	private Function<FormField, List<String>> optionsResolver = FormField::options;

	public ParametricFormPane() {
		setHgap(10);
		setVgap(6);
		ColumnConstraints titles = new ColumnConstraints();
		titles.setMinWidth(130);
		ColumnConstraints values = new ColumnConstraints();
		values.setHgrow(Priority.ALWAYS);
		values.setFillWidth(true);
		getColumnConstraints().setAll(titles, values);
	}

	/**
	 * Options of COMBO/FONT fields; default = the field's own
	 * {@link FormField#options() parameters}. Applies from the next
	 * {@link #setFields}.
	 */
	public void setOptionsResolver(Function<FormField, List<String>> resolver) {
		optionsResolver = resolver != null ? resolver : FormField::options;
	}

	/** Rebuilds the form, keeping values entered for fields of the same name. */
	public void setFields(List<FormField> newFields) {
		Map<String, String> previous = enteredValues();
		getChildren().clear();
		controls.clear();
		fields.clear();
		if (newFields != null)
			fields.addAll(newFields);

		int row = 0;
		for (FormField field : fields) {
			String type = field.normalizedType();
			String title = field.title() != null ? field.title() : field.name();
			switch (type) {
			case "HIDDEN" -> {
				continue;
			}
			case "INFO" -> {
				Label infoTitle = new Label(title + ":");
				GridPane.setHalignment(infoTitle, HPos.RIGHT);
				addRow(row++, infoTitle, new Label(nvl(field.defaultValue())));
				continue;
			}
			case "COMMENT" -> {
				Label comment = new Label(title);
				comment.setWrapText(true);
				comment.setMaxWidth(Double.MAX_VALUE);
				GridPane.setColumnSpan(comment, 2);
				addRow(row++, comment);
				continue;
			}
			default -> {
			}
			}
			List<String> options = "COMBO".equals(type) || "FONT".equals(type) ? optionsResolver.apply(field)
					: List.of();
			String initial = effectiveDefault(type, field.defaultValue(), previous.get(field.name()), options);
			Node control = buildControl(type, initial, field.parameters(), options);
			controls.put(field.name(), control);
			if (control instanceof Control c)
				c.setMaxWidth(Double.MAX_VALUE);
			Label fieldTitle = new Label(title + ":");
			GridPane.setHalignment(fieldTitle, HPos.RIGHT);
			addRow(row++, fieldTitle, control);
		}
	}

	public List<FormField> getFields() {
		return List.copyOf(fields);
	}

	/** Whether the form shows anything (fields other than HIDDEN). */
	public boolean hasVisibleFields() {
		return fields.stream().anyMatch(f -> !"HIDDEN".equals(f.normalizedType()));
	}

	/**
	 * The values by field name: HIDDEN = its default ({@code null} when empty),
	 * INFO/COMMENT have none.
	 *
	 * @throws IOException a CSVFILE field's file can't be read
	 */
	public Map<String, Object> getValues() throws IOException {
		Map<String, Object> values = new LinkedHashMap<>();
		for (FormField field : fields) {
			String type = field.normalizedType();
			if ("HIDDEN".equals(type)) {
				String value = field.defaultValue();
				values.put(field.name(), value == null || value.isEmpty() ? null : value);
				continue;
			}
			Node control = controls.get(field.name());
			if (control != null)
				values.put(field.name(), readValue(type, control));
		}
		return values;
	}

	/** Focuses the first editable control (a CSVFILE field's browse button). */
	public void focusFirst() {
		if (controls.isEmpty())
			return;
		Node first = controls.values().iterator().next();
		if (first instanceof HBox box && box.getChildren().size() >= 2
				&& box.getChildren().get(1) instanceof Control inner)
			inner.requestFocus();
		else
			first.requestFocus();
	}

	/**
	 * The value a rebuilt field starts with: the previously entered one when
	 * usable, else the field's default. COMBO/FONT keep the previous value only
	 * when it's still one of the options (or there are no options - a free text
	 * field); CSVFILE, INFO, ... always start from the default.
	 */
	static String effectiveDefault(String type, String fieldDefault, String previous, List<String> options) {
		if (previous == null)
			return fieldDefault;
		return switch (type) {
		case "COMBO", "FONT" -> options.isEmpty() || options.contains(previous) ? previous : fieldDefault;
		case "STRING", "MULTILINE", "BOOLEAN", "INT", "DECIMAL" -> previous;
		default -> fieldDefault;
		};
	}

	/** INT {@code min:max:step} ({@code parameters}), missing parts = no limit / 1. */
	static int[] intRange(String parameters) {
		int[] range = { Integer.MIN_VALUE, Integer.MAX_VALUE, 1 };
		if (parameters == null)
			return range;
		String[] parts = parameters.split(":");
		for (int i = 0; i < Math.min(3, parts.length); i++) {
			try {
				if (!parts[i].isBlank())
					range[i] = Integer.parseInt(parts[i].strip());
			} catch (NumberFormatException ignored) {
				// not a range - keep the default
			}
		}
		if (range[2] < 1)
			range[2] = 1;
		return range;
	}

	/** Entered values as text, for {@link #effectiveDefault} after a rebuild. */
	private Map<String, String> enteredValues() {
		Map<String, String> entered = new HashMap<>();
		for (FormField field : fields) {
			Node control = controls.get(field.name());
			String type = field.normalizedType();
			if (control == null || "CSVFILE".equals(type))
				continue;
			String text = switch (control) {
			case Spinner<?> s -> s.getValue() != null ? s.getValue().toString() : null;
			case CheckBox cb -> Boolean.toString(cb.isSelected());
			case ComboBox<?> cb -> cb.getValue() != null ? cb.getValue().toString() : null;
			case TextArea ta -> ta.getText();
			case TextField tf -> tf.getText();
			default -> null;
			};
			if (text != null)
				entered.put(field.name(), text);
		}
		return entered;
	}

	private Node buildControl(String type, String initial, String parameters, List<String> options) {
		return switch (type) {
		case "INT" -> {
			int[] range = intRange(parameters);
			int value = 0;
			try {
				if (initial != null && !initial.isBlank())
					value = Integer.parseInt(initial.strip());
			} catch (NumberFormatException ignored) {
				// not a number - 0
			}
			value = Math.max(range[0], Math.min(range[1], value));
			Spinner<Integer> s = new Spinner<>(
					new SpinnerValueFactory.IntegerSpinnerValueFactory(range[0], range[1], value, range[2]));
			s.setEditable(true);
			yield s;
		}
		case "DECIMAL" -> {
			TextField tf = new TextField();
			tf.setTextFormatter(
					new TextFormatter<String>(c -> DECIMAL_INPUT.matcher(c.getControlNewText()).matches() ? c : null));
			String text = nvl(initial).strip();
			tf.setText(DECIMAL_INPUT.matcher(text).matches() ? text : "");
			yield tf;
		}
		case "MULTILINE" -> {
			TextArea ta = new TextArea(nvl(initial));
			ta.setPrefRowCount(3);
			ta.setWrapText(true);
			yield ta;
		}
		case "COMBO", "FONT" -> {
			if (options.isEmpty() && "FONT".equals(type))
				// no fonts known (e.g. no printer selected) - a raw font name/file
				yield new TextField(nvl(initial));
			ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(options));
			if (initial != null && !initial.isEmpty())
				cb.setValue(initial);
			else if (!options.isEmpty())
				cb.getSelectionModel().selectFirst();
			yield cb;
		}
		case "BOOLEAN" -> {
			CheckBox cb = new CheckBox();
			cb.setSelected("true".equalsIgnoreCase(initial));
			yield cb;
		}
		case "CSVFILE" -> {
			TextField pathField = new TextField();
			pathField.setEditable(false);
			pathField.setPromptText(BSAppJFXMessages.getString("parametricForm.csvfile.prompt"));
			HBox.setHgrow(pathField, Priority.ALWAYS);
			Button browse = new Button(BSAppJFXMessages.getString("parametricForm.browse"));
			browse.setOnAction(e -> {
				FileChooser fc = new FileChooser();
				fc.setTitle(BSAppJFXMessages.getString("parametricForm.csvfile.chooserTitle"));
				fc.getExtensionFilters().addAll(
						new FileChooser.ExtensionFilter(BSAppJFXMessages.getString("parametricForm.csvfile.filter"),
								"*.csv", "*.CSV"),
						new FileChooser.ExtensionFilter(BSAppJFXMessages.getString("parametricForm.allFiles"), "*.*"));
				File chosen = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
				if (chosen != null)
					pathField.setText(chosen.getAbsolutePath());
			});
			HBox box = new HBox(4, pathField, browse);
			box.setMaxWidth(Double.MAX_VALUE);
			yield box;
		}
		default -> new TextField(nvl(initial));
		};
	}

	private static Object readValue(String type, Node control) throws IOException {
		return switch (type) {
		case "INT" -> control instanceof Spinner<?> s ? s.getValue() : 0;
		case "DECIMAL" -> control instanceof TextField tf ? TemplateValueCoercion.parseDecimal(tf.getText()) : null;
		case "BOOLEAN" -> control instanceof CheckBox cb && cb.isSelected();
		case "COMBO", "FONT" -> {
			if (control instanceof ComboBox<?> cb)
				yield cb.getValue() != null ? cb.getValue().toString() : "";
			yield control instanceof TextField tf ? tf.getText() : "";
		}
		case "CSVFILE" -> {
			if (control instanceof HBox box && !box.getChildren().isEmpty()
					&& box.getChildren().get(0) instanceof TextField tf) {
				String path = tf.getText();
				if (path == null || path.isBlank())
					yield null;
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
					yield CsvUtils.loadCsvList(reader, ";", true);
				} catch (IOException e) {
					throw e;
				} catch (Exception e) {
					throw new IOException(e.getMessage(), e);
				}
			}
			yield null;
		}
		default -> {
			if (control instanceof TextArea ta)
				yield ta.getText();
			yield control instanceof TextField tf ? tf.getText() : "";
		}
		};
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}
}
