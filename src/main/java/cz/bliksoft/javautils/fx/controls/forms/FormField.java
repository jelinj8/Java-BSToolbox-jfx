package cz.bliksoft.javautils.fx.controls.forms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import cz.bliksoft.javautils.freemarker.utils.TemplateParameterUtils;
import cz.bliksoft.javautils.freemarker.utils.TemplateParameterUtils.TemplateParameter;

/**
 * One field of a {@link ParametricFormPane}.
 *
 * @param type         STRING, INT, DECIMAL, BOOLEAN, MULTILINE, COMBO, FONT,
 *                     CSVFILE, INFO, COMMENT or HIDDEN (case-insensitive; an
 *                     unknown type is a text field)
 * @param name         the key of the value
 * @param title        the label (COMMENT: the text shown)
 * @param defaultValue the initial value as text (INFO: the text shown, HIDDEN:
 *                     the value)
 * @param parameters   type-specific: COMBO/FONT options separated by {@code ;}
 *                     or {@code ,}; INT {@code min:max:step}
 */
public record FormField(String type, String name, String title, String defaultValue, String parameters) {

	/** The type upper-cased, {@code STRING} when absent. */
	public String normalizedType() {
		return type == null || type.isBlank() ? "STRING" : type.strip().toUpperCase(Locale.ROOT);
	}

	/** COMBO/FONT options from {@link #parameters()}; empty when none. */
	public List<String> options() {
		return parameters == null || parameters.isBlank() ? List.of()
				: new ArrayList<>(Arrays.asList(parameters.split("[;,]")));
	}

	/**
	 * The fields declared in a template's {@code {var|type|name|title|default|parameters}}
	 * comment lines ({@link TemplateParameterUtils}).
	 */
	public static List<FormField> fromTemplate(String templateSource) {
		List<FormField> fields = new ArrayList<>();
		for (TemplateParameter p : TemplateParameterUtils.parseParameters(templateSource))
			fields.add(new FormField(p.getType(), p.getName(), p.getTitle(), p.getDefaultValue(), p.getParameters()));
		return fields;
	}
}
