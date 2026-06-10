package cz.bliksoft.javautils.fx.controls.images.cam;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

import javax.imageio.ImageIO;

/**
 * Captures still frames from a {@link NetworkCameraSource} via a plain HTTP
 * GET, decoding the response body as an image.
 *
 * <p>
 * Suitable for IP-camera apps that expose a single-image snapshot endpoint
 * (e.g. Android "IP Webcam"'s {@code /shot.jpg}).
 */
public final class NetworkCameraCapture {

	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int READ_TIMEOUT_MS = 5000;

	private NetworkCameraCapture() {
	}

	/**
	 * Fetches {@code url} and decodes the response as an image. Returns
	 * {@code null} if the response could not be decoded as an image.
	 */
	public static BufferedImage grabFrame(String url) throws IOException {
		URLConnection conn = URI.create(url).toURL().openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
		conn.setReadTimeout(READ_TIMEOUT_MS);
		try (InputStream in = conn.getInputStream()) {
			return ImageIO.read(in);
		}
	}
}
