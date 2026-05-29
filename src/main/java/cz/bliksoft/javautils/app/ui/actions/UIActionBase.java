package cz.bliksoft.javautils.app.ui.actions;

import cz.bliksoft.javautils.app.ui.interfaces.IIconSpecPropertyProvider;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.input.KeyCombination;

/**
 * Abstract base class for UI actions that optionally expose a keyboard
 * accelerator and icon spec.
 *
 * <p>
 * The accelerator field is lazily initialised so that actions with no
 * accelerator carry zero overhead. The property is {@code null} (not a
 * null-valued property) when no accelerator has been set, preserving the
 * existing {@code ActionBinder} / {@code AcceleratorManager} contract.
 *
 * <p>
 * The icon spec field follows the same lazy pattern:
 * {@link #iconSpecProperty()} returns {@code null} until
 * {@link #setIconSpec(String)} is called, so actions with no icon carry zero
 * overhead. {@link cz.bliksoft.javautils.app.ui.actions.IconBinder} already
 * handles a {@code null} return from {@code iconSpecProperty()}.
 */
public abstract class UIActionBase implements IUIAction, IIconSpecPropertyProvider {

	private ReadOnlyObjectWrapper<KeyCombination> accelerator;
	private ReadOnlyStringWrapper text;
	private SimpleStringProperty iconSpec;

	/**
	 * Sets (or replaces) the keyboard accelerator for this action. Typically called
	 * by {@link UIActions} after loading the action from the XML filesystem.
	 */
	public void setAccelerator(KeyCombination kc) {
		if (accelerator == null)
			accelerator = new ReadOnlyObjectWrapper<>(kc);
		else
			accelerator.set(kc);
	}

	@Override
	public ReadOnlyObjectProperty<KeyCombination> acceleratorProperty() {
		return accelerator != null ? accelerator.getReadOnlyProperty() : null;
	}

	/** Sets (or replaces) the label text for this action. */
	public void setText(String t) {
		if (text == null)
			text = new ReadOnlyStringWrapper(t);
		else
			text.set(t);
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return text != null ? text.getReadOnlyProperty() : null;
	}

	/** Sets (or replaces) the icon spec string for this action. */
	public void setIconSpec(String spec) {
		if (iconSpec == null)
			iconSpec = new SimpleStringProperty(spec);
		else
			iconSpec.set(spec);
	}

	@Override
	public Property<String> iconSpecProperty() {
		return iconSpec;
	}

	/**
	 * Creates a lightweight {@link UIActionBase} from a key, optional label text,
	 * optional icon spec, optional key-binding folder, and a {@link Runnable}.
	 *
	 * <p>
	 * Pass {@code null} for any optional parameter to omit it:
	 * <ul>
	 * <li>{@code text} — if {@code null}, {@link #textProperty()} returns
	 * {@code null}</li>
	 * <li>{@code iconSpec} — if {@code null}, no icon is set</li>
	 * <li>{@code shortcutFolder} — if non-{@code null}, the accelerator is loaded
	 * via {@link ShortcutFileLoader#loadFromKeyBindings(String)
	 * ShortcutFileLoader.loadFromKeyBindings(shortcutFolder + "/" + key)}</li>
	 * </ul>
	 */
	public static UIActionBase of(String key, String text, String iconSpec, String shortcutFolder, Runnable action) {
		UIActionBase result = new UIActionBase() {
			@Override
			public void execute() {
				action.run();
			}

			@Override
			public String getKey() {
				return key;
			}
		};
		if (text != null)
			result.setText(text);
		if (iconSpec != null)
			result.setIconSpec(iconSpec);
		if (shortcutFolder != null)
			result.setAccelerator(ShortcutFileLoader.loadFromKeyBindings(shortcutFolder + "/" + key));
		return result;
	}
}
