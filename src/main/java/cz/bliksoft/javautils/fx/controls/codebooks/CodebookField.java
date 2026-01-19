package cz.bliksoft.javautils.fx.controls.codebooks;

import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Window;

public class CodebookField<T> extends HBox {

	private final TextField textField = new TextField();
	private final Button selectButton = new Button("...");
	private final Button clearButton = new Button("✕");
//	private final StackPane iconPane = new StackPane();

	private final ObjectProperty<T> value = new SimpleObjectProperty<>();
	private ObjectProperty<T> boundExternal;

	private final ICodebookProvider<T> provider;

	private CodebookPopup popup;
	private boolean locked;

	// for "reuse CodebookField text as filter"
	private ChangeListener<String> filterListener;
	private IFilterableSelector activeFilterable;

	private T lastConfirmedValue = null;

	public CodebookField(ICodebookProvider<T> provider) {
		this.provider = provider;

		getStyleClass().add("codebook-field");
//		iconPane.getStyleClass().add("codebook-icon");
		textField.getStyleClass().add("codebook-text");
		selectButton.getStyleClass().add("select-button");
		selectButton.setFocusTraversable(false);

		clearButton.getStyleClass().add("clear-button");
		clearButton.setFocusTraversable(false);

		setAlignment(Pos.CENTER_LEFT);
		setSpacing(4);
		HBox.setHgrow(textField, Priority.ALWAYS);

		getChildren().addAll(/* iconPane, */textField, selectButton, clearButton);

		// start unlocked (editable)
		unlock();

		value.addListener((obs, oldVal, newVal) -> {
			if (newVal != null) {
				textField.setText(provider.toDisplayString(newVal));
				lock();
				lastConfirmedValue = newVal;
			} else {
				if (locked)
					unlock();
			}
		});

		selectButton.setOnAction(e -> identifyOrSelect());
		clearButton.setOnAction(e -> clear());

		textField.setOnKeyPressed(e -> {

			if (e.isAltDown())
				return;

			switch (e.getCode()) {
			case F4, DOWN -> {
				if (!locked) {
					identifyOrSelect();
					e.consume();
				}
			}
			case ESCAPE -> {
				if (popup != null && popup.isShowing()) {
					popup.hide();
					e.consume();
				}
			}
			case DELETE -> {
				if (locked) {
					clear();
					e.consume();
				}
			}
			case BACK_SPACE -> {
				if (locked) {
					unlock();
					// Optional: clear value so "select same value again" triggers a change
					setValue(null);
					textField.selectAll();
					e.consume();
				}
			}
			default -> {
			}
			}
		});
	}

	private void identifyOrSelect() {
		if (locked)
			unlock();

		String text = textField.getText();
		if (text != null && !text.isBlank()) {
			T identified = provider.identify(text, true);
			if (identified != null) {
				acceptValue(identified);
				return;
			}
		}

		// identify failed -> open popup or dialog based on provider
		openSelector();
	}

	private void openSelector() {
		// close popup if open
		if (popup != null && popup.isShowing()) {
			popup.hide();
			return;
		}

		ICodebookProvider.Selector<T> selector = provider.createSelector(this::acceptValue);

		if (selector instanceof ICodebookProvider.DialogSelector<T> dialogSel) {
			Window owner = (getScene() != null) ? getScene().getWindow() : null;
			dialogSel.show(owner, textField.getText() == null ? "" : textField.getText());
			return;
		}

		if (!(selector instanceof ICodebookProvider.PopupSelector<T> popupSel)) {
			throw new IllegalStateException("Unknown selector type: " + selector);
		}

		Region content = popupSel.content();
		popup = new CodebookPopup(content);

		// reuse CodebookField text as live filter if supported
		detachFilter();
		activeFilterable = popupSel.filterable();
		if (activeFilterable != null) {
			filterListener = (obs, o, n) -> activeFilterable.setFilterText(n == null ? "" : n);
			textField.textProperty().addListener(filterListener);
			activeFilterable.setFilterText(textField.getText() == null ? "" : textField.getText());

			popup.setOnHidden(ev -> detachFilter());
		}

		popup.installEnterToConfirm(this::acceptValue);

		popup.show(this, localToScreen(0, getHeight()).getX(), localToScreen(0, getHeight()).getY());
	}

	private void detachFilter() {
		if (filterListener != null) {
			textField.textProperty().removeListener(filterListener);
			filterListener = null;
		}
		activeFilterable = null;
	}

	private void acceptValue(T selected) {
		if (selected == null) {
			// cancel (ignore)
			if (popup != null)
				popup.hide();
			return;
		}
		lastConfirmedValue = selected;

		detachFilter();

		if (Objects.equals(selected, getValue())) {
			textField.setText(provider.toDisplayString(selected));
			lock();
		} else {
			setValue(selected);
		}

		if (popup != null)
			popup.hide();
	}

	// ---- API ----

	public ObjectProperty<T> valueProperty() {
		return value;
	}

	public T getValue() {
		return value.get();
	}

	public void setValue(T v) {
		value.set(v);
	}

	public void bindBidirectional(ObjectProperty<T> externalProperty) {
		if (externalProperty == null)
			throw new IllegalArgumentException("externalProperty must not be null");
		if (boundExternal == externalProperty)
			return;

		if (boundExternal != null)
			value.unbindBidirectional(boundExternal);
		boundExternal = externalProperty;
		value.bindBidirectional(boundExternal);
	}

	public void clear() {
		lastConfirmedValue = null;
		unlock();
		setValue(null);
		textField.clear();
	}

	public void lock() {
		locked = true;
		textField.setEditable(false);
		textField.setDisable(false);
		if (!getStyleClass().contains("locked"))
			getStyleClass().add("locked");
	}

	public void unlock() {
		locked = false;
		textField.setEditable(true);
		textField.setDisable(false);
		getStyleClass().remove("locked");

		if (lastConfirmedValue != null) {
			String seed = provider.toEditString(lastConfirmedValue);
			if (seed != null) {
				textField.setText(seed);
				textField.positionCaret(seed.length());
				textField.selectAll(); // optional: select all for quick overwrite
			}
		}
	}

	public boolean isLocked() {
		return locked;
	}
}
