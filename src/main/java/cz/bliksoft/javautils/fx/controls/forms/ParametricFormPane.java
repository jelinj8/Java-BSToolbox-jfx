package cz.bliksoft.javautils.fx.controls.forms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import cz.bliksoft.javautils.CsvUtils;
import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.freemarker.utils.TemplateDateValues;
import cz.bliksoft.javautils.freemarker.utils.TemplateFormSupport;
import cz.bliksoft.javautils.freemarker.utils.TemplateFormSupport.Option;
import cz.bliksoft.javautils.freemarker.utils.TemplateValueCoercion;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * A form generated from a list of {@link FormField}s - e.g. the
 * {@code {var|...}} declarations of a template
 * ({@link FormField#fromTemplate}), but usable for any set of parameters. Title
 * column + control column; field types:
 * <ul>
 * <li>STRING - text field; MULTILINE - text area</li>
 * <li>INT - spinner ({@code parameters} = {@code min:max:step}, all optional),
 * value {@code Integer}</li>
 * <li>DECIMAL - text field accepting a decimal dot or comma, value
 * {@code Double} ({@code null} when empty)</li>
 * <li>BOOLEAN - check box</li>
 * <li>DATE - date picker, value {@code LocalDate}; DATETIME - date picker and a
 * {@code HH:mm} time field, value {@code LocalDateTime} (no time = midnight);
 * {@code parameters} = {@code min..max} (date expressions) limits the days
 * offered; the default may be a date expression ({@code today+7},
 * {@link TemplateDateValues})</li>
 * <li>COMBO / FONT - choice of the {@linkplain #setOptionsResolver options};
 * FONT without options is a text field. A COMBO's options are replaced by the
 * {@linkplain #setModel model's} {@code <name>_options} (a list, or a map of
 * value → label); a FONT's only when the resolver gives none. The value is the
 * option's value (text).</li>
 * <li>CSVFILE - file chooser, value = the file's rows
 * ({@link CsvUtils#loadCsvList}, {@code ;}, header row)</li>
 * <li>INFO - read-only row showing the default; COMMENT - text over both
 * columns; neither has a value</li>
 * <li>HIDDEN - no row, value = the default</li>
 * </ul>
 * A field's {@link FormField#hint() hint} is shown as a tooltip of its title.
 * <p>
 * A field starts with ({@link TemplateFormSupport#initialValue}) the value the
 * user entered into a field of the same name before the last {@link #setFields}
 * (so e.g. switching a printer doesn't lose it; a value left as it was built is
 * not "entered"), else the value of the same name in the optional
 * {@linkplain #setModel model} (an application's data; also the text of INFO
 * and the value of HIDDEN), else the field's default. A value that doesn't fit
 * the field (not one of the options, ...) is skipped.
 */
public class ParametricFormPane extends GridPane {

	private static final Pattern DECIMAL_INPUT = Pattern.compile("-?[0-9]*([.,][0-9]*)?");
	private static final Pattern TIME_INPUT = Pattern.compile("[0-9]{0,2}(:[0-9]{0,2}(:[0-9]{0,2})?)?");
	/**
	 * Node property of a composite control: the node {@link #focusFirst} focuses.
	 */
	private static final String FOCUS_TARGET = "parametricForm.focusTarget";

	private final List<FormField> fields = new ArrayList<>();
	private final Map<String, Node> controls = new LinkedHashMap<>();
	/** The text each control was built with - a different one was entered. */
	private final Map<String, String> initialTexts = new HashMap<>();
	private final Map<String, String> hiddenValues = new HashMap<>();
	private Function<FormField, List<String>> optionsResolver = FormField::options;
	private Map<String, ?> model;

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

	/**
	 * The application's data the fields are filled from - values by field name,
	 * COMBO options as {@code <name>_options}; {@code null} = none. Applies from
	 * the next {@link #setFields}.
	 */
	public void setModel(Map<String, ?> model) {
		this.model = model;
	}

	public Map<String, ?> getModel() {
		return model;
	}

	/** {@link #setModel} and {@link #setFields}. */
	public void setFields(List<FormField> newFields, Map<String, ?> model) {
		setModel(model);
		setFields(newFields);
	}

	/**
	 * Rebuilds the form, keeping values entered for fields of the same name (see
	 * the class description).
	 */
	public void setFields(List<FormField> newFields) {
		Map<String, String> previous = enteredValues();
		getChildren().clear();
		controls.clear();
		initialTexts.clear();
		hiddenValues.clear();
		fields.clear();
		if (newFields != null)
			fields.addAll(newFields);

		int row = 0;
		for (FormField field : fields) {
			String type = field.normalizedType();
			String title = field.title() != null ? field.title() : field.name();
			switch (type) {
			case "HIDDEN" -> {
				hiddenValues.put(field.name(), modelOrDefault(type, field));
				continue;
			}
			case "INFO" -> {
				Label info = new Label(nvl(modelOrDefault(type, field)));
				addRow(row++, titleLabel(title, field.hint()), info);
				continue;
			}
			case "COMMENT" -> {
				Label comment = new Label(title);
				comment.setWrapText(true);
				comment.setMaxWidth(Double.MAX_VALUE);
				installHint(field.hint(), comment);
				GridPane.setColumnSpan(comment, 2);
				addRow(row++, comment);
				continue;
			}
			default -> {
			}
			}
			List<Option> options = "COMBO".equals(type) || "FONT".equals(type) ? TemplateFormSupport.resolveOptions(
					type, field.name(), field.parameters(), optionsResolver.apply(field), model) : List.of();
			String initial = effectiveDefault(type, field.name(), field.defaultValue(), model,
					previous.get(field.name()), Option.values(options));
			Node control = buildControl(type, initial, field.parameters(), options);
			controls.put(field.name(), control);
			initialTexts.put(field.name(), controlText(type, control));
			if (control instanceof Control c)
				c.setMaxWidth(Double.MAX_VALUE);
			addRow(row++, titleLabel(title, field.hint()), control);
		}
	}

	/** Appends a field, rebuilding the form like {@link #setFields}. */
	public void addField(FormField field) {
		List<FormField> newFields = new ArrayList<>(fields);
		newFields.add(field);
		setFields(newFields);
	}

	/** The right-aligned title label, with the field's hint as its tooltip. */
	private static Label titleLabel(String title, String hint) {
		Label label = new Label(title + ":");
		GridPane.setHalignment(label, HPos.RIGHT);
		installHint(hint, label);
		return label;
	}

	private static void installHint(String hint, Node... nodes) {
		if (hint == null || hint.isBlank())
			return;
		Tooltip tooltip = new Tooltip(hint);
		tooltip.setWrapText(true);
		tooltip.setMaxWidth(500);
		tooltip.setShowDelay(Duration.millis(200));
		tooltip.setShowDuration(Duration.INDEFINITE);
		for (Node node : nodes)
			Tooltip.install(node, tooltip);
	}

	/** INFO/HIDDEN: the model's value, else the default. */
	private String modelOrDefault(String type, FormField field) {
		if (model != null && model.containsKey(field.name()))
			return TemplateFormSupport.formValue(type, model.get(field.name()));
		return field.defaultValue();
	}

	public List<FormField> getFields() {
		return List.copyOf(fields);
	}

	/** Whether the form shows anything (fields other than HIDDEN). */
	public boolean hasVisibleFields() {
		return fields.stream().anyMatch(f -> !"HIDDEN".equals(f.normalizedType()));
	}

	/**
	 * The values by field name: HIDDEN = the model's value or its default
	 * ({@code null} when empty), INFO/COMMENT have none.
	 *
	 * @throws IOException a CSVFILE field's file can't be read
	 */
	public Map<String, Object> getValues() throws IOException {
		Map<String, Object> values = new LinkedHashMap<>();
		for (FormField field : fields) {
			String type = field.normalizedType();
			if ("HIDDEN".equals(type)) {
				String value = hiddenValues.get(field.name());
				values.put(field.name(), value == null || value.isEmpty() ? null : value);
				continue;
			}
			Node control = controls.get(field.name());
			if (control != null)
				values.put(field.name(), readValue(type, control));
		}
		return values;
	}

	/**
	 * Focuses the first editable control (a CSVFILE field's browse button, a
	 * DATETIME field's date).
	 */
	public void focusFirst() {
		if (controls.isEmpty())
			return;
		Node first = controls.values().iterator().next();
		if (first.getProperties().get(FOCUS_TARGET) instanceof Node target)
			target.requestFocus();
		else
			first.requestFocus();
	}

	/**
	 * The text a rebuilt field starts with: the previously entered one, else the
	 * model's value, else the field's default - the first usable
	 * ({@link TemplateFormSupport#initialValue}; COMBO/FONT only one of the
	 * options, when there are any). CSVFILE, INFO, ... don't keep an entered value.
	 */
	static String effectiveDefault(String type, String name, String fieldDefault, Map<String, ?> model, String previous,
			List<String> optionValues) {
		String entered = switch (type) {
		case "STRING", "MULTILINE", "BOOLEAN", "INT", "DECIMAL", "COMBO", "FONT", "DATE", "DATETIME" -> previous;
		default -> null;
		};
		return TemplateFormSupport.initialValue(type, name, fieldDefault, model, entered, optionValues);
	}

	/**
	 * INT {@code min:max:step} ({@code parameters}), missing parts = no limit / 1.
	 */
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

	/**
	 * Values the user entered (changed from what the field was built with) as text,
	 * for {@link #effectiveDefault} after a rebuild.
	 */
	private Map<String, String> enteredValues() {
		Map<String, String> entered = new HashMap<>();
		for (FormField field : fields) {
			Node control = controls.get(field.name());
			if (control == null)
				continue;
			String text = controlText(field.normalizedType(), control);
			if (text != null && !text.equals(initialTexts.get(field.name())))
				entered.put(field.name(), text);
		}
		return entered;
	}

	/** A control's value as text ({@code null} for a CSVFILE). */
	private static String controlText(String type, Node control) {
		switch (type) {
		case "CSVFILE":
			return null;
		case "DATE":
			return control instanceof DatePicker dp ? TemplateDateValues.formatDate(pickerValue(dp)) : null;
		case "DATETIME":
			return TemplateDateValues.formatDateTime(dateTimeValue(control));
		default:
			break;
		}
		return switch (control) {
		case Spinner<?> s -> s.getValue() != null ? s.getValue().toString() : null;
		case CheckBox cb -> Boolean.toString(cb.isSelected());
		case ComboBox<?> cb -> cb.getValue() != null ? cb.getValue().toString() : null;
		case TextArea ta -> ta.getText();
		case TextField tf -> tf.getText();
		default -> null;
		};
	}

	/**
	 * A date picker's date, including text typed into its editor but not committed
	 * yet (it commits on Enter / focus loss).
	 */
	static LocalDate pickerValue(DatePicker dp) {
		if (dp.getSkin() == null)
			// the skin fills the editor - until then it's empty
			return dp.getValue();
		String text = dp.getEditor().getText();
		if (text == null || text.isBlank())
			return null;
		try {
			LocalDate typed = dp.getConverter().fromString(text.strip());
			if (typed != null)
				return typed;
		} catch (RuntimeException e) {
			// not a date in the picker's format - the committed value
		}
		return dp.getValue();
	}

	/**
	 * A DATETIME control's value: {@code null} without a date, midnight without a
	 * (valid) time.
	 */
	private static LocalDateTime dateTimeValue(Node control) {
		if (!(control instanceof HBox box) || box.getChildren().size() < 2
				|| !(box.getChildren().get(0) instanceof DatePicker dp)
				|| !(box.getChildren().get(1) instanceof TextField time))
			return null;
		LocalDate date = pickerValue(dp);
		if (date == null)
			return null;
		LocalTime t = parseTime(time.getText());
		return date.atTime(t != null ? t : LocalTime.MIDNIGHT);
	}

	/** {@code H[:mm[:ss]]}; {@code null} when blank or invalid. */
	static LocalTime parseTime(String text) {
		if (text == null || text.isBlank())
			return null;
		String[] parts = text.strip().split(":");
		try {
			int h = Integer.parseInt(parts[0]);
			int m = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 0;
			int s = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) : 0;
			return LocalTime.of(h, m, s);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** {@code HH:mm}, {@code HH:mm:ss} when the seconds are not zero. */
	static String formatTime(LocalTime time) {
		return time.getSecond() != 0
				? String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond())
				: String.format("%02d:%02d", time.getHour(), time.getMinute());
	}

	/**
	 * A date picker with its initial date, offering only the days of the range
	 * (when given).
	 */
	private static DatePicker datePicker(LocalDate initial, String parameters) {
		DatePicker dp = new DatePicker(initial);
		LocalDateTime[] range = TemplateDateValues.parseRange(parameters);
		if (range != null) {
			LocalDate min = range[0] != null ? range[0].toLocalDate() : null;
			LocalDate max = range[1] != null ? range[1].toLocalDate() : null;
			dp.setDayCellFactory(p -> new DateCell() {
				@Override
				public void updateItem(LocalDate item, boolean empty) {
					super.updateItem(item, empty);
					if (item != null && ((min != null && item.isBefore(min)) || (max != null && item.isAfter(max))))
						setDisable(true);
				}
			});
		}
		dp.setMaxWidth(Double.MAX_VALUE);
		return dp;
	}

	private Node buildControl(String type, String initial, String parameters, List<Option> options) {
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
			Map<String, String> labels = new HashMap<>();
			for (Option o : options)
				labels.put(o.getValue(), o.getLabel());
			ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(Option.values(options)));
			cb.setConverter(new StringConverter<>() {
				@Override
				public String toString(String value) {
					return value == null ? "" : labels.getOrDefault(value, value);
				}

				@Override
				public String fromString(String text) {
					return text;
				}
			});
			if (initial != null && !initial.isEmpty())
				cb.setValue(initial);
			else if (!options.isEmpty())
				cb.getSelectionModel().selectFirst();
			yield cb;
		}
		case "DATE" -> datePicker(TemplateDateValues.tryParseDate(initial), parameters);
		case "DATETIME" -> {
			LocalDateTime value = TemplateDateValues.tryParseDateTime(initial);
			DatePicker dp = datePicker(value != null ? value.toLocalDate() : null, parameters);
			HBox.setHgrow(dp, Priority.ALWAYS);
			TextField time = new TextField(value != null ? formatTime(value.toLocalTime()) : "");
			time.setTextFormatter(
					new TextFormatter<String>(c -> TIME_INPUT.matcher(c.getControlNewText()).matches() ? c : null));
			time.setPromptText(BSAppJFXMessages.getString("parametricForm.time.prompt"));
			time.setPrefColumnCount(5);
			HBox box = new HBox(4, dp, time);
			box.setMaxWidth(Double.MAX_VALUE);
			box.getProperties().put(FOCUS_TARGET, dp);
			yield box;
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
			box.getProperties().put(FOCUS_TARGET, browse);
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
		case "DATE" -> control instanceof DatePicker dp ? pickerValue(dp) : null;
		case "DATETIME" -> dateTimeValue(control);
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
