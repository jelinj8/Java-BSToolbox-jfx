package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IPreview;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class PreviewAction extends BasicContextUIAction<IPreview> {

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
		return "/icons/base/SEARCH_16.png";
	}

	@Override
	public String getKey() {
		return "Preview";
	}
}
