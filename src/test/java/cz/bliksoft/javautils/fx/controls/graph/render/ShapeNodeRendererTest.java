package cz.bliksoft.javautils.fx.controls.graph.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShapeNodeRendererTest {

	@Test
	void resolveIconSpecSubstitutesNodeSize() {
		assertEquals("icon.svg|120|60",
				ShapeNodeRenderer.resolveIconSpec("icon.svg|${nodeWidth}|${nodeHeight}", 120, 60));
	}

	@Test
	void resolveIconSpecRoundsToPixels() {
		assertEquals("icon.svg|120|60",
				ShapeNodeRenderer.resolveIconSpec("icon.svg|${nodeWidth}|${nodeHeight}", 119.6, 60.4));
	}

	@Test
	void resolveIconSpecLeavesPlainSpecUnchanged() {
		assertEquals("icon.svg", ShapeNodeRenderer.resolveIconSpec("icon.svg", 120, 60));
	}
}
