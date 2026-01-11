package cz.bliksoft.javautils.fx.controls.renderers;

import java.util.Set;
import java.util.function.Function;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

public class IconTextListCell<T> extends ListCell<T> {

	private final Function<T, String> textProvider;
	private final Function<T, Set<String>> classProvider;
	private final Function<T, Image> iconProvider;
	private final Function<T, String> overlayPathProvider;

	private final ImageView icon = new ImageView();
	private final Region overlay = new Region();
	private final SVGPath overlayPath = new SVGPath();
	private final StackPane iconPane = new StackPane(icon, overlay, overlayPath);

	private Set<String> addedClasses = null;

	public IconTextListCell(Function<T, String> textProvider, Function<T, Image> iconProvider,
			Function<T, Set<String>> classProvider, Function<T, String> overlayPathProvider) {
		this.textProvider = textProvider;
		this.iconProvider = iconProvider;
		this.classProvider = classProvider;
		this.overlayPathProvider = overlayPathProvider;

		getStyleClass().add("icon-text-cell");

		icon.setFitWidth(-1);
		icon.setFitHeight(-1);
		icon.setPreserveRatio(true);
		icon.getStyleClass().add("icon");

		overlay.getStyleClass().add("icon-overlay");
		overlay.setMouseTransparent(true);

		overlayPath.getStyleClass().add("icon-overlay-path");
		StackPane.setAlignment(overlayPath, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(overlay, new Insets(0, 2, 2, 0));
	}

	@Override
	protected void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);

		if (addedClasses != null) {
			getStyleClass().removeAll(addedClasses);
			addedClasses = null;
		}

		if (empty || item == null) {
			setText(null);
			setGraphic(null);
		} else {
			if (textProvider != null) {
				setText(textProvider.apply(item));
			} else {
				setText(String.valueOf(item));
			}

			if (iconProvider != null) {
				Image img = iconProvider.apply(item);
				if (img != null) {
					icon.setImage(img);
					setGraphic(iconPane);
				} else {
					setGraphic(null);
				}
			} else if (overlayPathProvider != null) {
				icon.setImage(null);
				setGraphic(iconPane);
			}

			if (classProvider != null) {
				addedClasses = classProvider.apply(item);
				if (addedClasses != null)
					getStyleClass().addAll(addedClasses);
			}

			if (overlayPathProvider != null) {
				String path = overlayPathProvider.apply(item);
				overlayPath.setContent(path == null ? "" : path);
			} else {
				overlayPath.setContent("");
			}
		}
	}
}
