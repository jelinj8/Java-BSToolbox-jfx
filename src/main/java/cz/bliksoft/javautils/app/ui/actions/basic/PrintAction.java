package cz.bliksoft.javautils.app.ui.actions.basic;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IPrint;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public class PrintAction extends BasicContextUIAction<IPrint> {

	public PrintAction() {
		super(IPrint.class);
	}

	@Override
	protected void execute(IPrint current) {
		current.print();
	}

	@Override
	protected BooleanProperty getEnabledProperty(IPrint current) {
		return current.getPrintEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IPrint current) {
		return current.getPrintIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return "/icons/base/PRINT_24.png";
	}

	@Override
	public String getKey() {
		return "Print";
	}
}
