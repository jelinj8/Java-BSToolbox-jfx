package cz.bliksoft.javautils.app.ui.widgets;

/**
 * Factory metadata and instantiation contract for a widget type.
 *
 * <p>
 * Register an implementation by adding a {@code <file>} entry whose name is the
 * fully-qualified class name under {@code core/widgets} in any module's
 * XmlFilesystem XML:
 *
 * <pre>{@code
 * <file name="core">
 *     <file name="widgets">
 *         <file name="com.example.widgets.ClockWidgetFactory"/>
 *     </file>
 * </file>
 * }</pre>
 *
 * Implementations must have a public no-arg constructor.
 */
public interface IWidgetFactory {

	/**
	 * Unique identifier for this widget type, typically the fully-qualified class
	 * name. Used as the persistence key when saving the user's widget selection.
	 */
	String getName();

	/**
	 * Category key used to group widgets in the selector tree. The value is shown
	 * as-is in the tree header; localise it yourself if needed.
	 */
	String getCategory();

	/** Localised display name shown in the widget selector. */
	String getLocalizedName();

	/**
	 * Minimum width required by this widget, in pixels. Return {@code 0} if there
	 * is no minimum. {@link WidgetContainer} filters out widgets that do not fit
	 * its configured {@link WidgetContainer#getConstrainedMaxWidth() maxWidth}.
	 */
	default double getMinWidth() {
		return 0;
	}

	/**
	 * Minimum height required by this widget, in pixels. Return {@code 0} if there
	 * is no minimum.
	 */
	default double getMinHeight() {
		return 0;
	}

	/** Creates and returns a new instance of the widget. */
	IWidget create();
}
