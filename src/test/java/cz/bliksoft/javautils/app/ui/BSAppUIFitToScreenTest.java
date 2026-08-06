package cz.bliksoft.javautils.app.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link BSAppUI#computeFitBounds} treats the window's min-width/
 * min-height as a floor, not a target: when a saved/oversized window is reduced
 * to fit a screen, the result should be the screen-constrained size whenever
 * that's still above the minimum — only falling back to the minimum when the
 * screen itself is smaller than the window's own minimum content size, which is
 * unavoidable without clipping content.
 */
class BSAppUIFitToScreenTest {

	private static final Rectangle2D SCREEN = new Rectangle2D(0, 0, 1280, 720);

	@Test
	void shrinksToScreenSizeWhenRoomyEnoughAboveMinimum() {
		// Saved on a bigger monitor (1920x1200); min content size (300x200) is well
		// below the 1280x720 screen, so the result should be the screen size, not
		// the minimum.
		Rectangle2D fit = BSAppUI.computeFitBounds(100, 100, 1920, 1200, 300, 200, SCREEN);

		assertEquals(1280, fit.getWidth());
		assertEquals(720, fit.getHeight());
	}

	@Test
	void leavesWindowUntouchedWhenAlreadySmallerThanScreen() {
		Rectangle2D fit = BSAppUI.computeFitBounds(50, 50, 800, 600, 300, 200, SCREEN);

		assertEquals(800, fit.getWidth());
		assertEquals(600, fit.getHeight());
		assertEquals(50, fit.getMinX());
		assertEquals(50, fit.getMinY());
	}

	@Test
	void fallsBackToMinimumOnlyWhenScreenIsSmallerThanTheMinimumItself() {
		// A ui.scale-inflated minimum (1400x800) that itself exceeds the screen
		// (1280x720) — the window can't shrink below its own minimum, so the
		// result is necessarily still bigger than the screen. This is the one
		// case where the result legitimately equals the minimum.
		Rectangle2D fit = BSAppUI.computeFitBounds(100, 100, 1200, 800, 1400, 800, SCREEN);

		assertEquals(1400, fit.getWidth());
		assertEquals(800, fit.getHeight());
	}

	@Test
	void positionsWindowFullyWithinScreenAfterShrinking() {
		// Window near the right/bottom edge before shrinking; after reducing width
		// and height to fit, the position must be pulled back so the whole window
		// stays on screen, not left where the (now smaller) window would spill off
		// the edge.
		Rectangle2D fit = BSAppUI.computeFitBounds(1200, 700, 1920, 1200, 300, 200, SCREEN);

		assertEquals(0, fit.getMinX());
		assertEquals(0, fit.getMinY());
		assertEquals(1280, fit.getWidth());
		assertEquals(720, fit.getHeight());
	}

	@Test
	void onlyRepositionsWhenSizeAlreadyFitsButOverlapsScreenEdge() {
		// 800x600 already fits within the 1280x720 screen — it's only positioned
		// too far right (x=1250, so x+w=2050 spills off the right edge). Only X
		// should move (pulled back so the window is flush with the right edge);
		// width/height and Y (already fine) must stay untouched.
		Rectangle2D fit = BSAppUI.computeFitBounds(1250, 100, 800, 600, 300, 200, SCREEN);

		assertEquals(800, fit.getWidth());
		assertEquals(600, fit.getHeight());
		assertEquals(480, fit.getMinX()); // 1280 - 800
		assertEquals(100, fit.getMinY()); // unchanged — already within bounds
	}
}
