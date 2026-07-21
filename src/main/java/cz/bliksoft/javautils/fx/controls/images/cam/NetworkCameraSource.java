package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;

/**
 * {@link ICameraSource} backed by an HTTP snapshot endpoint (e.g. a phone
 * running an IP-camera app), capturing frames via {@link NetworkCameraCapture}.
 *
 * <p>
 * Registered declaratively via the {@code /services} XmlFilesystem registry
 * (see {@code cz.bliksoft.javautils.xmlfilesystem.singletons.Services}), looked
 * up with {@code Services.getServices(ICameraSource.class)} from
 * {@code CameraCapturePane}. Example registration in the application's local
 * configuration:
 *
 * <pre>{@code
 * <file name="services">
 *     <file name="warehouse-phone" type="Class">
 *         <attribute name="class" value=
"cz.bliksoft.javautils.fx.controls.images.cam.NetworkCameraSource"/>
 *         <attribute name="name" value="Warehouse phone"/>
 *         <attribute name="url" value="http://192.168.1.50:8080/shot.jpg"/>
 *     </file>
 * </file>
 * }</pre>
 */
public final class NetworkCameraSource implements ICameraSource {

	private static final Logger log = LogManager.getLogger(NetworkCameraSource.class);

	/** Poll interval between live-preview frames. */
	private static final int PREVIEW_POLL_MS = 200;

	/** Prefix for {@link #getId()}, used to recognize network camera ids. */
	public static final String ID_PREFIX = "network:";

	private final String id;
	private final String displayName;
	private final String url;

	public NetworkCameraSource(FileObject fo) {
		this.id = ID_PREFIX + fo.getName();
		this.displayName = fo.getLocalizedAttribute("name", fo.getName());
		this.url = fo.getAttribute("url", null);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	/** The HTTP(S) snapshot URL to fetch a frame from. */
	public String getUrl() {
		return url;
	}

	@Override
	public BufferedImage grabFrame(Dimension resolution) throws IOException {
		return NetworkCameraCapture.grabFrame(url);
	}

	@Override
	public ICameraPreviewSession openPreview(Dimension resolution) {
		return new PreviewSession(url);
	}

	private record PreviewSession(String url) implements ICameraPreviewSession {
		@Override
		public BufferedImage readFrame() {
			BufferedImage frame;
			try {
				frame = NetworkCameraCapture.grabFrame(url);
			} catch (Exception ex) {
				log.debug("Network camera preview frame failed for {}", url, ex);
				frame = null;
			}
			try {
				Thread.sleep(PREVIEW_POLL_MS);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
			return frame;
		}

		@Override
		public void close() {
		}
	}
}
