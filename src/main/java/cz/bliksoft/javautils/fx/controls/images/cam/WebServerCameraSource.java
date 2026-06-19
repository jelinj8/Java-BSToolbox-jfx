package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.net.http.BSHttpServer;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;

/**
 * {@link ICameraSource} that receives frames pushed via HTTP POST to an
 * embedded {@link BSHttpServer}, instead of polling a device.
 *
 * <p>
 * Because the upload listener must be ready to accept the very first handsfree
 * capture, this class is registered eagerly via the {@code /services}
 * XmlFilesystem registry ({@code type="Class"}, instantiated at application
 * startup - see {@code Services.loadServices()}), rather than the
 * lazily-instantiated {@code /singletons} registry used by
 * {@link FolderMonitorCameraSource} and {@link NetworkCameraSource}. It
 * self-registers as an {@link ICameraSource} via {@code Services} (looked up
 * with {@code Services.getServices(ICameraSource.class)} from
 * {@code CameraCapturePane}).
 *
 * <p>
 * It reuses a shared {@link BSHttpServer} singleton, registered separately
 * under {@code /singletons} (looked up via
 * {@link BSHttpServer#getSingleton()}):
 *
 * <pre>{@code
 * <file name="singletons">
 *     <file name="webserver" type=
"cz.bliksoft.javautils.net.http.BSHttpServer">
 *         <attribute name="port" value="8090"/>
 *     </file>
 * </file>
 * <file name="services">
 *     <file name="phone-pusher" type="Class">
 *         <attribute name="class" value=
"cz.bliksoft.javautils.fx.controls.images.cam.WebServerCameraSource"/>
 *         <attribute name="name" value="Phone pusher"/>
 *         <attribute name="path" value="/upload"/>
 *         <attribute name="comment" value=
"Use the back camera and hold the phone steady."/>
 *     </file>
 * </file>
 * }</pre>
 *
 * <p>
 * If no {@link BSHttpServer} singleton is registered, a dedicated server is
 * started instead, using the mandatory {@code port} attribute (legacy fallback,
 * port-per-source).
 *
 * <p>
 * A remote device POSTs a JPEG/PNG image to {@code http://<host>:<port><path>};
 * {@link #grabFrame(Dimension)} and {@link #openPreview(Dimension)} serve the
 * most recently received frame. A {@code GET} on the same URL serves a small
 * mobile-friendly HTML page with a file picker (camera capture on phones) that
 * uploads via {@code multipart/form-data}. The optional {@code comment}
 * attribute (localizable) is shown on that page between the title and the file
 * picker, e.g. to give the person taking the photo some instructions.
 */
public final class WebServerCameraSource implements ICameraSource, Closeable {

	private static final Logger log = LogManager.getLogger(WebServerCameraSource.class);

	/** Default URL path the remote device POSTs frames to. */
	private static final String DEFAULT_PATH = "/upload";

	/** Timeout for {@link #grabFrame(Dimension)} waiting for the next frame. */
	private static final long GRAB_TIMEOUT_MS = 10_000;

	/**
	 * Timeout for a single preview {@link ICameraPreviewSession#readFrame()} wait.
	 */
	private static final long PREVIEW_POLL_TIMEOUT_MS = 1_000;

	private final String id;
	private final String displayName;
	private final String comment;
	private final String path;

	private final BSHttpServer server;
	private final boolean ownsServer;
	private final String mdnsName;
	private final Object mdnsInfo;

	private final Object lock = new Object();
	private BufferedImage latestFrame;
	private byte[] latestFrameBytes;
	private int latestFrameOrientation;
	private boolean latestFrameAutocrop;
	private long sequence = 0;

	public WebServerCameraSource(FileObject fo) {
		this.id = "webserver-" + fo.getName();
		this.displayName = fo.getLocalizedAttribute("name", fo.getName());
		this.comment = fo.getLocalizedAttribute("comment", null);
		this.path = fo.getAttribute("path", DEFAULT_PATH);

		BSHttpServer shared = BSHttpServer.getSingleton();
		if (shared != null) {
			this.server = shared;
			this.ownsServer = false;
			server.addHandler(path, new UploadHandler());
		} else {
			int port = Integer.parseInt(fo.getAttribute("port", null));

			this.server = new BSHttpServer(port);
			this.ownsServer = true;
			server.addHandler(path, new UploadHandler());
			try {
				server.start();
			} catch (IOException e) {
				throw new RuntimeException("Failed to start WebServerCameraSource HTTP server on port " + port, e);
			}
		}

		this.mdnsName = fo.getAttribute("mdnsName", null);
		this.mdnsInfo = mdnsName != null ? server.registerMdnsService(mdnsName, path) : null;
	}

	@Override
	public void close() {
		if (mdnsInfo != null)
			server.unregisterMdnsService(mdnsInfo);
		server.removeHandler(path);
		if (!ownsServer)
			return;
		try {
			server.stop();
		} catch (Exception e) {
			log.warn("Failed to stop WebServerCameraSource HTTP server for {}", id, e);
		}
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getStatusInfo() {
		try {
			int port = server.getServer().getAddress().getPort();
			String ipUrl = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port + path;
			if (mdnsName != null)
				return "http://" + mdnsName + ".local:" + port + path + "  |  " + ipUrl;
			return ipUrl;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String getQrUrl() {
		try {
			int port = server.getServer().getAddress().getPort();
			if (mdnsName != null)
				return "http://" + mdnsName + ".local:" + port + path;
			return "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port + path;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public BufferedImage grabFrame(Dimension resolution) throws InterruptedException {
		synchronized (lock) {
			if (sequence > 0)
				return latestFrame;

			long deadline = System.currentTimeMillis() + GRAB_TIMEOUT_MS;
			while (sequence == 0) {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0)
					return latestFrame;
				lock.wait(remaining);
			}
			return latestFrame;
		}
	}

	@Override
	public ICameraPreviewSession openPreview(Dimension resolution) {
		return new PreviewSession();
	}

	@Override
	public byte[] grabFrameBytes(Dimension resolution) throws InterruptedException {
		synchronized (lock) {
			if (sequence == 0) {
				long deadline = System.currentTimeMillis() + GRAB_TIMEOUT_MS;
				while (sequence == 0) {
					long remaining = deadline - System.currentTimeMillis();
					if (remaining <= 0)
						break;
					lock.wait(remaining);
				}
			}
			return latestFrameOrientation <= 1 ? latestFrameBytes : null;
		}
	}

	@Override
	public Map<String, Object> grabFrameMetadata(Dimension resolution) {
		synchronized (lock) {
			return sequence == 0 ? Map.of() : Map.of(METADATA_AUTOCROP, latestFrameAutocrop);
		}
	}

	/**
	 * Decodes an image and applies its EXIF {@code Orientation} tag (if any), so
	 * photos taken in portrait/landscape on a phone come out the right way up
	 * regardless of how the sensor stored them.
	 */
	private static BufferedImage decodeImage(byte[] imageBytes) throws IOException {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
		if (image == null)
			return null;

		int orientation = readExifOrientation(imageBytes);
		return orientation <= 1 ? image : applyExifOrientation(image, orientation);
	}

	/**
	 * Reads the EXIF {@code Orientation} tag (1-8) from a JPEG, or 1 if absent/not
	 * a JPEG.
	 */
	private static int readExifOrientation(byte[] data) {
		if (data.length < 4 || (data[0] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xD8)
			return 1;

		int i = 2;
		while (i + 4 <= data.length) {
			if ((data[i] & 0xFF) != 0xFF)
				break;
			int marker = data[i + 1] & 0xFF;
			if (marker == 0xD8 || marker == 0xD9 || marker == 0xDA)
				break;
			int length = ((data[i + 2] & 0xFF) << 8) | (data[i + 3] & 0xFF);
			if (marker == 0xE1 && i + 4 + 6 <= data.length
					&& new String(data, i + 4, 6, StandardCharsets.US_ASCII).equals("Exif\0\0")) {
				return parseTiffOrientation(data, i + 4 + 6);
			}
			i += 2 + length;
		}
		return 1;
	}

	private static int parseTiffOrientation(byte[] data, int tiffStart) {
		if (tiffStart + 8 > data.length)
			return 1;

		boolean little = data[tiffStart] == 'I';
		int ifd0Offset = readInt32(data, tiffStart + 4, little);
		int entryCountOffset = tiffStart + ifd0Offset;
		if (entryCountOffset + 2 > data.length)
			return 1;

		int entryCount = readInt16(data, entryCountOffset, little);
		for (int e = 0; e < entryCount; e++) {
			int entryOffset = entryCountOffset + 2 + e * 12;
			if (entryOffset + 12 > data.length)
				break;
			int tag = readInt16(data, entryOffset, little);
			if (tag == 0x0112)
				return readInt16(data, entryOffset + 8, little);
		}
		return 1;
	}

	private static int readInt16(byte[] data, int offset, boolean little) {
		int b0 = data[offset] & 0xFF;
		int b1 = data[offset + 1] & 0xFF;
		return little ? (b1 << 8) | b0 : (b0 << 8) | b1;
	}

	private static int readInt32(byte[] data, int offset, boolean little) {
		int b0 = data[offset] & 0xFF;
		int b1 = data[offset + 1] & 0xFF;
		int b2 = data[offset + 2] & 0xFF;
		int b3 = data[offset + 3] & 0xFF;
		return little ? (b3 << 24) | (b2 << 16) | (b1 << 8) | b0 : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
	}

	/**
	 * Applies an EXIF orientation (2-8) to {@code image}, returning a new,
	 * correctly oriented image.
	 */
	private static BufferedImage applyExifOrientation(BufferedImage image, int orientation) {
		int width = image.getWidth();
		int height = image.getHeight();
		AffineTransform t = new AffineTransform();

		switch (orientation) {
		case 2: // flip horizontal
			t.scale(-1.0, 1.0);
			t.translate(-width, 0);
			break;
		case 3: // rotate 180
			t.translate(width, height);
			t.rotate(Math.PI);
			break;
		case 4: // flip vertical
			t.scale(1.0, -1.0);
			t.translate(0, -height);
			break;
		case 5: // transpose (rotate 90 CW + flip horizontal)
			t.rotate(-Math.PI / 2);
			t.scale(-1.0, 1.0);
			break;
		case 6: // rotate 90 CW
			t.translate(height, 0);
			t.rotate(Math.PI / 2);
			break;
		case 7: // transverse (rotate 270 CW + flip horizontal)
			t.scale(-1.0, 1.0);
			t.translate(-height, 0);
			t.translate(0, width);
			t.rotate(3 * Math.PI / 2);
			break;
		case 8: // rotate 270 CW
			t.translate(0, width);
			t.rotate(3 * Math.PI / 2);
			break;
		default:
			return image;
		}

		AffineTransformOp op = new AffineTransformOp(t, AffineTransformOp.TYPE_BILINEAR);
		BufferedImage destination = op.createCompatibleDestImage(image,
				image.getType() == BufferedImage.TYPE_BYTE_GRAY ? image.getColorModel() : null);
		return op.filter(image, destination);
	}

	/**
	 * Escapes {@code &}, {@code <}, {@code >} and {@code "} for safe inclusion in
	 * HTML.
	 */
	private static String escapeHtml(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private final class UploadHandler implements HttpHandler {

		private static final byte[] CRLFCRLF = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			try {
				String method = exchange.getRequestMethod();
				if ("GET".equalsIgnoreCase(method)) {
					serveForm(exchange);
					return;
				}
				if (!"POST".equalsIgnoreCase(method)) {
					sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
					return;
				}

				byte[] body = exchange.getRequestBody().readAllBytes();
				String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
				boolean isFormUpload = contentType != null
						&& contentType.toLowerCase().startsWith("multipart/form-data");

				byte[] imageBytes = isFormUpload ? extractFilePart(body, contentType) : body;
				BufferedImage frame = imageBytes == null ? null : decodeImage(imageBytes);

				if (frame == null) {
					if (isFormUpload)
						redirect(exchange, path + "?err=1");
					else
						sendResponse(exchange, 400, "text/plain", "Bad Request");
					return;
				}

				int orientation = readExifOrientation(imageBytes);
				boolean autocrop = isFormUpload && extractFieldValue(body, contentType, "autocrop") != null;

				synchronized (lock) {
					latestFrame = frame;
					latestFrameBytes = imageBytes;
					latestFrameOrientation = orientation;
					latestFrameAutocrop = autocrop;
					sequence++;
					lock.notifyAll();
				}

				if (isFormUpload)
					redirect(exchange, path + "?ok=1");
				else
					sendResponse(exchange, 200, "text/plain", "OK");
			} catch (Exception e) {
				log.warn("Failed to handle camera frame upload", e);
				sendResponse(exchange, 400, "text/plain", "Bad Request");
			}
		}

		private void serveForm(HttpExchange exchange) throws IOException {
			String query = exchange.getRequestURI().getQuery();
			String status = "";
			if (query != null) {
				if (query.contains("ok=1"))
					status = "<p style=\"color:green\">"
							+ escapeHtml(BSAppMessages.getString("WebServerCameraSource.form.success")) + "</p>";
				else if (query.contains("err=1"))
					status = "<p style=\"color:red\">"
							+ escapeHtml(BSAppMessages.getString("WebServerCameraSource.form.error")) + "</p>";
			}

			String subtitle = comment != null && !comment.isBlank() ? "<p>" + escapeHtml(comment) + "</p>" : "";

			String html = "<!DOCTYPE html>\n" //
					+ "<html><head><meta charset=\"utf-8\">" //
					+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" //
					+ "<title>" + escapeHtml(displayName) + "</title></head>" //
					+ "<body style=\"font-family:sans-serif;text-align:center;padding:2em;\">" //
					+ "<h1>" + escapeHtml(displayName) + "</h1>" //
					+ subtitle //
					+ "<form method=\"POST\" enctype=\"multipart/form-data\">" //
					+ "<input type=\"file\" name=\"frame\" accept=\"image/*\" capture=\"environment\"><br><br>" //
					+ "<label><input type=\"checkbox\" name=\"autocrop\"> "
					+ escapeHtml(BSAppMessages.getString("WebServerCameraSource.form.autocropLabel"))
					+ "</label><br><br>" //
					+ "<input type=\"submit\" value=\""
					+ escapeHtml(BSAppMessages.getString("WebServerCameraSource.form.uploadButton")) + "\">" //
					+ "</form>" + status //
					+ "</body></html>";

			sendResponse(exchange, 200, "text/html; charset=utf-8", html);
		}

		/**
		 * Extracts the bytes of the first {@code multipart/form-data} part that has a
		 * non-empty {@code filename}, or {@code null} if none is found.
		 */
		private byte[] extractFilePart(byte[] body, String contentType) {
			String boundary = null;
			for (String part : contentType.split(";")) {
				part = part.trim();
				if (part.startsWith("boundary="))
					boundary = part.substring("boundary=".length()).replaceAll("^\"|\"$", "");
			}
			if (boundary == null)
				return null;

			byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

			int pos = indexOf(body, delimiter, 0);
			while (pos >= 0) {
				int partStart = pos + delimiter.length;
				int nextDelimiter = indexOf(body, delimiter, partStart);
				if (nextDelimiter < 0)
					break;

				int headerEnd = indexOf(body, CRLFCRLF, partStart);
				if (headerEnd < 0 || headerEnd > nextDelimiter) {
					pos = nextDelimiter;
					continue;
				}

				String headers = new String(body, partStart, headerEnd - partStart, StandardCharsets.ISO_8859_1);
				if (headers.contains("filename=\"") && !headers.contains("filename=\"\"")) {
					int bodyStart = headerEnd + CRLFCRLF.length;
					int bodyEnd = nextDelimiter;
					if (bodyEnd >= bodyStart + 2 && body[bodyEnd - 1] == '\n' && body[bodyEnd - 2] == '\r')
						bodyEnd -= 2;
					if (bodyEnd > bodyStart)
						return Arrays.copyOfRange(body, bodyStart, bodyEnd);
				}
				pos = nextDelimiter;
			}
			return null;
		}

		/**
		 * Extracts the decoded text value of the {@code multipart/form-data} part named
		 * {@code fieldName} (a plain field, i.e. without {@code filename=}), or
		 * {@code null} if no such part is present.
		 */
		private String extractFieldValue(byte[] body, String contentType, String fieldName) {
			String boundary = null;
			for (String part : contentType.split(";")) {
				part = part.trim();
				if (part.startsWith("boundary="))
					boundary = part.substring("boundary=".length()).replaceAll("^\"|\"$", "");
			}
			if (boundary == null)
				return null;

			byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
			String nameMarker = "name=\"" + fieldName + "\"";

			int pos = indexOf(body, delimiter, 0);
			while (pos >= 0) {
				int partStart = pos + delimiter.length;
				int nextDelimiter = indexOf(body, delimiter, partStart);
				if (nextDelimiter < 0)
					break;

				int headerEnd = indexOf(body, CRLFCRLF, partStart);
				if (headerEnd < 0 || headerEnd > nextDelimiter) {
					pos = nextDelimiter;
					continue;
				}

				String headers = new String(body, partStart, headerEnd - partStart, StandardCharsets.ISO_8859_1);
				if (headers.contains(nameMarker) && !headers.contains("filename=")) {
					int bodyStart = headerEnd + CRLFCRLF.length;
					int bodyEnd = nextDelimiter;
					if (bodyEnd >= bodyStart + 2 && body[bodyEnd - 1] == '\n' && body[bodyEnd - 2] == '\r')
						bodyEnd -= 2;
					if (bodyEnd >= bodyStart)
						return new String(body, bodyStart, bodyEnd - bodyStart, StandardCharsets.UTF_8);
				}
				pos = nextDelimiter;
			}
			return null;
		}

		private int indexOf(byte[] data, byte[] pattern, int from) {
			outer: for (int i = from; i <= data.length - pattern.length; i++) {
				for (int j = 0; j < pattern.length; j++) {
					if (data[i + j] != pattern[j])
						continue outer;
				}
				return i;
			}
			return -1;
		}

		private void redirect(HttpExchange exchange, String location) throws IOException {
			exchange.getResponseHeaders().add("Location", location);
			exchange.sendResponseHeaders(303, -1);
			exchange.close();
		}

		private void sendResponse(HttpExchange exchange, int status, String contentType, String body)
				throws IOException {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", contentType);
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		}
	}

	private final class PreviewSession implements ICameraPreviewSession {

		private long lastSeenSequence = -1;

		@Override
		public BufferedImage readFrame() throws InterruptedException {
			synchronized (lock) {
				if (lastSeenSequence < 0)
					lastSeenSequence = sequence;

				if (sequence == lastSeenSequence) {
					lock.wait(PREVIEW_POLL_TIMEOUT_MS);
					if (sequence == lastSeenSequence)
						return null;
				}

				lastSeenSequence = sequence;
				return latestFrame;
			}
		}

		@Override
		public void close() {
		}
	}
}
