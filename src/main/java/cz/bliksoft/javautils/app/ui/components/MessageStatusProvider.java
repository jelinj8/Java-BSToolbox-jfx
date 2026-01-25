package cz.bliksoft.javautils.app.ui.components;

import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.BSAppUIConstants;
import cz.bliksoft.javautils.context.AbstractContextListener;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.ContextChangedEvent;
import cz.bliksoft.javautils.context.ContextSearchResult;
import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import cz.bliksoft.javautils.fx.tools.PropertyAnimator;
import javafx.scene.control.Label;

public class MessageStatusProvider extends Label {

	private PropertyAnimator<Boolean> animator;

	public MessageStatusProvider() {
		this.animator = new PropertyAnimator<Boolean>(this, "setVisible", 10000, false, new Boolean[] { true }, //$NON-NLS-1$
				boolean.class);
		
		this.getStyleClass().add(BSAppUIConstants.CLASS_STATUSBAR_LABEL);

		Context.getGlobal().addContextListener(
				new AbstractContextListener<Object>(BSAppUI.CTX_MESSAGE_KEY, "MessageStatusProvider update") { // $NON-NLS-1$ //$NON-NLS-1$

					@Override
					public void fired(ContextChangedEvent<Object> event) {
						if (event.isNewNotNull()) {
							MessageStatusProvider.this.setText(event.getNewValue().toString());
							ContextSearchResult i = Context.getGlobal().getValue(BSAppUI.CTX_MESSAGE_ICON_KEY);
							MessageStatusProvider.this.setGraphic(i.isValid() ? ImageUtils.getIconView(i.getResult()) : null);
							MessageStatusProvider.this.animator.startOnceForward(false);
						} else {
							MessageStatusProvider.this.setText(null);
						}
						event.blockEventPropagation();
					}

				}, true);
	}

	/*
	 * private void updateStatus() {
	 * 
	 * }
	 */
}
