package cz.bliksoft.javautils.fx.controls.forms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The FX-free parts of {@link ParametricFormPane} and {@link FormField}. */
class ParametricFormPaneTest {

	@Test
	void fieldsFromTemplate() {
		List<FormField> fields = FormField.fromTemplate(
				"<#--\n{var|decimal|width|Width (mm)|60,5}\n{var|COMBO|size|Size|M|S;M,L}\n-->\n");
		assertEquals(2, fields.size());
		assertEquals("DECIMAL", fields.get(0).normalizedType());
		assertEquals("60,5", fields.get(0).defaultValue());
		assertEquals(List.of("S", "M", "L"), fields.get(1).options());
		assertEquals("STRING", new FormField(null, "x", "X", null, null).normalizedType());
	}

	@Test
	void previousValueKeptWhenUsable() {
		assertEquals("a", ParametricFormPane.effectiveDefault("STRING", "d", "a", List.of()));
		assertEquals("d", ParametricFormPane.effectiveDefault("STRING", "d", null, List.of()));
		assertEquals("1,5", ParametricFormPane.effectiveDefault("DECIMAL", "2", "1,5", List.of()));
		assertEquals("M", ParametricFormPane.effectiveDefault("COMBO", "S", "M", List.of("S", "M")));
		assertEquals("S", ParametricFormPane.effectiveDefault("COMBO", "S", "X", List.of("S", "M")));
		assertEquals("X", ParametricFormPane.effectiveDefault("FONT", "S", "X", List.of()));
		assertEquals("d", ParametricFormPane.effectiveDefault("CSVFILE", "d", "a", List.of()));
	}

	@Test
	void intRange() {
		assertArrayEquals(new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE, 1 }, ParametricFormPane.intRange(null));
		assertArrayEquals(new int[] { 1, 10, 2 }, ParametricFormPane.intRange("1:10:2"));
		assertArrayEquals(new int[] { 0, Integer.MAX_VALUE, 1 }, ParametricFormPane.intRange("0::0"));
	}
}
