package cz.bliksoft.javautils.app.exceptions;

public class ViewableException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6293069082089946890L;

	public ViewableException(String message) {
		super(message);
	}

	public ViewableException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
