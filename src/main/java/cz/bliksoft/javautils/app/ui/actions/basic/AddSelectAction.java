package cz.bliksoft.javautils.app.ui.actions.basic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.ui.actions.BasicContextUIAction;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.IUIActionWithSubactions;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IAddOptions;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Context-aware action for {@link IAddOptions}.
 *
 * <p>
 * Maintains a stable {@link #getOptions()} list that mirrors the
 * {@link IUIAction} options provided by the current context value. Bind a
 * toolbar dropdown to this list once — it tracks context changes automatically.
 *
 * <p>
 * {@link #execute(IAddOptions)} runs the first available option, matching the
 * default-click behaviour of a {@code SplitMenuButton}.
 */
public class AddSelectAction extends BasicContextUIAction<IAddOptions> implements IUIActionWithSubactions {

	private static final Logger log = LogManager.getLogger(AddSelectAction.class);

	private final ObservableList<IUIAction> subactions = FXCollections.observableArrayList();
	private ListChangeListener<IUIAction> sourceListener = null;

	public AddSelectAction() {
		super(IAddOptions.class);
		// subactions is now initialized — replay the initial context value that
		// onValueChanged skipped while the field was still null.
		refreshContext();
	}

	@Override
	public ObservableList<IUIAction> getSubactions() {
		return subactions;
	}

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
		return "/icons/base/PLUS_16.png";
	}

	@Override
	public String getKey() {
		return "AddSelect";
	}
}
