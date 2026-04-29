package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.List;

import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import javafx.scene.Node;

/**
 * Describes a single logical type of node in a {@link TreeEditor}.
 *
 * <p>
 * Implementations define: how to display and recognise nodes of this type, what
 * child types are allowed, how to add/remove children in the model, and
 * optionally how to rename a node inline via an {@link IValueEditorProvider}.
 *
 * @param <N> the application node type managed by the tree
 */
public interface ITreeNodeType<N> {

	/** Label shown in the add-child menu. */
	String getTypeName();

	/** Text rendered in the tree cell for the given node. */
	String getDisplayText(N node);

	/** Optional icon shown in the tree cell; {@code null} = no icon. */
	default Node createIcon(N node) {
		return null;
	}

	/** Returns {@code true} if this type is responsible for the given node. */
	boolean matches(N node);

	/** Child types available under {@code parent}; empty list means leaf node. */
	List<? extends ITreeNodeType<N>> childTypes(N parent);

	List<N> getChildren(N node);

	void addChild(N parent, N child);

	void removeChild(N parent, N child);

	/** Creates a new, default-initialised node of this type. */
	N create();

	/**
	 * Inline editor for this node type (double-click / ENTER to activate). Return
	 * {@code null} to make nodes of this type non-editable inline.
	 *
	 * <p>
	 * The provider type is {@code N}: {@link IValueEditorProvider#createEditor}
	 * receives an {@code ObjectProperty<N>} holding the node being edited. The
	 * editor may be a simple text field or any composite panel that binds directly
	 * to the node's sub-properties.
	 *
	 * <p>
	 * If {@link IValueEditorProvider#supportsDialog()} is {@code true}, a "…"
	 * button is also shown in the {@link TreeEditor} toolbar for the selected node.
	 */
	default IValueEditorProvider<N> inlineEditor() {
		return null;
	}

	/**
	 * Called after the user commits an inline edit or closes the dialog. The node's
	 * properties have already been updated by the editor at this point; use this
	 * hook to fire model events, persist, or validate. ESC (cancel) does NOT call
	 * this method.
	 */
	default void onEditCommitted(N node) {
	}
}
