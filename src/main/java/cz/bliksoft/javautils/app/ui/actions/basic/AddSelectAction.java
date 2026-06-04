package cz.bliksoft.javautils.app.ui.actions.basic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.IUIActionWithSubactions;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IAddOptions;
import cz.bliksoft.javautils.app.ui.interfaces.IIconSpecPropertyProvider;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Context-aware action for {@link IAddOptions}.
 *
 * <p>
 * Maintains a stable {@link #getSubactions()} list that mirrors the
 * {@link IUIAction} options provided by the current context value. Bind a
 * toolbar dropdown to this list once — it tracks context changes automatically.
 *
 * <p>
 * {@link #execute(IAddOptions)} runs the first available option, matching the
 * default-click behaviour of a {@code SplitMenuButton}.
 *
 * <p>
 * The icon, menu-icon, and text exposed by this action are taken from the first
 * subaction when it provides them, and fall back to the built-in
 * {@code action/add-select} iconspec and no-text otherwise.
 */
public class AddSelectAction extends BasicContextUIAction<IAddOptions> implements IUIActionWithSubactions {

	private static final Logger log = LogManager.getLogger(AddSelectAction.class);

	private final ObservableList<IUIAction> subactions = FXCollections.observableArrayList();
	private ListChangeListener<IUIAction> sourceListener = null;

	// Effective display properties (shadow BasicContextUIAction's computed values)
	private SimpleStringProperty effectiveIconSpec;
	private SimpleStringProperty effectiveMenuIconSpec;
	private ReadOnlyStringWrapper effectiveText;

	// References to BasicContextUIAction's computed properties (used as fallback)
	private Property<String> baseIconSpec;
	private Property<String> baseMenuIconSpec;

	// Track the current first subaction and react to its property changes
	private IUIAction trackedFirstSubaction;
	private final ChangeListener<String> firstSubactionPropListener = (obs, o, n) -> reapplyFirstSubaction();

	/** Creates the action and registers it with the current context. */
	public AddSelectAction() {
		super(IAddOptions.class);

		// Capture references to BasicContextUIAction's computed properties before
		// we override iconSpecProperty() / menuIconSpecProperty() below.
		this.baseIconSpec = super.iconSpecProperty();
		this.baseMenuIconSpec = super.menuIconSpecProperty();
		this.effectiveIconSpec = new SimpleStringProperty(baseIconSpec.getValue());
		this.effectiveMenuIconSpec = new SimpleStringProperty(baseMenuIconSpec.getValue());
		this.effectiveText = new ReadOnlyStringWrapper();

		// Keep effective icons in sync when the base class recomputes them
		// (e.g. when the context-provided overlay changes).
		baseIconSpec.addListener((obs, o, n) -> {
			if (isBlankOrNull(getFirstSubactionIconValue()))
				effectiveIconSpec.set(n);
		});
		baseMenuIconSpec.addListener((obs, o, n) -> {
			if (isBlankOrNull(getFirstSubactionMenuIconValue()))
				effectiveMenuIconSpec.set(n);
		});

		// Update first-subaction display whenever the subactions list changes.
		subactions.addListener((ListChangeListener<IUIAction>) c -> updateFirstSubactionDisplay());

		// Replay the initial context value now that all fields are initialised.
		// This triggers onValueChanged → subactions.setAll() → the listener above
		// → updateFirstSubactionDisplay(), so effective properties end up correct.
		refreshContext();
	}

	// =========================================================================
	// IIconSpecPropertyProvider / IUIAction — delegate to first subaction
	// =========================================================================

	@Override
	public Property<String> iconSpecProperty() {
		return effectiveIconSpec;
	}

	@Override
	public Property<String> menuIconSpecProperty() {
		return effectiveMenuIconSpec;
	}

	@Override
	public ReadOnlyStringProperty textProperty() {
		return effectiveText != null ? effectiveText.getReadOnlyProperty() : null;
	}

	// =========================================================================
	// IUIActionWithSubactions
	// =========================================================================

	@Override
	public ObservableList<IUIAction> getSubactions() {
		return subactions;
	}

	// =========================================================================
	// Context tracking
	// =========================================================================

	@Override
	protected void onValueChanged(IAddOptions oldValue, IAddOptions newValue) {
		if (subactions == null)
			return; // super() called this before field init; refreshContext() will re-fire

		if (oldValue != null && sourceListener != null) {
			ObservableList<IUIAction> oldOptions = oldValue.getOptions();
			if (oldOptions != null)
				oldOptions.removeListener(sourceListener);
			sourceListener = null;
		}

		subactions.clear();

		if (newValue != null) {
			ObservableList<IUIAction> options = newValue.getOptions();
			if (options != null) {
				subactions.setAll(options);
				sourceListener = c -> subactions.setAll(newValue.getOptions());
				options.addListener(sourceListener);
			} else {
				log.warn("AddSelectAction: {}.getOptions() returned null — dropdown will be empty.",
						newValue.getClass().getSimpleName());
			}
		}
	}

	/**
	 * Executes the first available subaction — matches split-button default click.
	 */
	@Override
	protected void execute(IAddOptions current) {
		if (!subactions.isEmpty()) {
			subactions.get(0).execute();
		}
	}

	@Override
	protected BooleanProperty getEnabledProperty(IAddOptions current) {
		return current.getAddSelectEnabled();
	}

	@Override
	protected StringProperty getIconOverlay(IAddOptions current) {
		return current.getAddSelectIconProperty();
	}

	@Override
	protected String getBaseIconSpec() {
		return IconspecUtils.getIconspec("action/add-select"); //$NON-NLS-1$
	}

	@Override
	protected String getBaseMenuIconSpec() {
		return IconspecUtils.getMenuIconspec("action/add-select"); //$NON-NLS-1$
	}

	@Override
	public String getKey() {
		return "AddSelect";
	}

	// =========================================================================
	// First-subaction display logic
	// =========================================================================

	private void updateFirstSubactionDisplay() {
		// Detach listeners from the previous first subaction.
		if (trackedFirstSubaction != null) {
			if (trackedFirstSubaction instanceof IIconSpecPropertyProvider p) {
				Property<String> iconProp = p.iconSpecProperty();
				if (iconProp != null)
					iconProp.removeListener(firstSubactionPropListener);
				Property<String> menuIconProp = p.menuIconSpecProperty();
				if (menuIconProp != null)
					menuIconProp.removeListener(firstSubactionPropListener);
			}
			ReadOnlyStringProperty textProp = trackedFirstSubaction.textProperty();
			if (textProp != null)
				textProp.removeListener(firstSubactionPropListener);
			trackedFirstSubaction = null;
		}

		IUIAction first = subactions.isEmpty() ? null : subactions.get(0);
		trackedFirstSubaction = first;

		if (first != null) {
			if (first instanceof IIconSpecPropertyProvider p) {
				Property<String> iconProp = p.iconSpecProperty();
				if (iconProp != null)
					iconProp.addListener(firstSubactionPropListener);
				Property<String> menuIconProp = p.menuIconSpecProperty();
				if (menuIconProp != null)
					menuIconProp.addListener(firstSubactionPropListener);
			}
			ReadOnlyStringProperty textProp = first.textProperty();
			if (textProp != null)
				textProp.addListener(firstSubactionPropListener);
		}

		applyFirstSubactionIcon(first);
		applyFirstSubactionMenuIcon(first);
		applyFirstSubactionText(first);
	}

	private void reapplyFirstSubaction() {
		if (effectiveIconSpec == null)
			return;
		applyFirstSubactionIcon(trackedFirstSubaction);
		applyFirstSubactionMenuIcon(trackedFirstSubaction);
		applyFirstSubactionText(trackedFirstSubaction);
	}

	private void applyFirstSubactionIcon(IUIAction first) {
		String value = getIconValue(first);
		effectiveIconSpec.set(isBlankOrNull(value) ? baseIconSpec.getValue() : value);
	}

	private void applyFirstSubactionMenuIcon(IUIAction first) {
		String value = null;
		if (first instanceof IIconSpecPropertyProvider p) {
			Property<String> menuProp = p.menuIconSpecProperty();
			if (menuProp != null)
				value = menuProp.getValue();
			if (isBlankOrNull(value))
				value = getIconValue(first);
		}
		effectiveMenuIconSpec.set(isBlankOrNull(value) ? baseMenuIconSpec.getValue() : value);
	}

	private void applyFirstSubactionText(IUIAction first) {
		String value = null;
		if (first != null) {
			ReadOnlyStringProperty textProp = first.textProperty();
			if (textProp != null)
				value = textProp.getValue();
		}
		effectiveText.set(value);
	}

	private String getIconValue(IUIAction action) {
		if (action instanceof IIconSpecPropertyProvider p) {
			Property<String> iconProp = p.iconSpecProperty();
			return iconProp != null ? iconProp.getValue() : null;
		}
		return null;
	}

	private String getFirstSubactionIconValue() {
		return getIconValue(trackedFirstSubaction);
	}

	private String getFirstSubactionMenuIconValue() {
		if (trackedFirstSubaction instanceof IIconSpecPropertyProvider p) {
			Property<String> menuProp = p.menuIconSpecProperty();
			if (menuProp != null && !isBlankOrNull(menuProp.getValue()))
				return menuProp.getValue();
			return getIconValue(trackedFirstSubaction);
		}
		return null;
	}

	private static boolean isBlankOrNull(String s) {
		return s == null || s.isBlank();
	}
}
