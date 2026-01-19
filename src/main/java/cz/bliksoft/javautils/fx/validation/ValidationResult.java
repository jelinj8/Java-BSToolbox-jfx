package cz.bliksoft.javautils.fx.validation;

import java.util.Comparator;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ValidationResult {

	ObservableList<ValidationMessage> messages = FXCollections.observableArrayList();

	Property<ValidationResultLevel> level = new SimpleObjectProperty<>(ValidationResultLevel.OK);

	public ObservableList<ValidationMessage> messages() {
		return messages;
	}

	public Property<ValidationResultLevel> level() {
		return level;
	}

	public void addMessage(ValidationResultLevel level, Object key, String message) {
		addMessage(level, key, message, null);

	}

	public void addMessage(ValidationResultLevel level, Object key, String message, String property) {
		messages.add(new ValidationMessage(level, key, message, property));

		if (this.level.getValue().compareTo(level) < 0)
			this.level.setValue(level);
	}

	public void clear() {
		messages.clear();
		level.setValue(ValidationResultLevel.OK);
	}

	public void removeMessage(Object key) {
		if (key != null)
			messages.removeIf(m -> key.equals(m.key()));

		updateLevel();
	}

	private void updateLevel() {
		level.setValue(
				messages.stream().map(m -> m.level()).max(Comparator.naturalOrder()).orElse(ValidationResultLevel.OK));
	}

}
