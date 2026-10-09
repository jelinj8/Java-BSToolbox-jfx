package cz.bliksoft.javautils.fx.controls.forms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** The FX-free parts of {@link ParametricFormPane} and {@link FormField}. */
class ParametricFormPaneTest {

	@Test
	void fieldsFromTemplate() {
		List<FormField> fields = FormField
				.fromTemplate("<#--\n{var|decimal|width|Width (mm)|60,5}\n{var|COMBO|size|Size|M|S;M;L}\n-->\n");
		assertEquals(2, fields.size());
		assertEquals("DECIMAL", fields.get(0).normalizedType());
		assertEquals("60,5", fields.get(0).defaultValue());
		assertEquals(List.of("S", "M", "L"), fields.get(1).options());
		assertEquals("STRING", new FormField(null, "x", "X", null, null).normalizedType());
	}

	@Test
	void hintsAndKeyedCommentsFromTemplate() {
		List<FormField> fields = FormField.fromTemplate("<#--\n{var|comment|-|test/formPane/missing|Fallback}\n"
				+ "{var|csvfile|list|List||:.csv}\n{var|hint|-|-|first}\n{var|hint|-|-|second}\n-->\n");
		assertEquals(2, fields.size());
		assertEquals("Fallback", fields.get(0).title());
		assertEquals("first\nsecond", fields.get(1).hint());
		assertEquals("h", new FormField("STRING", "x", "X", null, null).withHint("h").hint());
	}

	@Test
	void previousValueKeptWhenUsable() {
		assertEquals("a", ParametricFormPane.effectiveDefault("STRING", "x", "d", null, "a", List.of()));
		assertEquals("d", ParametricFormPane.effectiveDefault("STRING", "x", "d", null, null, List.of()));
		assertEquals("1,5", ParametricFormPane.effectiveDefault("DECIMAL", "x", "2", null, "1,5", List.of()));
		assertEquals("M", ParametricFormPane.effectiveDefault("COMBO", "x", "S", null, "M", List.of("S", "M")));
		assertEquals("S", ParametricFormPane.effectiveDefault("COMBO", "x", "S", null, "X", List.of("S", "M")));
		assertEquals("X", ParametricFormPane.effectiveDefault("FONT", "x", "S", null, "X", List.of()));
		// a CSVFILE has no text value
		assertNull(ParametricFormPane.effectiveDefault("CSVFILE", "x", "d", null, "a", List.of()));
		assertEquals("2026-01-02",
				ParametricFormPane.effectiveDefault("DATE", "x", "today", null, "2026-01-02", List.of()));
	}

	/**
	 * Rebuilds as a printer switch does them: what the user entered stays, however
	 * many times - not just over the first rebuild.
	 */
	@Test
	void enteredValueSurvivesRepeatedRebuilds() {
		Map<String, String> entered = new HashMap<>();
		// built with the defaults, the user types a text and picks a font
		ParametricFormPane.rememberEntered(entered, Map.of("txt", "Krabice 12", "font", "Comic"),
				Map.of("txt", "", "font", "Swiss"));
		assertEquals(Map.of("txt", "Krabice 12", "font", "Comic"), entered);
		// rebuilt with them, untouched, rebuilt again: still entered
		ParametricFormPane.rememberEntered(entered, Map.of("txt", "Krabice 12", "font", "Comic"),
				Map.of("txt", "Krabice 12", "font", "Comic"));
		assertEquals(Map.of("txt", "Krabice 12", "font", "Comic"), entered);
		// a new change replaces the remembered value - also back to the default
		ParametricFormPane.rememberEntered(entered, Map.of("txt", "", "font", "Comic"),
				Map.of("txt", "Krabice 12", "font", "Comic"));
		assertEquals(Map.of("txt", "", "font", "Comic"), entered);
	}

	/** A font one printer lacks: its default meanwhile, the font again on one that has it. */
	@Test
	void fontMissingOnAPrinterComesBack() {
		Map<String, String> entered = new HashMap<>(Map.of("font", "Comic"));
		// printer B has no Comic: built with B's default
		String onB = ParametricFormPane.effectiveDefault("FONT", "font", null, null, entered.get("font"),
				List.of("Swiss", "Arial"));
		assertEquals("Swiss", onB);
		// left as built on B - Comic is still what was entered
		ParametricFormPane.rememberEntered(entered, Map.of("font", onB), Map.of("font", onB));
		assertEquals("Comic", ParametricFormPane.effectiveDefault("FONT", "font", null, null, entered.get("font"),
				List.of("Swiss", "Comic")));
	}

	@Test
	void modelBetweenEnteredAndDefault() {
		Map<String, Object> model = new HashMap<>();
		model.put("n", 5);
		model.put("c", 2);
		model.put("d", LocalDate.of(2026, 3, 4));
		assertEquals("7", ParametricFormPane.effectiveDefault("INT", "n", "1", model, "7", List.of()));
		assertEquals("5", ParametricFormPane.effectiveDefault("INT", "n", "1", model, null, List.of()));
		assertEquals("2026-03-04", ParametricFormPane.effectiveDefault("DATE", "d", "today", model, null, List.of()));
		// a model value that is not an option - the default
		assertEquals("1", ParametricFormPane.effectiveDefault("COMBO", "c", "1", model, null, List.of("1", "3")));
		assertEquals("2", ParametricFormPane.effectiveDefault("COMBO", "c", "1", model, null, List.of("1", "2")));
		// INFO/CSVFILE never keep an entered value
		assertEquals("i", ParametricFormPane.effectiveDefault("INFO", "x", "i", null, "e", List.of()));
	}

	@Test
	void declaredOptions() {
		assertEquals(List.of("a", "b"), new FormField("COMBO", "c", "C", null, " a ; b ;").options());
		assertEquals(List.of("a", "b"), new FormField("COMBO", "c", "C", null, "a,b").options());
		assertEquals(List.of(), new FormField("COMBO", "c", "C", null, null).options());
	}

	@Test
	void times() {
		assertEquals(LocalTime.of(8, 5), ParametricFormPane.parseTime("8:05"));
		assertEquals(LocalTime.of(8, 0), ParametricFormPane.parseTime("8"));
		assertEquals(LocalTime.of(8, 5, 9), ParametricFormPane.parseTime("08:05:09"));
		assertNull(ParametricFormPane.parseTime("25:00"));
		assertNull(ParametricFormPane.parseTime(" "));
		assertEquals("08:05", ParametricFormPane.formatTime(LocalTime.of(8, 5)));
		assertEquals("08:05:09", ParametricFormPane.formatTime(LocalTime.of(8, 5, 9)));
	}

	@Test
	void intRange() {
		assertArrayEquals(new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE, 1 }, ParametricFormPane.intRange(null));
		assertArrayEquals(new int[] { 1, 10, 2 }, ParametricFormPane.intRange("1:10:2"));
		assertArrayEquals(new int[] { 0, Integer.MAX_VALUE, 1 }, ParametricFormPane.intRange("0::0"));
	}
}
