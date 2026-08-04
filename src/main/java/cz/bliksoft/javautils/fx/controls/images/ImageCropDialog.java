package cz.bliksoft.javautils.fx.controls.images;

import java.awt.image.BufferedImage;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.fx.customization.BSButtonTypes;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

/**
 * Dialog for viewing and optionally cropping/rotating an already-captured image
 * - e.g. a photo received in the background (see
 * {@code cz.bliksoft.javautils.fx.controls.images.cam.UnsolicitedFrameEvent})
 * or an existing attachment's image. Unlike {@link CameraCaptureDialog}, there
 * is no camera source selection or live capture - the image is supplied
 * directly.
 *
 * <p>
 * The result carries both the (possibly rotated) source image and the crop
 * rectangle applied to it, rather than only the cropped pixels, so a caller can
 * keep the full-resolution source around and let the user re-crop from it again
 * later instead of compounding crops.
 */
public class ImageCropDialog extends Dialog<ImageCropDialog.CropResult> {

	private final ImageCropPane cropPane = new ImageCropPane();

	private final Button autocropBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/crop")));
	private final Button rotateLeftBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/rotate-left")));
	private final Button rotateRightBtn = new Button(null,
			ImageUtils.getIconView(IconspecUtils.getIconspec("buttons/rotate-right")));

	/**
	 * The (possibly rotated) source image plus the crop rectangle applied to it (in
	 * {@code sourceImage}'s pixel coordinates), or {@code null} if no crop was
	 * selected.
	 */
	public record CropResult(BufferedImage sourceImage, java.awt.Rectangle cropRect) {

		/** Returns the cropped pixels, or {@code sourceImage} unchanged if no crop. */
		public BufferedImage cropped() {
			return cropRect == null ? sourceImage
					: sourceImage.getSubimage(cropRect.x, cropRect.y, cropRect.width, cropRect.height);
		}
	}

	public ImageCropDialog() {
		setTitle(BSAppJFXMessages.getString("ImageCropDialog.title"));
		getDialogPane().getButtonTypes().setAll(BSButtonTypes.OK, BSButtonTypes.CANCEL);

		autocropBtn.setTooltip(new Tooltip(BSAppJFXMessages.getString("CameraCaptureDialog.toolbar.autocropButton")));
		autocropBtn.setOnAction(e -> cropPane.autocrop());

		rotateLeftBtn
				.setTooltip(new Tooltip(BSAppJFXMessages.getString("CameraCaptureDialog.toolbar.rotateLeftButton")));
		rotateLeftBtn.setOnAction(e -> cropPane.rotateLeft());

		rotateRightBtn
				.setTooltip(new Tooltip(BSAppJFXMessages.getString("CameraCaptureDialog.toolbar.rotateRightButton")));
		rotateRightBtn.setOnAction(e -> cropPane.rotateRight());

		ToolBar toolbar = new ToolBar(autocropBtn, rotateLeftBtn, rotateRightBtn);

		BorderPane content = new BorderPane();
		content.setTop(toolbar);
		content.setCenter(cropPane);
		getDialogPane().setContent(content);
		getDialogPane().setPrefSize(760, 520);

		Button okButton = (Button) getDialogPane().lookupButton(BSButtonTypes.OK);
		okButton.setDisable(true);
		cropPane.imageProperty().addListener((obs, o, n) -> okButton.setDisable(n == null));

		setResultConverter(bt -> {
			if (bt != BSButtonTypes.OK)
				return null;
			Image img = cropPane.getImage();
			if (img == null)
				return null;
			return new CropResult(SwingFXUtils.fromFXImage(img, null),
					ImageCropPane.toAwtRect(cropPane.cropRectInImagePixelsProperty().get()));
		});
	}

	/**
	 * Opens the dialog pre-loaded with {@code image}. If {@code initialCropRect} is
	 * non-{@code null}, pre-selects that region (e.g. to restore a previous crop);
	 * otherwise, if {@code autocropIfNoInitial}, runs
	 * {@link ImageCropPane#autocrop()} immediately so the user can confirm/adjust
	 * the auto-detected box.
	 *
	 * @return the result, or {@code null} if cancelled
	 */
	public static CropResult edit(Window owner, BufferedImage image, java.awt.Rectangle initialCropRect,
			boolean autocropIfNoInitial) {
		ImageCropDialog dlg = new ImageCropDialog();
		if (owner != null)
			dlg.initOwner(owner);
		dlg.cropPane.setImage(image);
		if (initialCropRect != null)
			dlg.cropPane.setCropRect(initialCropRect);
		else if (autocropIfNoInitial)
			dlg.cropPane.autocrop();
		dlg.showAndWait();
		return dlg.getResult();
	}
}
