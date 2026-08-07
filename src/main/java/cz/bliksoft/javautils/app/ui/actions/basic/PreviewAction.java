package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.BSAppJFXMessages;
import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IPreview;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;

public class PreviewAction extends BasicContextUIAction<IPreview> {

	private static final ReadOnlyStringProperty TEXT = new ReadOnlyStringWrapper(
			BSAppJFXMessages.getString("PreviewAction.text")); //$NON-NLS-1$

	public PreviewAction() {
		super(IPreview.class);
	}

	@Override
	protected void execute(IPreview current) {
		current.preview();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IPreview current) {
		return current.getPreviewEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IPreview current) {
		return current.getPreviewIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/preview"); //$NON-NLS-1$
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return TEXT;
	}

	@Override
	public String getKey() {
		return "Preview";
	}
}
