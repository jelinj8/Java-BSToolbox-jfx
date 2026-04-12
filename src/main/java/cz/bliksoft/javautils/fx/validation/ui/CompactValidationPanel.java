package cz.bliksoft.javautils.fx.validation.ui;

import java.util.Comparator;
import java.util.List;

import cz.bliksoft.javautils.fx.validation.ValidationMessage;
import cz.bliksoft.javautils.fx.validation.ValidationResult;
import cz.bliksoft.javautils.fx.validation.ValidationResultLevel;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

/**
 * Compact validation message display — no header, no filter, no fixed-height
 * list. Renders one row per message, sorted by severity (ERROR first), and
 * collapses to zero height when there are no messages.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * CompactValidationPanel panel = new CompactValidationPanel();
 * panel.setValidationResult(validationResult);
 * </pre>
 */
public final class CompactValidationPanel extends VBox {

	private static final Comparator<ValidationMessage> BY_SEVERITY_DESC =
			Comparator.comparing(ValidationMessage::level).reversed();

	private final ObjectProperty<ValidationResult> validationResult =
			new SimpleObjectProperty<>(this, "validationResult");

	/** Stored so we can remove it when the ValidationResult is replaced. */
	private javafx.collections.ListChangeListener<ValidationMessage> messagesListener;

	public CompactValidationPanel() {
		getStyleClass().add("compact-validation-panel");
		setSpacing(4);

		// Collapse when empty — bind managed to visible so layout space is also freed
		managedProperty().bind(visibleProperty());
		setVisible(false);

		validationResult.addListener((obs, oldVr, newVr) -> {
			detach(oldVr);
			attach(newVr);
			rebuild(newVr);
		});
	}

	// -------------------------------------------------------------------------

	private void attach(ValidationResult vr) {
		if (vr == null) return;
		messagesListener = c -> rebuild(vr);
		vr.messages().addListener(messagesListener);
	}

	private void detach(ValidationResult vr) {
		if (vr == null || messagesListener == null) return;
		vr.messages().removeListener(messagesListener);
		messagesListener = null;
	}

	private void rebuild(ValidationResult vr) {
		getChildren().clear();

		if (vr == null || vr.messages().isEmpty()) {
			setVisible(false);
		} else {
			List<ValidationMessage> sorted = vr.messages().stream()
					.sorted(BY_SEVERITY_DESC)
					.toList();
			for (ValidationMessage msg : sorted) {
				getChildren().add(buildRow(msg));
			}
			setVisible(true);
		}

		// Let the layout pass finish, then ask the containing stage to fit its scene.
		Platform.runLater(this::sizeContainingStage);
	}

	private void sizeContainingStage() {
		if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
			stage.sizeToScene();
		}
	}

	private static HBox buildRow(ValidationMessage msg) {
		SVGPath icon = new SVGPath();
		icon.setContent(iconPath(msg.level()));
		icon.getStyleClass().addAll("compact-val-icon", "val-" + msg.level().name().toLowerCase());

		Label text = new Label(msg.message());
		text.setWrapText(true);
		text.getStyleClass().addAll("compact-val-text", "val-" + msg.level().name().toLowerCase());
		HBox.setHgrow(text, Priority.ALWAYS);

		HBox row = new HBox(6, icon, text);
		row.setAlignment(Pos.TOP_LEFT);
		row.setPadding(new Insets(2, 0, 2, 0));
		row.getStyleClass().addAll("compact-val-row", "val-" + msg.level().name().toLowerCase());
		return row;
	}

	private static String iconPath(ValidationResultLevel lvl) {
		return switch (lvl) {
			case ERROR -> "M12 2 L22 20 H2 Z";
			case WARN  -> "M12 2 L22 20 H2 Z";
			case INFO  -> "M12 2 A10 10 0 1 0 12 22 A10 10 0 1 0 12 2";
			case OK    -> "M4 12 L10 18 L20 6";
		};
	}

	// -------------------------------------------------------------------------
	// Properties

	public ValidationResult getValidationResult() {
		return validationResult.get();
	}

	public void setValidationResult(ValidationResult vr) {
		this.validationResult.set(vr);
	}

	public ObjectProperty<ValidationResult> validationResultProperty() {
		return validationResult;
	}
}
