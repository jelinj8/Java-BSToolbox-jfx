package cz.bliksoft.javautils.fx.controls.editors.iconspec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import cz.bliksoft.javautils.fx.controls.codebooks.providers.basic.IconCodebookPopupProviderAccess;

/**
 * Which parameters the composer offers per image kind - a raster image (JPG
 * too) takes width/height/scale, not just SVG.
 */
class IconspecComposerParamsTest {

	@Test
	void paramCountPerKind() {
		assertEquals(5, IconspecComposer.paramCount("svg/file.svg"));
		assertEquals(3, IconspecComposer.paramCount("EMPTY"));
		assertEquals(3, IconspecComposer.paramCount("[F]:C:/photos/Photo.JPG"));
		assertEquals(3, IconspecComposer.paramCount("logo.png"));
		assertEquals(2, IconspecComposer.paramCount("app.ico"));
		assertEquals(0, IconspecComposer.paramCount("QR"));
	}

	@Test
	void jpgIsAnIconFile() {
		assertTrue(IconCodebookPopupProviderAccess.isIconFile(new File("photo.jpeg")));
		assertTrue(IconCodebookPopupProviderAccess.isIconFile(new File("PHOTO.JPG")));
		assertFalse(IconCodebookPopupProviderAccess.isIconFile(new File("notes.txt")));
	}

	/** Paste: a whole iconspec becomes its steps. */
	@Test
	void splitSteps() {
		assertEquals(java.util.List.of("base.svg|24", "badge.svg|12", "*+"),
				IconspecComposer.splitSteps("base.svg|24#badge.svg|12##*+"));
		assertEquals(java.util.List.of(), IconspecComposer.splitSteps(" "));
	}
}
