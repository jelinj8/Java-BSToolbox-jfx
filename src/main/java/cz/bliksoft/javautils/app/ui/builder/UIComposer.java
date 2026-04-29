package cz.bliksoft.javautils.app.ui.builder;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.ui.actions.ActionBinder;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.IUIActionWithSubactions;
import cz.bliksoft.javautils.app.ui.actions.UIActions;
import cz.bliksoft.javautils.exceptions.InitializationException;
import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class UIComposer {
	static Logger log = LogManager.getLogger();

	private UIComposer() {
	}

	public sealed interface UiProduct
			permits UiNodeProduct, UiSceneProduct, UiMenuProduct, UiMenuItemProduct, UiContextMenuProduct {
		default boolean isNode() {
			return this instanceof UiNodeProduct;
		}

		default boolean isScene() {
			return this instanceof UiSceneProduct;
		}

		Node getNodeContext();
	}

	public record UiNodeProduct(javafx.scene.Node node) implements UiProduct {
		@Override
		public Node getNodeContext() {
			return node;
		}
	}

	public record UiMenuProduct(javafx.scene.control.Menu menu) implements UiProduct {
		@Override
		public Node getNodeContext() {
			throw new IllegalStateException("Menu is not a Node");
		}
	}

	public record UiSceneProduct(javafx.scene.Scene scene) implements UiProduct {
		@Override
		public Node getNodeContext() {
			if (scene.getRoot() == null)
				throw new IllegalStateException("Scene has no root yet");
			return scene.getRoot();
		}
	}

	public record UiMenuItemProduct(javafx.scene.control.MenuItem item) implements UiProduct {
		@Override
		public Node getNodeContext() {
			throw new IllegalStateException("MenuItem is not a Node");
		}
	}

	public record UiContextMenuProduct(javafx.scene.control.ContextMenu menu) implements UiProduct {
		@Override
		public Node getNodeContext() {
			throw new IllegalStateException("ContextMenu is not a Node");
		}
	}

	private static UiProduct toUiProduct(Object o) {
		if (o instanceof UiProduct p)
			return p;
		if (o instanceof javafx.scene.Node n)
			return new UiNodeProduct(n);
		if (o instanceof javafx.scene.Scene s)
			return new UiSceneProduct(s);
		if (o instanceof javafx.scene.control.Menu m)
			return new UiMenuProduct(m);
		if (o instanceof javafx.scene.control.MenuItem mi)
			return new UiMenuItemProduct(mi);
		throw new IllegalArgumentException(
				"Loader returned unsupported UI object: " + (o == null ? "null" : o.getClass().getName()));
	}

	public static void buildUI(FileObject file, Stage stage) {
		UIBuildContext ctx = new UIBuildContext();
		UiProduct product;
		try {
			product = build(file, ctx);
		} catch (Exception e) {
			if (ctx.currentBuildObject == null)
				throw new InitializationException("Failed to build UI", e);
			else
				throw new InitializationException(
						"Failed to build UI, failed on " + ctx.currentBuildObject.getFullPath(), e);
		}
		attachToStage(stage, product, file, ctx);
	}

	private static void attachToStage(Stage stage, UiProduct product, FileObject file, UIBuildContext ctx) {
		if (product instanceof UiSceneProduct sp) {
			stage.setScene(sp.scene());
			ctx.accelerators().attach(sp.scene());
			return;
		}

		Node node = product.getNodeContext();
		Parent root = (node instanceof Parent p) ? p : new StackPane(node);
		Scene scn = new Scene(root);
		applySceneAttribs(scn, file);

		// Title from attributes
		String title = file.getAttribute("title", null);
		if (title != null)
			stage.setTitle(title);

		// Icons from attributes (e.g. iconBase="/icons/app")
		String iconBase = file.getAttribute("iconBase", null);
		if (iconBase != null)
			setStageIcons(stage, iconBase);

		stage.setScene(scn);

		IUIAction closeAction = UIActions.getAction("AppClose");
		if (closeAction != null) {
			stage.setOnCloseRequest(e -> {
				e.consume();
				closeAction.execute();
			});
		}

		ctx.accelerators().attach(scn);
	}

	private static void setStageIcons(Stage stage, String basePath) {
		int[] sizes = { 16, 32, 48, 256 };
		for (int s : sizes) {
			String path = String.format("%s%d.png", basePath, s);
			Image i = ImageUtils.getImageIfPossible(path, false);
			if (i != null)
				stage.getIcons().add(i);
		}
	}

	// private static UiProduct buildUIProduct(FileObject entry, UIBuildContext ctx)
	// throws Exception {
	// UiProduct result = build(entry, ctx);
	// return result;
	// }

	private static UiProduct loadUIProduct(FileObject file) {
		Object o = FileLoader.loadFile(file);
		if (o == null) {
			throw new InitializationException("Failed to load UI item from file " + file.getFullPath());
		}
		return toUiProduct(o);
	}

	private static UiProduct build(FileObject entry, UIBuildContext ctx) throws Exception {
		try {
			log.debug("Building UI for " + entry.getName());
			ctx.currentBuildObject = entry;

			UiProduct parent = loadUIProduct(entry);

			if (parent instanceof UiSceneProduct sp) {
				// optional: allow scene-level CSS, fill, etc. here
				applySceneAttribs(sp.scene(), entry);

				Parent root = sp.scene().getRoot();
				if (root != null) {
					applyNodeAttribs(root, entry, ctx);
				}

				return parent;
			}

			// ContextMenu: children must be MenuItem/Menu (submenu)
			if (parent instanceof UiContextMenuProduct cmp) {
				for (FileObject childFile : entry.getChildren()) {
					UiProduct child = build(childFile, ctx);
					MenuItem item = requireMenuItem(child, childFile, entry, "ContextMenu");
					bindMenuActionIfPresent(item, childFile, ctx);
					cmp.menu().getItems().add(item);
				}
				return parent;
			}

			// Menu: children are MenuItem (Menu is also MenuItem)
			if (parent instanceof UiMenuProduct mp) {
				for (FileObject childFile : entry.getChildren()) {
					UiProduct child = build(childFile, ctx);
					MenuItem item = requireMenuItem(child, childFile, entry, "Menu");
					if (item instanceof RadioMenuItem) {
						String group = childFile.getAttribute("group");
						if (group != null)
							((RadioMenuItem) item).setToggleGroup(ctx.toggleGoupResolver.getGroup(group));
					}
					bindMenuActionIfPresent(item, childFile, ctx);
					mp.menu().getItems().add(item);
				}
				bindMenuVisibility(mp.menu());
				return parent;
			}

			// MenuItem: only Menu can have children
			if (parent instanceof UiMenuItemProduct mip) {
				if (mip.item() instanceof javafx.scene.control.Menu submenu) {
					for (FileObject childFile : entry.getChildren()) {
						UiProduct child = build(childFile, ctx);
						MenuItem item = requireMenuItem(child, childFile, entry, "Menu");
						bindMenuActionIfPresent(item, childFile, ctx);
						submenu.getItems().add(item);
					}
					bindMenuVisibility(submenu);
					return parent;
				} else {
					if (!entry.getChildren().isEmpty())
						throw new IllegalArgumentException("MenuItem cannot have children: " + entry.getName());
					return parent;
				}
			}

			Node n = parent.getNodeContext();
			applyNodeAttribs(n, entry, ctx);

			if (n instanceof Region)
				applyCommon((Region) n, entry);

			if (n instanceof Labeled)
				applyLabeled((Labeled) n, entry);

			if (n instanceof Control)
				applyControl((Control) n, entry);

			if (n instanceof TextInputControl)
				applyTextInputControl((TextInputControl) n, entry);

			if (n instanceof RadioButton) {
				String group = entry.getAttribute("group");
				if (group != null)
					((RadioButton) n).setToggleGroup(ctx.toggleGoupResolver.getGroup(group));
			}

			for (FileObject childFile : entry.getChildren()) {
				UiProduct child = build(childFile, ctx);

				if (parent.getNodeContext() instanceof javafx.scene.control.MenuBar mb) {
					mb.getMenus().add(requireMenu(child, childFile, entry, "MenuBar"));
					continue;
				}
				if (parent.getNodeContext() instanceof javafx.scene.control.MenuButton mbtn) {
					MenuItem item = requireMenuItem(child, childFile, entry, "MenuButton");
					bindMenuActionIfPresent(item, childFile, ctx);
					mbtn.getItems().add(item);
					continue;
				}
				if (parent.getNodeContext() instanceof javafx.scene.control.SplitMenuButton smb) {
					MenuItem item = requireMenuItem(child, childFile, entry, "SplitMenuButton");
					bindMenuActionIfPresent(item, childFile, ctx);
					smb.getItems().add(item);
					continue;
				}

				attach(parent.getNodeContext(), entry, child.getNodeContext(), childFile, ctx);
			}
			return parent;
		} finally {
			UIBuildContextHolder.clear();
		}
	}

	// private static Node buildNode(FileObject entry, UIBuildContext ctx) throws
	// Exception {
	// UiProduct p = buildUIProduct(entry, ctx);
	// if (p instanceof UiSceneProduct)
	// throw new IllegalArgumentException("Nested Scene not allowed: " +
	// entry.getName());
	// return p.getNodeContext();
	// }

	private static void applySceneAttribs(Scene scene, FileObject definition) {
		String styles = definition.getAttribute("stylesheets", null);
		if (styles != null && !styles.isBlank()) {
			for (String s : styles.split(","))
				scene.getStylesheets().add(s.trim());
		}
	}

	public static void applyNodeAttribs(Node node, FileObject definition, UIBuildContext ctx) {
		String className = definition.getAttribute("FXclass");
		if (className != null)
			node.getStyleClass().add(className);

		String classes = definition.getAttribute("FXclasses");
		if (classes != null) {
			for (String c : classes.split("\\s+")) {
				if (!c.isBlank())
					node.getStyleClass().add(c);
			}
		}

		Boolean disabled = definition.getBool("disabled");
		if (disabled != null)
			node.setDisable(disabled);

		Boolean visible = definition.getBool("visible");
		if (visible != null)
			node.setVisible(visible);

		Boolean managed = definition.getBool("managed");
		if (managed != null)
			node.setManaged(managed);

		String id = definition.getAttribute("id", null);
		if (id != null)
			node.setId(id);

		Boolean focusTraversable = definition.getBool("focusTraversable");
		if (focusTraversable != null)
			node.setFocusTraversable(focusTraversable);

		Double opacity = definition.getDouble("opacity", null);
		if (opacity != null)
			node.setOpacity(opacity);

		String cursor = definition.getAttribute("cursor", null);
		if (cursor != null) {
			Cursor c = FxAttrHelper.parseCursor(cursor);
			if (c != null)
				node.setCursor(c);
		}

		bindActionIfPresent(node, definition, ctx);
	}

	public static void applyControl(Control c, FileObject f) {
		String tt = f.getAttribute("tooltip");
		if (tt != null)
			c.setTooltip(new Tooltip(tt));
	}

	public static void applyTextInputControl(TextInputControl c, FileObject f) {
		Font fnt = FxAttrHelper.getFont(f);
		if (fnt != null)
			c.setFont(fnt);

		Boolean b = f.getBool("editable");
		if (b != null)
			c.setEditable(b);

		String prompt = f.getAttribute("prompt");
		if (prompt != null)
			c.setPromptText(prompt);
	}

	public static void applyCommon(Region r, FileObject f) {
		// sizing
		if (f.getAttribute("prefWidth", null) != null)
			r.setPrefWidth(FxAttrHelper.sizeInfPref(f.getAttribute("prefWidth"), r.getPrefWidth()));
		if (f.getAttribute("prefHeight", null) != null)
			r.setPrefHeight(FxAttrHelper.sizeInfPref(f.getAttribute("prefHeight"), r.getPrefHeight()));
		if (f.getAttribute("minWidth", null) != null)
			r.setMinWidth(FxAttrHelper.sizeInfPref(f.getAttribute("minWidth"), r.getMinWidth()));
		if (f.getAttribute("minHeight", null) != null)
			r.setMinHeight(FxAttrHelper.sizeInfPref(f.getAttribute("minHeight"), r.getMinHeight()));
		if (f.getAttribute("maxWidth", null) != null)
			r.setMaxWidth(FxAttrHelper.sizeInfPref(f.getAttribute("maxWidth"), r.getMaxWidth()));
		if (f.getAttribute("maxHeight", null) != null)
			r.setMaxHeight(FxAttrHelper.sizeInfPref(f.getAttribute("maxHeight"), r.getMaxHeight()));

		Insets p = FxAttrHelper.insets(f, "padding");
		if (p != null)
			r.setPadding(p);
	}

	public static void applyLabeled(Labeled l, FileObject f) {
		String text = f.getAttribute("text", null);
		if (text != null)
			l.setText(text);

		Font fnt = FxAttrHelper.getFont(f);
		if (fnt != null)
			l.setFont(fnt);

		Boolean wrap = f.getBool("wrap");
		if (wrap != null)
			l.setWrapText(wrap);

		String iconDef = f.getAttribute("icon");
		if (iconDef != null) {
			l.setGraphic(ImageUtils.getIconNode(iconDef));
		}

		Paint textFill = FxAttrHelper.getPaint(f, "textFill"); // nebo "fill" pokud chceš alias
		if (textFill != null)
			l.setTextFill(textFill);

		Pos p = FxAttrHelper.pos(f, "alignment", null);
		if (p != null)
			l.setAlignment(p);

		String cd = f.getAttribute("contentDisplay", null);
		if (cd != null) {
			ContentDisplay d = FxAttrHelper.parseContentDisplay(cd);
			if (d != null)
				l.setContentDisplay(d);
		}

		Double gap = f.getDouble("graphicTextGap", null);
		if (gap != null)
			l.setGraphicTextGap(gap);
	}

	// public static void applyPane(Pane p, FileObject f) {
	// // (nothing universal here beyond Region sizing, which Pane inherits)
	// }

	private static void attach(Node parent, FileObject parentEntry, Node child, FileObject childEntry,
			UIBuildContext ctx) {

		// (1) slot targeting
		String into = childEntry.getAttribute("into", null);
		if (into != null && !into.isBlank()) {
			Node slot = ctx.slotResolver.resolveSlot(parent, into);
			if (slot == null) {
				if (parentEntry != null)
					throw new IllegalArgumentException("Slot not found: " + into + " in " + parentEntry.getName());
				else
					throw new IllegalArgumentException("Slot not found: " + into + " with no parent ");
			}
			attachToContainer(slot, child, childEntry, ctx);
			return;
		}

		attachByParentType(parent, parentEntry, child, childEntry);
	}

	private static void attachToContainer(Node container, Node child, FileObject childEntry, UIBuildContext ctx) {
		attach(container, null, child, childEntry, ctx);
	}

	private static void attachByParentType(Node parent, FileObject parentEntry, Node child, FileObject childEntry) {

		// TabPane: each child becomes a Tab (content = child)
		if (parent instanceof TabPane tp) {
			tp.getTabs().add(makeTab(child, childEntry));
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		// GridPane: add + constraints
		if (parent instanceof GridPane gp) {
			gp.getChildren().add(child);
			applyGridConstraints(child, childEntry);
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		// HBox / VBox: both are Pane, but apply grow constraints here explicitly
		if (parent instanceof HBox hb) {
			hb.getChildren().add(child);
			applyHBoxConstraints(child, childEntry);
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}
		if (parent instanceof VBox vb) {
			vb.getChildren().add(child);
			applyVBoxConstraints(child, childEntry);
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		// Generic Pane: add child
		// if (parent instanceof Pane p) {
		// p.getChildren().add(child);
		// applyCommonChildConstraints(parent, child, childEntry);
		// return;
		// }

		// Group: add child
		if (parent instanceof Group g) {
			g.getChildren().add(child);
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		// BorderPane: attach by region
		if (parent instanceof BorderPane bp) {
			String region = childEntry.getAttribute("region", "center");
			setBorderRegion(bp, region, child, childEntry);
			return;
		}

		// SplitPane: add item
		if (parent instanceof SplitPane sp) {
			sp.getItems().add(child);
			// Optional: divider positions can be set by parent attrs, not per-child
			// typically
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		// ScrollPane: single content
		if (parent instanceof ScrollPane sp) {
			setSingleContent("ScrollPane.content", sp.getContent(), sp::setContent, child, childEntry, parentEntry);
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		// Accordion: children become TitledPane items (content = child)
		if (parent instanceof Accordion acc) {
			acc.getPanes().add(wrapAsTitledPane(child, childEntry));
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		// TitledPane: single content
		if (parent instanceof TitledPane tp) {
			setSingleContent("TitledPane.content", tp.getContent(), tp::setContent, child, childEntry, parentEntry);
			// optional: allow child to set text if not already set? (up to you)
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		// ToolBar: add to items (Node list)
		if (parent instanceof ToolBar tb) {
			tb.getItems().add(child);
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		if (parent instanceof AnchorPane ap) {
			ap.getChildren().add(child);
			applyAnchorConstraints(child, childEntry);
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

		String parentName = (parentEntry != null ? parentEntry.getName() : "<container>");
		throw new IllegalArgumentException(
				"Cannot attach child to: " + parent.getClass().getName() + " (" + parentName + ")");
	}

	// ------------------- BorderPane helpers -------------------

	private static void setBorderRegion(BorderPane bp, String region, Node child, FileObject childEntry) {
		String r = region == null ? "center" : region.trim().toLowerCase();
		switch (r) {
		case "top" -> bp.setTop(child);
		case "left" -> bp.setLeft(child);
		case "center" -> bp.setCenter(child);
		case "right" -> bp.setRight(child);
		case "bottom" -> bp.setBottom(child);
		default ->
			throw new IllegalArgumentException("Unknown BorderPane region: " + region + " for " + childEntry.getName());
		}

		// BorderPane supports margin per child
		Insets m = FxAttrHelper.parseInsets(childEntry, "margin");
		if (m != null)
			BorderPane.setMargin(child, m);

		// Optional alignment inside the region (Pos enum)
		Pos pos = FxAttrHelper.pos(childEntry, "alignment", null);
		if (pos != null)
			BorderPane.setAlignment(child, pos);
	}

	// ------------------- TabPane helpers -------------------

	private static Tab makeTab(Node content, FileObject tabDef) {
		String text = tabDef.getAttribute("text", null);
		if (text == null || text.isBlank())
			text = tabDef.getName();

		Tab tab = new Tab(text, content);

		boolean closable = tabDef.getBool("closable", false);
		tab.setClosable(closable);

		boolean disable = tabDef.getBool("disable", false);
		tab.setDisable(disable);

		// Optional: tooltip
		// String tip = tabDef.getAttribute("tooltip", null);
		// if (tip != null) tab.setTooltip(new Tooltip(tip));

		return tab;
	}

	// ------------------- Common child constraints -------------------

	private static void applyCommonChildConstraints(Node parent, Node child, FileObject def) {

		// Optional maxWidth/maxHeight for resizable nodes
		if (child instanceof Region r) {
			Double mw = def.getDouble("maxWidth", null);
			if (mw != null)
				r.setMaxWidth(mw);

			Double mh = def.getDouble("maxHeight", null);
			if (mh != null)
				r.setMaxHeight(mh);

			Boolean focusable = def.getBool("focusable", parent instanceof ToolBar ? false : null);
			if (focusable != null)
				child.setFocusTraversable(focusable);
		}
	}

	// ------------------- GridPane helpers -------------------

	private static void applyGridConstraints(Node child, FileObject def) {
		Integer row = def.getInteger("grid.row", null);
		Integer col = def.getInteger("grid.col", null);
		Integer rowSpan = def.getInteger("grid.rowSpan", null);
		Integer colSpan = def.getInteger("grid.colSpan", null);

		if (row != null)
			GridPane.setRowIndex(child, row);
		if (col != null)
			GridPane.setColumnIndex(child, col);
		if (rowSpan != null)
			GridPane.setRowSpan(child, rowSpan);
		if (colSpan != null)
			GridPane.setColumnSpan(child, colSpan);

		// Optional hgrow/vgrow as Priority enum
		Priority hg = FxAttrHelper.growPriority(def, "grid.hgrow");
		Priority vg = FxAttrHelper.growPriority(def, "grid.vgrow");
		if (hg != null)
			GridPane.setHgrow(child, hg);
		if (vg != null)
			GridPane.setVgrow(child, vg);

		// Optional halign/valign
		HPos halign = FxAttrHelper.hPos(def, "grid.halign");
		VPos valign = FxAttrHelper.vPos(def, "grid.valign");
		if (halign != null)
			GridPane.setHalignment(child, halign);
		if (valign != null)
			GridPane.setValignment(child, valign);

		Insets m = FxAttrHelper.parseInsets(def, "margin");
		if (m != null)
			GridPane.setMargin(child, m);

		// Optional "fillWidth/fillHeight" isn’t directly a GridPane constraint; you
		// typically control via hgrow/vgrow and max size.
	}

	// ------------------- HBox / VBox helpers -------------------

	private static void applyHBoxConstraints(Node child, FileObject def) {
		Priority hgrow = FxAttrHelper.growPriority(def, "hgrow");
		if (hgrow != null)
			HBox.setHgrow(child, hgrow);

		Insets m = FxAttrHelper.parseInsets(def, "margin");
		if (m != null)
			HBox.setMargin(child, m);
	}

	private static void applyVBoxConstraints(Node child, FileObject def) {
		Priority vgrow = FxAttrHelper.growPriority(def, "vgrow");
		if (vgrow != null)
			VBox.setVgrow(child, vgrow);

		Insets m = FxAttrHelper.parseInsets(def, "margin");
		if (m != null)
			VBox.setMargin(child, m);
	}

	private static void setSingleContent(String slotName, Node existingContent, Consumer<Node> setter, Node newContent,
			FileObject childEntry, FileObject parentEntry) {
		boolean replace = childEntry.getBool("replace", false);
		boolean clear = childEntry.getBool("clear", false);

		if (clear) {
			setter.accept(null);
			existingContent = null;
		}

		if (existingContent != null && !replace) {
			String parentName = parentEntry != null ? parentEntry.getName() : "<container>";
			throw new IllegalArgumentException(slotName + " already set in " + parentName
					+ ". Add replace=\"true\" (or clear=\"true\") on child " + childEntry.getName());
		}

		setter.accept(newContent);

		// Optional: margin for content in ScrollPane / TitledPane isn't a thing on the
		// parent;
		// you can wrap into StackPane with padding if you want such feature.
	}

	private static TitledPane wrapAsTitledPane(Node content, FileObject def) {
		String text = def.getAttribute("text", null);
		if (text == null || text.isBlank())
			text = def.getName();

		TitledPane tp = new TitledPane(text, content);

		boolean expanded = def.getBool("expanded", false);
		tp.setExpanded(expanded);

		boolean collapsible = def.getBool("collapsible", true);
		tp.setCollapsible(collapsible);

		// Optional: graphic via separate loader etc. (not here)
		return tp;
	}

	private static void applyAnchorConstraints(Node child, FileObject childEntry) {
		Double top = childEntry.getDouble("anchor.top", null);
		Double right = childEntry.getDouble("anchor.right", null);
		Double bottom = childEntry.getDouble("anchor.bottom", null);
		Double left = childEntry.getDouble("anchor.left", null);

		if (top != null)
			AnchorPane.setTopAnchor(child, top);
		if (right != null)
			AnchorPane.setRightAnchor(child, right);
		if (bottom != null)
			AnchorPane.setBottomAnchor(child, bottom);
		if (left != null)
			AnchorPane.setLeftAnchor(child, left);
	}

	private static void bindActionIfPresent(Node node, FileObject entry, UIBuildContext ctx) {
		String actionKey = entry.getAttribute("action", null);
		if (actionKey == null || actionKey.isBlank())
			return;

		IUIAction a = UIActions.getAction(actionKey.trim());

		if (node instanceof javafx.scene.control.ButtonBase bb) {
			if (a != null) {
				// SplitMenuButton must be checked before MenuButton (it extends it).
				// IUIActionWithSubactions gets special treatment to wire the dropdown items.
				if (bb instanceof javafx.scene.control.SplitMenuButton smb
						&& a instanceof IUIActionWithSubactions aws) {
					ActionBinder.bind(smb, aws);
				} else if (bb instanceof javafx.scene.control.MenuButton mb
						&& a instanceof IUIActionWithSubactions aws) {
					ActionBinder.bind(mb, aws);
				} else {
					ActionBinder.bind(bb, a);
				}
				ctx.accelerators().bind(a);
				return;
			} else {
				throw new InitializationException(String.format("Action not found: %s for %s. Available actions:\n%s",
						actionKey.trim(), entry, UIActions.dumpActions()));
			}
		}

		if (node instanceof javafx.scene.control.Hyperlink hl) {
			if (a != null) {
				ActionBinder.bind(hl, a);
				return;
			} else {
				String url = entry.getAttribute("url", null);
				if (url == null || url.isBlank())
					return;

				if (hl.getOnAction() != null)
					return;

				hl.setOnAction(e -> {
					if (BSApp.getApplication() != null) {
						BSApp.getApplication().getHostServices().showDocument(url.trim());
					} else {
						throw new InitializationException("HostServices not set, cannot open URL: " + url);
					}
				});
			}
		}
	}

	private static void bindMenuActionIfPresent(javafx.scene.control.MenuItem mi, FileObject entry,
			UIBuildContext ctx) {
		String actionKey = entry.getAttribute("action", null);
		if (actionKey == null || actionKey.isBlank())
			return;

		IUIAction a = UIActions.getAction(actionKey.trim());
		ActionBinder.bind(mi, a);
	}

	private static void bindMenuVisibility(javafx.scene.control.Menu menu) {
		// visible if ANY child MenuItem is visible
		javafx.beans.binding.BooleanBinding anyChildVisible = javafx.beans.binding.Bindings.createBooleanBinding(
				() -> menu.getItems().stream().anyMatch(javafx.scene.control.MenuItem::isVisible),
				menu.getItems().stream().map(javafx.scene.control.MenuItem::visibleProperty)
						.toArray(javafx.beans.Observable[]::new));

		menu.visibleProperty().bind(anyChildVisible);
	}

	private static javafx.scene.control.MenuItem requireMenuItem(UiProduct p, FileObject childEntry,
			FileObject parentEntry, String parentType) {

		if (p instanceof UiMenuItemProduct mip)
			return mip.item();
		if (p instanceof UiMenuProduct mp)
			return mp.menu(); // Menu is a MenuItem

		throw new IllegalArgumentException("Expected MenuItem under " + parentType + " in " + parentEntry.getName()
				+ ", got " + p.getClass().getSimpleName() + " from " + childEntry.getName());
	}

	private static javafx.scene.control.Menu requireMenu(UiProduct p, FileObject childEntry, FileObject parentEntry,
			String parentType) {

		if (p instanceof UiMenuProduct mp)
			return mp.menu();
		if (p instanceof UiMenuItemProduct mip && mip.item() instanceof javafx.scene.control.Menu m)
			return m;

		throw new IllegalArgumentException("Expected Menu under " + parentType + " in " + parentEntry.getName()
				+ ", got " + p.getClass().getSimpleName() + " from " + childEntry.getName());
	}
}