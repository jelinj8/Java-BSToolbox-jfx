package cz.bliksoft.javautils.fx.controls.dialogs.busy;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public final class BusyOverlay extends StackPane {

	private final Timeline frameTimeline = new Timeline();
	private int frameIndex = 0;

	public BusyOverlay(BusyDialogModel model) {
		setPickOnBounds(true); // capture clicks
		setVisible(false);
		setManaged(false);

		// Semi-transparent scrim
		Rectangle scrim = new Rectangle();
		scrim.setFill(Color.rgb(0, 0, 0, 0.35));
		scrim.widthProperty().bind(widthProperty());
		scrim.heightProperty().bind(heightProperty());

		// Content card
		VBox card = new VBox(10);
		card.setPadding(new Insets(16));
		card.setAlignment(Pos.CENTER_LEFT);
		card.setMaxWidth(520);
		card.setBackground(new Background(new BackgroundFill(Color.web("#2b2b2b"), new CornerRadii(12), Insets.EMPTY)));
		card.setBorder(new Border(new BorderStroke(Color.rgb(255, 255, 255, 0.12), BorderStrokeStyle.SOLID,
				new CornerRadii(12), new BorderWidths(1))));
		card.setEffect(new javafx.scene.effect.DropShadow(20, Color.rgb(0, 0, 0, 0.35)));

		Label title = new Label();
		title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
		title.textProperty().bind(model.titleProperty());

		Label detail = new Label();
		detail.setWrapText(true);
		detail.setStyle("-fx-text-fill: rgba(255,255,255,0.85);");
		detail.textProperty().bind(model.detailProperty());

		// Animated image sequence (optional)
		ImageView animView = new ImageView();
		animView.setFitWidth(40);
		animView.setFitHeight(40);
		animView.setPreserveRatio(true);
		animView.setSmooth(true);

		// Fallback indicator if no frames
		ProgressIndicator spinner = new ProgressIndicator();
		spinner.setPrefSize(32, 32);

		Node leftVisual = new StackPane(spinner, animView);
		((StackPane) leftVisual).setAlignment(Pos.CENTER);
		animView.visibleProperty().bind(Bindings.isNotEmpty(model.getFrames()));
		spinner.visibleProperty().bind(Bindings.isEmpty(model.getFrames()));

		// Progress handling
		ProgressBar bar = new ProgressBar();
		bar.setMaxWidth(Double.MAX_VALUE);

		// progress in [0..1]; if indeterminate => -1
		bar.progressProperty().bind(Bindings.createDoubleBinding(() -> {
			if (model.indeterminateProperty().get())
				return -1.0;
			long max = model.maxProperty().get();
			long val = model.valueProperty().get();
			if (max <= 0)
				return 0.0;
			return Math.max(0.0, Math.min(1.0, (double) val / (double) max));
		}, model.indeterminateProperty(), model.maxProperty(), model.valueProperty()));

		Label progressText = new Label();
		progressText.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 11px;");
		progressText.textProperty().bind(Bindings.createStringBinding(() -> {
			if (model.indeterminateProperty().get())
				return "";
			return model.valueProperty().get() + " / " + model.maxProperty().get();
		}, model.indeterminateProperty(), model.valueProperty(), model.maxProperty()));

		HBox header = new HBox(12, leftVisual, new VBox(4, title, detail));
		header.setAlignment(Pos.CENTER_LEFT);

		VBox progressBox = new VBox(6, bar, progressText);
		progressBox.setFillWidth(true);
		VBox.setVgrow(progressBox, Priority.NEVER);

		card.getChildren().addAll(header, progressBox);

		// Center the card
		StackPane.setAlignment(card, Pos.CENTER);

		getChildren().addAll(scrim, card);

		// Bind visibility to model.showing
		visibleProperty().bind(model.showingProperty());
		managedProperty().bind(model.showingProperty());

		// Block all mouse/keys while visible
		addEventFilter(MouseEvent.ANY, e -> {
			if (isVisible())
				e.consume();
		});
		addEventFilter(KeyEvent.ANY, e -> {
			if (isVisible())
				e.consume();
		});

		// Animation timeline: switch frame every 120ms (adjust as needed)
		frameTimeline.setCycleCount(Animation.INDEFINITE);
		frameTimeline.getKeyFrames().setAll(new KeyFrame(Duration.millis(120), e -> {
			var frames = model.getFrames();
			if (frames.isEmpty())
				return;
			frameIndex = (frameIndex + 1) % frames.size();
			Image img = frames.get(frameIndex);
			animView.setImage(img);
		}));

		// Start/stop timeline depending on showing + frames
		model.showingProperty().addListener((obs, oldV, newV) -> {
			if (newV) {
				frameIndex = -1;
				if (!model.getFrames().isEmpty())
					frameTimeline.playFromStart();
			} else {
				frameTimeline.stop();
			}
		});
		model.getFrames().addListener((javafx.collections.ListChangeListener<Image>) c -> {
			if (isVisible() && !model.getFrames().isEmpty())
				frameTimeline.play();
			if (model.getFrames().isEmpty()) {
				frameTimeline.stop();
				animView.setImage(null);
			}
		});

		// A little default CSS-ish look for ProgressBar on dark background
		bar.setStyle("""
				    -fx-accent: #4ea1ff;
				    -fx-control-inner-background: rgba(255,255,255,0.12);
				""");
	}
}
