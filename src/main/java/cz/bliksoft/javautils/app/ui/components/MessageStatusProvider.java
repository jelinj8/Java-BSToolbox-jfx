package cz.bliksoft.javautils.app.ui.components;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.events.MessageEvent;
import cz.bliksoft.javautils.app.ui.BSAppUIConstants;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.events.EventListener;
import cz.bliksoft.javautils.fx.tools.PropertyAnimator;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;

public class MessageStatusProvider extends Label {
	Logger log = LogManager.getLogger();

	private PropertyAnimator<Boolean> animator;
	private StringProperty additionalClass = new SimpleStringProperty(null);

	public MessageStatusProvider() {
		managedProperty().bind(visibleProperty());

		this.animator = new PropertyAnimator<Boolean>(this::setVisible, 10000, false, new Boolean[] { true });

		this.getStyleClass().add(BSAppUIConstants.CLASS_STATUSBAR_LABEL);

		additionalClass.addListener((observable, oldClass, newClass) -> {
			if (Objects.equals(oldClass, newClass))
				return;
			if (oldClass != null)
				getStyleClass().remove(oldClass);
			if (newClass != null)
				getStyleClass().add(newClass);
		});

		Context.getRoot().addEventListener(new EventListener<MessageEvent>(MessageEvent.class, "statusbar message") {

			@Override
			public void fired(MessageEvent event) {
				log.info(event.getMesage());
				graphicProperty().setValue(event.getGraphics());
				textProperty().setValue(event.getMesage());
				additionalClass.setValue(event.getFxStyle());
				MessageStatusProvider.this.animator.startOnceForward();
			}
		});
	}
}
