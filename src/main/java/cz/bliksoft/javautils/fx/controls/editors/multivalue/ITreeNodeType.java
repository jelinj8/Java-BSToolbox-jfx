package cz.bliksoft.javautils.fx.controls.editors.multivalue;

import java.util.List;

import cz.bliksoft.javautils.app.ui.interfaces.IIconSpecPropertyProvider;
import cz.bliksoft.javautils.app.ui.interfaces.ITitleProvider;
import cz.bliksoft.javautils.fx.controls.editors.IValueEditorProvider;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.stage.Window;

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
public interface ITreeNodeType<N> extends ITitleProvider {

	/** Label shown in the add-child menu. */
	String getTypeName();

	/**
	 * Defaults to {@link #getTypeName()} so existing implementors need no change.
	 */
	@Override
	default String getTitle() {
		return getTypeName();
	}

	/** Text rendered in the tree cell for the given node. */
	String getDisplayText(N node);

	/**
	 * Optional icon shown in the tree cell; {@code null} = no icon.
	 *
	 * <p>
	 * The default implementation checks whether {@code node} implements
	 * {@link IIconSpecPropertyProvider} and, if so, resolves its iconSpec via
	 * {@link ImageUtils#getIconNode}. Override to supply a custom icon or to
	 * suppress the default behaviour.
	 */
	default Node createIcon(N node) {
		if (node instanceof IIconSpecPropertyProvider p) {
			String spec = p.iconSpecProperty().getValue();
			if (spec != null && !spec.isBlank())
				return ImageUtils.getIconNode(spec);
		}
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
	 * {@code null} for dialog-only types — the tree will call {@link #showDialog}
	 * instead when the node is activated.
	 *
	 * <p>
	 * The provider type is {@code N}: {@link IValueEditorProvider#createEditor}
	 * receives an {@code ObjectProperty<N>} holding the node being edited. The
	 * editor may be a simple text field or any composite panel that binds directly
	 * to the node's sub-properties.
	 */
	default IValueEditorProvider<N> inlineEditor() {
		return null;
	}

	/**
	 * Returns {@code true} if this node type supports a full dialog editor (shown
	 * via the "…" toolbar button, ENTER, or double-click when
	 * {@link #inlineEditor()} is {@code null}).
	 *
	 * <p>
	 * The default delegates to {@link IValueEditorProvider#supportsDialog()} on the
	 * inline editor. Override independently to support dialog-only types where
	 * {@link #inlineEditor()} returns {@code null}.
	 */
	default boolean supportsDialog() {
		IValueEditorProvider<N> ed = inlineEditor();
		return ed != null && ed.supportsDialog();
	}

	/**
	 * Opens the full dialog editor for the given node. Called by the tree when the
	 * "…" button is pressed, or when ENTER / double-click activates a node whose
	 * {@link #inlineEditor()} returns {@code null}.
	 *
	 * <p>
	 * The default delegates to {@link IValueEditorProvider#showDialog} on the
	 * inline editor and then calls {@link #onEditCommitted}. Override for
	 * dialog-only types.
	 *
	 * @param owner owning window for the dialog, may be {@code null}
	 * @param node  the node to edit
	 */
	default void showDialog(Window owner, N node) {
		IValueEditorProvider<N> ed = inlineEditor();
		if (ed == null || !ed.supportsDialog())
			return;
		ObjectProperty<N> prop = new SimpleObjectProperty<>(node);
		ed.showDialog(owner, prop);
		onEditCommitted(prop.get());
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
