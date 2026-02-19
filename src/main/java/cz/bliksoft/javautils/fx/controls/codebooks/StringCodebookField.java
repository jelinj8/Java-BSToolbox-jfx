package cz.bliksoft.javautils.fx.controls.codebooks;

import java.util.ArrayList;
import java.util.List;

import cz.bliksoft.javautils.fx.controls.codebooks.providers.ListCodebookPopupProvider;
import javafx.beans.property.StringProperty;

public class StringCodebookField extends CodebookField<String> {

	/**
	 * proper usage
	 * @param provider
	 */
	public StringCodebookField(ICodebookProvider<String> provider) {
		super(provider);
	}

	/**
	 * demo variant for UI editor
	 */
	public StringCodebookField() {
		super(new ListCodebookPopupProvider<>(new ArrayList<>(List.of("A", "B", "C"))));
	}

	public final void setValue(String v) {
		super.setValue(v);
	}

	public final String getValue() {
		return super.getValue();
	}

	public final StringProperty valuePropertyString() {
		throw new UnsupportedOperationException();
	}
}