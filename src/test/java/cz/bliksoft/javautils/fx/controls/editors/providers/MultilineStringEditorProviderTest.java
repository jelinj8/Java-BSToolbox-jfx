package cz.bliksoft.javautils.fx.controls.editors.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MultilineStringEditorProviderTest {

	@Test
	void escapesLineBreaksTabsAndBackslashes() {
		assertEquals("line 1\\nline 2\\tC:\\\\temp", MultilineStringEditorProvider.escape("line 1\nline 2\tC:\\temp"));
	}

	@Test
	void normalizesCrLfToASingleEscape() {
		assertEquals("a\\nb\\nc", MultilineStringEditorProvider.escape("a\r\nb\rc"));
	}

	@Test
	void roundTrips() {
		String text = "first \"quoted\"\n\tsecond \\ third\n";
		assertEquals(text, MultilineStringEditorProvider.unescape(MultilineStringEditorProvider.escape(text)));
	}

	@Test
	void keepsUnknownEscapesAndTrailingBackslashLiterally() {
		assertEquals("a\\xb\\", MultilineStringEditorProvider.unescape("a\\xb\\"));
	}
}
