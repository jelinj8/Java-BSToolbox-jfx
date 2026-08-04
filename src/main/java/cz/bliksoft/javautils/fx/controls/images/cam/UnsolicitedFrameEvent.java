package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;

import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ILevelEvent;
import cz.bliksoft.javautils.context.events.IConsumableEvent;
import javafx.application.Platform;

/**
 * Fired by {@link WebServerCameraSource} when a frame is pushed to its upload
 * endpoint while nobody in the app is currently capturing from it - i.e. no
 * live {@link ICameraPreviewSession} is open and no
 * {@link ICameraSource#grabFrame} / {@link ICameraSource#grabFrameBytes} call
 * is in flight for that source.
 *
 * <p>
 * Modeled on {@code BarcodeScannedEvent} in StorageManagerDesktopClient2: a
 * consumer (e.g. the currently-open storage object editor) can listen for this
 * event to propose creating an attachment from the pushed image.
 */
public class UnsolicitedFrameEvent implements IConsumableEvent, ILevelEvent {

	private boolean consumed = false;

	@Override
	public void consume() {
		consumed = true;
	}

	@Override
	public boolean isConsumed() {
		return consumed;
	}

	private final String sourceId;
	private final String sourceName;
	private final BufferedImage image;
	private final byte[] imageBytes;
	private final boolean autocrop;
	private final LocalDateTime received;

	public UnsolicitedFrameEvent(String sourceId, String sourceName, BufferedImage image, byte[] imageBytes,
			boolean autocrop) {
		this.sourceId = sourceId;
		this.sourceName = sourceName;
		this.image = image;
		this.imageBytes = imageBytes;
		this.autocrop = autocrop;
		this.received = LocalDateTime.now();
	}

	public String getSourceId() {
		return sourceId;
	}

	public String getSourceName() {
		return sourceName;
	}

	public BufferedImage getImage() {
		return image;
	}

	public byte[] getImageBytes() {
		return imageBytes;
	}

	public boolean isAutocrop() {
		return autocrop;
	}

	public LocalDateTime getReceived() {
		return received;
	}

	@Override
	public String toString() {
		return "UnsolicitedFrame[" + sourceId + "]@" + received;
	}

	/**
	 * Builds the event and dispatches it on the FX application thread, since
	 * callers (e.g. {@link WebServerCameraSource}'s HTTP upload handler) typically
	 * run on a background thread and {@link Context#fireGUIEvent} enforces the EDT.
	 */
	public static void fire(String sourceId, String sourceName, BufferedImage image, byte[] imageBytes,
			boolean autocrop) {
		UnsolicitedFrameEvent evt = new UnsolicitedFrameEvent(sourceId, sourceName, image, imageBytes, autocrop);
		Platform.runLater(() -> Context.getCurrentContext().fireGUIEvent(evt));
	}
}
