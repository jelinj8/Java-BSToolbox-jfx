package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.image.BufferedImage;

/**
 * An open live-preview session for an {@link ICameraSource}, returned by
 * {@link ICameraSource#openPreview(java.awt.Dimension)}.
 *
 * <p>
 * {@link #readFrame()} is called repeatedly from a background thread until the
 * preview is stopped, at which point {@link #close()} releases the underlying
 * device or connection. Implementations are responsible for their own
 * frame-rate pacing (e.g. sleeping between frames).
 */
public interface ICameraPreviewSession extends AutoCloseable {

	/**
	 * Reads the next preview frame, or {@code null} if none is available this round
	 * (the caller will simply try again).
	 */
	BufferedImage readFrame() throws Exception;

	/** Releases the underlying device or connection. Never throws. */
	@Override
	void close();
}
