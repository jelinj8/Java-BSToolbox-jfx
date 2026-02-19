package cz.bliksoft.javautils.app.events;

import javafx.scene.Node;

public class MessageEvent {

	private String message;
	private Node graphics = null;
	private String fxStyle = null;

	public String getMesage() {
		return message;
	}

	public String getFxStyle() {
		return fxStyle;
	}

	public Node getGraphics() {
		return graphics;
	}

	public MessageEvent(String message) {
		this.message = message;
		this.graphics = null;
		this.fxStyle = null;
	}

	public MessageEvent(String message, Node graphics) {
		this.message = message;
		this.graphics = graphics;
		this.fxStyle = null;
	}

	public MessageEvent(String message, Node graphics, String fxStyle) {
		this.message = message;
		this.graphics = graphics;
		this.fxStyle = fxStyle;
	}

	public MessageEvent(String message, String fxStyle) {
		this.message = message;
		this.graphics = null;
		this.fxStyle = fxStyle;
	}
}
