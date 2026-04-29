package cz.bliksoft.javautils.fx.validation;

public record ValidationMessage(ValidationResultLevel level, Object key, String message, String property) {

	public ValidationMessage(ValidationResultLevel level, Object key, String message) {
		this(level, key, message, null);
	}
}