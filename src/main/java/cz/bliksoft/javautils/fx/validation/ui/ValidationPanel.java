package cz.bliksoft.javautils.fx.validation.ui;

import java.util.Objects;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.fx.validation.ValidationMessage;
import cz.bliksoft.javautils.fx.validation.ValidationResult;
import cz.bliksoft.javautils.fx.validation.ValidationResultLevel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Reusable panel that displays ValidationResult messages.
 *
 * Usage: ValidationPanel panel = new ValidationPanel();
 * panel.setValidationResult(validationResult);
 *
 * Optional: panel.setAutoHideWhenOk(true);
 * panel.setFilterMode(ValidationPanel.FilterMode.WARN_AND_ERROR);
 */
public final class ValidationPanel extends VBox {

	public enum FilterMode {
		ALL, INFO_AND_ABOVE, WARN_AND_ABOVE, ERROR_ONLY
	}

	private final ObjectProperty<ValidationResult> validationResult = new SimpleObjectProperty<>(this,
			"validationResult");

	private final ObjectProperty<FilterMode> filterMode = new SimpleObjectProperty<>(this, "filterMode",
			FilterMode.ALL);

	private final BooleanProperty autoHideWhenOk = new SimpleBooleanProperty(this, "autoHideWhenOk", false);

	private void bindAutoHide(ValidationResult vr) {
		// unbind first
		visibleProperty().unbind();

		if (vr == null) {
			setVisible(true);
			return;
		}

		BooleanBinding show = Bindings.createBooleanBinding(() -> {
			if (!isAutoHideWhenOk())
				return true;
			return vr.level().getValue() != ValidationResultLevel.OK && !vr.messages().isEmpty();
		}, autoHideWhenOk, vr.level(), vr.messages());

		managedProperty().bind(visibleProperty());
		visibleProperty().bind(show);
	}

	private final Label header = new Label("Validation");
	private final ComboBox<FilterMode> filterBox = new ComboBox<>();
	private final ListView<ValidationMessage> listView = new ListView<>();

	private FilteredList<ValidationMessage> filtered;

	public ValidationPanel() {
		getStyleClass().add("validation-panel");
		setSpacing(8);
		setPadding(new Insets(8));

		header.getStyleClass().add("validation-header");

		filterBox.getItems().setAll(FilterMode.values());
		filterBox.valueProperty().bindBidirectional(filterMode);
		filterBox.setMaxWidth(Double.MAX_VALUE);

		HBox top = new HBox(8, header, spacer(), filterBox);
		top.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(filterBox, Priority.NEVER);

		listView.setPlaceholder(new Label("No validation messages."));
		listView.getStyleClass().add("validation-list");
		listView.setCellFactory(lv -> new ValidationMessageCell());

		getChildren().addAll(top, new Separator(), listView);

		// Re-bind when validationResult changes
		validationResult.addListener((obs, oldVr, newVr) -> {
			bindTo(newVr);
			bindAutoHide(newVr);
			bindHeader(newVr);
		});
		bindTo(null);
		bindAutoHide(null);
		bindHeader(null);

		// Auto-hide when OK/empty
		BooleanBinding hasAnythingToShow = Bindings.createBooleanBinding(() -> {
			ValidationResult vr = getValidationResult();
			if (vr == null)
				return false;
			if (!isAutoHideWhenOk())
				return true;
			return vr.level().getValue() != ValidationResultLevel.OK && !vr.messages().isEmpty();
		}, validationResult, autoHideWhenOk,
				// these bindings exist only if vr != null; we also refresh in bindTo()
				filterMode);

		managedProperty().bind(visibleProperty());
		visibleProperty().bind(hasAnythingToShow);

		// Update header whenever inputs change
		header.textProperty().bind(Bindings.createStringBinding(() -> {
			ValidationResult vr = getValidationResult();
			if (vr == null)
				return "Validation";
			ValidationResultLevel lvl = vr.level().getValue();
			int count = vr.messages().size();
			return "Validation: " + lvl + " (" + count + ")";
		}, validationResult));
	}

	private void bindTo(ValidationResult vr) {
		// Items + filtering
		if (vr == null) {
			filtered = new FilteredList<>(FXCollections.observableArrayList());
			listView.setItems(filtered);
			return;
		}

		filtered = new FilteredList<>(vr.messages());
		listView.setItems(filtered);

		// Predicate depends on filter mode
		filterMode.addListener((o, ov, nv) -> updatePredicate());
		updatePredicate();

		// Refresh visibility binding dependencies (since vr changed)
		vr.level().addListener((o, ov, nv) -> {
			// force refresh of list formatting
			listView.refresh();
		});
		vr.messages().addListener((ListChangeListener<ValidationMessage>) c -> listView.refresh());
	}

	private void bindHeader(ValidationResult vr) {
		header.textProperty().unbind();

		if (vr == null) {
			header.setText("Validation");
			return;
		}

		header.textProperty().bind(Bindings.createStringBinding(() -> {
			ValidationResultLevel lvl = vr.level().getValue();
			int count = vr.messages().size();
			return "Validation: " + lvl + " (" + count + ")";
		}, vr.level(), vr.messages()));
	}

	private void updatePredicate() {
		if (filtered == null)
			return;

		FilterMode mode = getFilterMode();
		filtered.setPredicate(m -> {
			if (m == null)
				return false;
			return switch (mode) {
			case ALL -> true;
			case INFO_AND_ABOVE -> m.level().compareTo(ValidationResultLevel.INFO) >= 0;
			case WARN_AND_ABOVE -> m.level().compareTo(ValidationResultLevel.WARN) >= 0;
			case ERROR_ONLY -> m.level() == ValidationResultLevel.ERROR;
			};
		});
	}

	private static VBox spacer() {
		VBox s = new VBox();
		HBox.setHgrow(s, Priority.ALWAYS);
		return s;
	}

	// --- Properties

	public ValidationResult getValidationResult() {
		return validationResult.get();
	}

	public void setValidationResult(ValidationResult vr) {
		this.validationResult.set(vr);
	}

	public ObjectProperty<ValidationResult> validationResultProperty() {
		return validationResult;
	}

	public FilterMode getFilterMode() {
		return filterMode.get();
	}

	public void setFilterMode(FilterMode mode) {
		this.filterMode.set(Objects.requireNonNull(mode));
	}

	public ObjectProperty<FilterMode> filterModeProperty() {
		return filterMode;
	}

	public boolean isAutoHideWhenOk() {
		return autoHideWhenOk.get();
	}

	public void setAutoHideWhenOk(boolean v) {
		this.autoHideWhenOk.set(v);
	}

	public BooleanProperty autoHideWhenOkProperty() {
		return autoHideWhenOk;
	}

	// --- Cell

	private static final class ValidationMessageCell extends ListCell<ValidationMessage> {
		private final HBox root = new HBox(8);
		private final SVGPath icon = new SVGPath();
		private final Label text = new Label();
		private final Label property = new Label();

		ValidationMessageCell() {
			root.setAlignment(Pos.TOP_LEFT);

			icon.getStyleClass().add("validation-list-icon");
			text.getStyleClass().add("validation-list-text");

			property.getStyleClass().add("validation-list-property");
			property.setWrapText(true);
			property.setOpacity(0.65);

			VBox v = new VBox(2, text, property);
			root.getChildren().addAll(icon, v);

			text.setWrapText(true);
		}

		@Override
		protected void updateItem(ValidationMessage item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setGraphic(null);
				setText(null);
				getStyleClass().removeAll("val-ok", "val-info", "val-warn", "val-error");
				return;
			}

			text.setText(item.message());
			if (StringUtils.hasLength(item.property())) {
				property.setVisible(true);
				property.setManaged(true);
				property.setText(String.valueOf(item.property()));
			} else {
				property.setVisible(false);
				property.setManaged(false);
				property.setText("");
			}

			ValidationResultLevel lvl = item.level();
			icon.setContent(iconPath(lvl));

			getStyleClass().removeAll("val-ok", "val-info", "val-warn", "val-error");
			getStyleClass().add("val-" + lvl.name().toLowerCase());

			setGraphic(root);
			setText(null);
		}

		private static String iconPath(ValidationResultLevel lvl) {
			// simple icons (you can replace with your own SVG paths)
			return switch (lvl) {
			case ERROR -> "M12 2 L22 20 H2 Z"; // triangle
			case WARN -> "M12 2 L22 20 H2 Z"; // triangle (CSS differentiates)
			case INFO -> "M12 2 A10 10 0 1 0 12 22 A10 10 0 1 0 12 2"; // circle
			case OK -> "M4 12 L10 18 L20 6"; // check-ish
			};
		}
	}
}
