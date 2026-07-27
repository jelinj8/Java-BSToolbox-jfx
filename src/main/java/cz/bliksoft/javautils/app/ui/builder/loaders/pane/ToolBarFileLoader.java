package cz.bliksoft.javautils.app.ui.builder.loaders.pane;

import cz.bliksoft.javautils.app.ui.builder.FxAttrHelper;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ToolBar;

public class ToolBarFileLoader extends FileLoader {
	@Override
	public Object loadObject(FileObject file) {
		ToolBar tb = new ToolBar();
		tb.setOrientation(FxAttrHelper.orientation(file, "orientation", Orientation.HORIZONTAL));

		if (Boolean.FALSE.equals(file.getBool("overflowFocusable")))
			suppressOverflowButtonFocus(tb);

		return tb;
	}

	/**
	 * Makes the toolbar's overflow button (shown when the toolbar is too narrow to
	 * fit all items) non-focus-traversable, so it cannot steal focus from input
	 * fields - matching toolbar buttons, which the UI builder makes non-focusable
	 * by default. The button stays fully mouse-operable.
	 *
	 * <p>
	 * CSS cannot do this: {@code ToolBarSkin} calls
	 * {@code setFocusTraversable(true)} on the button programmatically (and again
	 * from a visibility listener), and an API-set value outranks author
	 * stylesheets. The button is therefore looked up when the skin is attached and
	 * a listener keeps the value pinned to {@code false}.
	 */
	public static void suppressOverflowButtonFocus(ToolBar tb) {
		tb.skinProperty().addListener((obs, oldSkin, skin) -> {
			if (skin == null)
				return;
			Node overflow = tb.lookup(".tool-bar-overflow-button");
			if (overflow == null)
				return;
			overflow.setFocusTraversable(false);
			overflow.focusTraversableProperty().addListener((o, was, is) -> {
				if (Boolean.TRUE.equals(is))
					overflow.setFocusTraversable(false);
			});
		});
	}

	@Override
	public String getSupportedType() {
		return "ToolBar";
	}
}
