package cz.bliksoft.javautils.fx.controls.images;

import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;

public class ImageCropPane extends StackPane {

	private final ImageView imageView = new ImageView();
	private final Pane overlay = new Pane();

	private final Rectangle topShade = shadeRect();
	private final Rectangle leftShade = shadeRect();
	private final Rectangle rightShade = shadeRect();
	private final Rectangle bottomShade = shadeRect();

	private final Rectangle selection = new Rectangle();

	private double startX, startY;
	private double endX, endY;

	private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
	private final ReadOnlyObjectWrapper<javafx.geometry.Rectangle2D> cropRectInImagePixels = new ReadOnlyObjectWrapper<>();

	private boolean selectionNormalizedValid = false;
	private double rx, ry, rw, rh; // relative to displayed image bounds (0..1)

	/**
	 * Optional limit for returned cropped image: 0 (default) = no limit. If &gt; 0,
	 * the returned image will be downscaled so that
	 * {@code max(width, height) <= limit}.
	 */
	private final IntegerProperty maxOutputPixels = new SimpleIntegerProperty(this, "maxOutputPixels", 0);

	public ImageCropPane() {
		setMinSize(0, 0);
		getStyleClass().add("image-crop-pane");
		setBackground(new Background(new BackgroundFill(Color.web("#2b2b2b"), null, null)));

		imageView.setPreserveRatio(true);
		imageView.setSmooth(true);
		imageView.fitWidthProperty().bind(widthProperty());
		imageView.fitHeightProperty().bind(heightProperty());

		StackPane.setAlignment(overlay, Pos.CENTER);
		overlay.setPickOnBounds(true);
		overlay.setMinSize(0, 0);

		selection.setFill(Color.TRANSPARENT);
		selection.setStroke(Color.WHITE);
		selection.setStrokeWidth(2);
		selection.getStrokeDashArray().setAll(6.0, 4.0);

		overlay.getChildren().addAll(topShade, leftShade, rightShade, bottomShade, selection);
		getChildren().addAll(imageView, overlay);

		imageView.boundsInParentProperty().addListener((obs, oldB, b) -> updateShades());
		widthProperty().addListener((obs, o, n) -> updateShades());
		heightProperty().addListener((obs, o, n) -> updateShades());

		imageView.boundsInParentProperty().addListener((obs, oldB, b) -> {
			applySelectionFromNormalized();
			updateShades();
		});

		image.addListener((obs, oldImg, newImg) -> {
			imageView.setImage(newImg);
			clearSelection();
		});

		overlay.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onPress);
		overlay.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onDrag);
		overlay.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onRelease);

		overlay.setCursor(Cursor.CROSSHAIR);
		clearSelection();
	}

	// ------------------- Public API -------------------

	public ObjectProperty<Image> imageProperty() {
		return image;
	}

	public void setImage(Image img) {
		image.set(img);
	}

	public Image getImage() {
		return image.get();
	}

	public ReadOnlyObjectProperty<javafx.geometry.Rectangle2D> cropRectInImagePixelsProperty() {
		return cropRectInImagePixels.getReadOnlyProperty();
	}

	public IntegerProperty maxOutputPixelsProperty() {
		return maxOutputPixels;
	}

	public int getMaxOutputPixels() {
		return maxOutputPixels.get();
	}

	public void setMaxOutputPixels(int maxPixels) {
		maxOutputPixels.set(Math.max(0, maxPixels));
	}

	public void clearSelection() {
		selection.setVisible(false);
		selection.setX(0);
		selection.setY(0);
		selection.setWidth(0);
		selection.setHeight(0);
		cropRectInImagePixels.set(null);
		selectionNormalizedValid = false;
		updateShades();
	}

	/**
	 * Returns cropped image (possibly downscaled if maxOutputPixels > 0).
	 */
	public WritableImage getCroppedImage() {
		Image img = imageView.getImage();
		javafx.geometry.Rectangle2D r = cropRectInImagePixels.get();
		if (img == null || r == null)
			return null;

		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return null;

		WritableImage cropped = new WritableImage(pr, (int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(),
				(int) r.getHeight());

		return downscaleIfNeeded(cropped);
	}

	/**
	 * Convenience if you need BufferedImage for ImageIO / JDBC blobs / etc.
	 */
	public BufferedImage getCroppedBufferedImage() {
		WritableImage w = getCroppedImage();
		return (w == null) ? null : SwingFXUtils.fromFXImage(w, null);
	}

	// ------------------- Internals -------------------

	private static Rectangle shadeRect() {
		Rectangle r = new Rectangle();
		r.setFill(Color.color(0, 0, 0, 0.45));
		r.setMouseTransparent(true);
		return r;
	}

	private void onPress(MouseEvent e) {
		if (e.getButton() != MouseButton.PRIMARY)
			return;
		if (imageView.getImage() == null)
			return;

		startX = clamp(e.getX(), 0, overlay.getWidth());
		startY = clamp(e.getY(), 0, overlay.getHeight());
		endX = startX;
		endY = startY;

		updateSelection();
		e.consume();
	}

	private void onDrag(MouseEvent e) {
		if (imageView.getImage() == null)
			return;

		endX = clamp(e.getX(), 0, overlay.getWidth());
		endY = clamp(e.getY(), 0, overlay.getHeight());

		updateSelection();
		captureSelectionNormalized();
		e.consume();
	}

	private void onRelease(MouseEvent e) {
		if (imageView.getImage() == null)
			return;

		endX = clamp(e.getX(), 0, overlay.getWidth());
		endY = clamp(e.getY(), 0, overlay.getHeight());

		updateSelection();
		updateCropRectInImagePixels();
		captureSelectionNormalized();
		e.consume();
	}

	private void updateSelection() {
		double x = Math.min(startX, endX);
		double y = Math.min(startY, endY);
		double w = Math.abs(endX - startX);
		double h = Math.abs(endY - startY);

		selection.setX(x);
		selection.setY(y);
		selection.setWidth(w);
		selection.setHeight(h);

		boolean visible = w >= 2 && h >= 2;
		selection.setVisible(visible);

		updateShades();
	}

	private void updateShades() {
		var img = imageView.getImage();
		if (img == null) {
			// nothing to shade
			topShade.setWidth(0);
			leftShade.setWidth(0);
			rightShade.setWidth(0);
			bottomShade.setWidth(0);
			return;
		}

		// The actual rendered image rectangle within this StackPane
		var b = imageView.getBoundsInParent();
		double X = b.getMinX();
		double Y = b.getMinY();
		double W = b.getWidth();
		double H = b.getHeight();

		// If selection is hidden, shade the whole image area
		if (!selection.isVisible()) {
			topShade.setX(X);
			topShade.setY(Y);
			topShade.setWidth(W);
			topShade.setHeight(H);

			leftShade.setWidth(0);
			rightShade.setWidth(0);
			bottomShade.setWidth(0);
			return;
		}

		// Selection coords must be in the SAME coordinate space (parent of selection).
		// Since selection is in overlay (which is a child of the same StackPane),
		// and you align overlay center, selection coordinates are in overlay local
		// coords.
		// To make this robust, compute selection in parent coords:
		var sel = selection.localToParent(selection.getBoundsInLocal());

		double sx = sel.getMinX();
		double sy = sel.getMinY();
		double sw = sel.getWidth();
		double sh = sel.getHeight();

		// Clamp selection to image rect (optional but recommended)
		double x1 = clamp(sx, X, X + W);
		double y1 = clamp(sy, Y, Y + H);
		double x2 = clamp(sx + sw, X, X + W);
		double y2 = clamp(sy + sh, Y, Y + H);

		double x = x1;
		double y = y1;
		double w = Math.max(0, x2 - x1);
		double h = Math.max(0, y2 - y1);

		// Top shade: image area above selection
		topShade.setX(X);
		topShade.setY(Y);
		topShade.setWidth(W);
		topShade.setHeight(y - Y);

		// Left shade: left of selection (within selection vertical span)
		leftShade.setX(X);
		leftShade.setY(y);
		leftShade.setWidth(x - X);
		leftShade.setHeight(h);

		// Right shade: right of selection
		rightShade.setX(x + w);
		rightShade.setY(y);
		rightShade.setWidth((X + W) - (x + w));
		rightShade.setHeight(h);

		// Bottom shade: below selection
		bottomShade.setX(X);
		bottomShade.setY(y + h);
		bottomShade.setWidth(W);
		bottomShade.setHeight((Y + H) - (y + h));
	}

	private void updateCropRectInImagePixels() {
		Image img = imageView.getImage();
		if (img == null || !selection.isVisible()) {
			cropRectInImagePixels.set(null);
			return;
		}

		double dispW = overlay.getWidth();
		double dispH = overlay.getHeight();
		if (dispW <= 0 || dispH <= 0) {
			cropRectInImagePixels.set(null);
			return;
		}

		double sx = img.getWidth() / dispW;
		double sy = img.getHeight() / dispH;

		int ix = (int) Math.floor(selection.getX() * sx);
		int iy = (int) Math.floor(selection.getY() * sy);
		int iw = (int) Math.ceil(selection.getWidth() * sx);
		int ih = (int) Math.ceil(selection.getHeight() * sy);

		ix = clampInt(ix, 0, (int) img.getWidth() - 1);
		iy = clampInt(iy, 0, (int) img.getHeight() - 1);

		int maxW = (int) img.getWidth() - ix;
		int maxH = (int) img.getHeight() - iy;

		iw = clampInt(iw, 1, maxW);
		ih = clampInt(ih, 1, maxH);

		cropRectInImagePixels.set(new javafx.geometry.Rectangle2D(ix, iy, iw, ih));
	}

	private void captureSelectionNormalized() {
		if (!selection.isVisible()) {
			selectionNormalizedValid = false;
			return;
		}
		var b = imageView.getBoundsInParent();
		double X = b.getMinX(), Y = b.getMinY(), W = b.getWidth(), H = b.getHeight();
		if (W <= 0 || H <= 0) {
			selectionNormalizedValid = false;
			return;
		}

		var sel = selection.localToParent(selection.getBoundsInLocal());
		double sx = sel.getMinX(), sy = sel.getMinY(), sw = sel.getWidth(), sh = sel.getHeight();

		rx = (sx - X) / W;
		ry = (sy - Y) / H;
		rw = sw / W;
		rh = sh / H;

		// clamp (safety)
		rx = clamp(rx, 0, 1);
		ry = clamp(ry, 0, 1);
		rw = clamp(rw, 0, 1 - rx);
		rh = clamp(rh, 0, 1 - ry);

		selectionNormalizedValid = true;
	}

	private void applySelectionFromNormalized() {
		if (!selection.isVisible() || !selectionNormalizedValid)
			return;

		var b = imageView.getBoundsInParent();
		double X = b.getMinX(), Y = b.getMinY(), W = b.getWidth(), H = b.getHeight();
		if (W <= 0 || H <= 0)
			return;

		double x = X + rx * W;
		double y = Y + ry * H;
		double w = rw * W;
		double h = rh * H;

		// selection is in overlay coordinates; overlay shares same parent as imageView,
		// so we can set in parent coords by converting parent->overlay local:
		var p = overlay.parentToLocal(x, y);
		selection.setX(p.getX());
		selection.setY(p.getY());
		selection.setWidth(w);
		selection.setHeight(h);
	}

	private WritableImage downscaleIfNeeded(Image cropped) {
		int limit = getMaxOutputPixels();
		if (limit <= 0) {
			return (cropped instanceof WritableImage wi) ? wi : snapshotToWritable(cropped);
		}

		double w = cropped.getWidth();
		double h = cropped.getHeight();
		double maxSide = Math.max(w, h);

		if (maxSide <= limit) {
			return (cropped instanceof WritableImage wi) ? wi : snapshotToWritable(cropped);
		}

		double scale = limit / maxSide;
		int targetW = Math.max(1, (int) Math.round(w * scale));
		int targetH = Math.max(1, (int) Math.round(h * scale));

		ImageView iv = new ImageView(cropped);
		iv.setPreserveRatio(true);
		iv.setSmooth(true);
		iv.setFitWidth(targetW);
		iv.setFitHeight(targetH);

		SnapshotParameters sp = new SnapshotParameters();
		sp.setFill(Color.TRANSPARENT);

		WritableImage out = new WritableImage(targetW, targetH);
		iv.snapshot(sp, out);
		return out;
	}

	private static WritableImage snapshotToWritable(Image img) {
		ImageView iv = new ImageView(img);
		SnapshotParameters sp = new SnapshotParameters();
		sp.setFill(Color.TRANSPARENT);
		WritableImage out = new WritableImage((int) img.getWidth(), (int) img.getHeight());
		iv.snapshot(sp, out);
		return out;
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	private static int clampInt(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	/**
	 * Sets image from a Swing/AWT BufferedImage. Useful for webcam capture,
	 * ImageIO, legacy Swing code.
	 */
	public void setImage(BufferedImage bufferedImage) {
		if (bufferedImage == null) {
			setImage((Image) null);
			return;
		}
		setImage(SwingFXUtils.toFXImage(bufferedImage, null));
	}

	public void setImageFromBuffered(BufferedImage bufferedImage) {
		setImage(bufferedImage);
	}

}
