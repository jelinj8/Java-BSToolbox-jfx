package cz.bliksoft.javautils.app.ui.builder;

import java.util.function.Consumer;

import cz.bliksoft.javautils.app.ui.actions.ActionBinder;
import cz.bliksoft.javautils.app.ui.actions.IUIAction;
import cz.bliksoft.javautils.app.ui.actions.IconBinder;
import cz.bliksoft.javautils.fx.controls.images.ImageUtils;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class UIComposer {

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
		Object o = FileLoader.loadFile(file);
		attachToStage(stage, toUiProduct(o), file, new UIBuildContext());
	}

	private static UiProduct loadUIProduct(FileObject file) {
		Object o = FileLoader.loadFile(file);
		return toUiProduct(o);
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
		stage.setScene(scn);

		// Title from attributes
		String title = file.getAttribute("title", null);
		if (title != null)
			stage.setTitle(title);

		// Icons from attributes (e.g. iconBase="/icons/app")
		String iconBase = file.getAttribute("iconBase", null);
		if (iconBase != null)
			setStageIcons(stage, iconBase);

		ctx.accelerators().attach(scn);
	}

	public static void setStageIcons(Stage stage, String basePath) {
		int[] sizes = { 16, 32, 48, 256 };
		for (int s : sizes) {
			String path = String.format("%s-%d.png", basePath, s);
			Image i = ImageUtils.getImageIfPossible(path);
			if (i != null)
				stage.getIcons().add(i);
		}
	}

	public static UiProduct buildUI(FileObject entry, UIBuildContext ctx) throws Exception {
		UiProduct result = build(entry, ctx);
		return result;
	}

	private static UiProduct build(FileObject entry, UIBuildContext ctx) throws Exception {
		UIBuildContextHolder.set(ctx);
		try {
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
						submenu.getItems().add(requireMenuItem(child, childFile, entry, "Menu"));
					}
					return parent;
				} else {
					if (!entry.getChildren().isEmpty())
						throw new IllegalArgumentException("MenuItem cannot have children: " + entry.getName());
					return parent;
				}
			}

			applyNodeAttribs(parent.getNodeContext(), entry, ctx);

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

	public static Node buildNode(FileObject entry, UIBuildContext ctx) throws Exception {
		UiProduct p = buildUI(entry, ctx);
		if (p instanceof UiSceneProduct)
			throw new IllegalArgumentException("Nested Scene not allowed: " + entry.getName());
		return p.getNodeContext();
	}

	private static void applyNodeAttribs(Node node, FileObject definition, UIBuildContext ctx) {
		String className = definition.getAttribute("class", null);
		if (className != null)
			node.getStyleClass().add(className);

		String classes = definition.getAttribute("classes", null);
		if (classes != null) {
			for (String c : classes.split("\\s+")) {
				if (!c.isBlank())
					node.getStyleClass().add(c);
			}
		}

		bindActionIfPresent(node, definition, ctx);

	}

	private static void applySceneAttribs(Scene scene, FileObject definition) {
		String styles = definition.getAttribute("stylesheets", null);
		if (styles != null && !styles.isBlank()) {
			for (String s : styles.split(","))
				scene.getStylesheets().add(s.trim());
		}
	}

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
		if (parent instanceof Pane p) {
			p.getChildren().add(child);
			applyCommonChildConstraints(parent, child, childEntry);
			return;
		}

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
		Insets m = parseInsets(childEntry.getAttribute("margin", null));
		if (m != null)
			BorderPane.setMargin(child, m);

		// Optional alignment inside the region (Pos enum)
		String align = childEntry.getAttribute("alignment", null);
		if (align != null) {
			Pos pos = parsePos(align);
			if (pos != null)
				BorderPane.setAlignment(child, pos);
		}
	}

	// ------------------- TabPane helpers -------------------

	private static Tab makeTab(Node content, FileObject tabDef) {
		String text = tabDef.getAttribute("text", null);
		if (text == null || text.isBlank())
			text = tabDef.getName();

		Tab tab = new Tab(text, content);

		boolean closable = parseBoolean(tabDef.getAttribute("closable", "false"), false);
		tab.setClosable(closable);

		boolean disable = parseBoolean(tabDef.getAttribute("disable", "false"), false);
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
			String mw = def.getAttribute("maxWidth", null);
			if (mw != null)
				r.setMaxWidth(parseSize(mw, r.getMaxWidth()));

			String mh = def.getAttribute("maxHeight", null);
			if (mh != null)
				r.setMaxHeight(parseSize(mh, r.getMaxHeight()));
		}
	}

	// ------------------- GridPane helpers -------------------

	private static void applyGridConstraints(Node child, FileObject def) {
		Integer row = parseIntOrNull(def.getAttribute("grid.row", null));
		Integer col = parseIntOrNull(def.getAttribute("grid.col", null));
		Integer rowSpan = parseIntOrNull(def.getAttribute("grid.rowSpan", null));
		Integer colSpan = parseIntOrNull(def.getAttribute("grid.colSpan", null));

		if (row != null)
			GridPane.setRowIndex(child, row);
		if (col != null)
			GridPane.setColumnIndex(child, col);
		if (rowSpan != null)
			GridPane.setRowSpan(child, rowSpan);
		if (colSpan != null)
			GridPane.setColumnSpan(child, colSpan);

		// Optional hgrow/vgrow as Priority enum
		Priority hg = parsePriority(def.getAttribute("grid.hgrow", null));
		Priority vg = parsePriority(def.getAttribute("grid.vgrow", null));
		if (hg != null)
			GridPane.setHgrow(child, hg);
		if (vg != null)
			GridPane.setVgrow(child, vg);

		// Optional halign/valign
		HPos halign = parseHPos(def.getAttribute("grid.halign", null));
		VPos valign = parseVPos(def.getAttribute("grid.valign", null));
		if (halign != null)
			GridPane.setHalignment(child, halign);
		if (valign != null)
			GridPane.setValignment(child, valign);

		Insets m = parseInsets(def.getAttribute("margin", null));
		if (m != null)
			GridPane.setMargin(child, m);

		// Optional "fillWidth/fillHeight" isn’t directly a GridPane constraint; you
		// typically control via hgrow/vgrow and max size.
	}

	// ------------------- HBox / VBox helpers -------------------

	private static void applyHBoxConstraints(Node child, FileObject def) {
		Priority hgrow = parsePriority(def.getAttribute("hgrow", null));
		if (hgrow != null)
			HBox.setHgrow(child, hgrow);

		Insets m = parseInsets(def.getAttribute("margin", null));
		if (m != null)
			HBox.setMargin(child, m);
	}

	private static void applyVBoxConstraints(Node child, FileObject def) {
		Priority vgrow = parsePriority(def.getAttribute("vgrow", null));
		if (vgrow != null)
			VBox.setVgrow(child, vgrow);

		Insets m = parseInsets(def.getAttribute("margin", null));
		if (m != null)
			VBox.setMargin(child, m);
	}

	// ------------------- Parsing helpers -------------------

	private static boolean parseBoolean(String s, boolean def) {
		if (s == null)
			return def;
		s = s.trim();
		if (s.isEmpty())
			return def;
		return Boolean.parseBoolean(s);
	}

	private static Integer parseIntOrNull(String s) {
		if (s == null)
			return null;
		s = s.trim();
		if (s.isEmpty())
			return null;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Accepts "inf", "infinite", "max" for Double.MAX_VALUE, otherwise parses
	 * double.
	 */
	private static double parseSize(String s, double def) {
		if (s == null)
			return def;
		String v = s.trim().toLowerCase();
		if (v.isEmpty())
			return def;
		if (v.equals("inf") || v.equals("infinite") || v.equals("max"))
			return Double.MAX_VALUE;
		try {
			return Double.parseDouble(v);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static Priority parsePriority(String s) {
		if (s == null)
			return null;
		String v = s.trim().toUpperCase();
		if (v.isEmpty())
			return null;
		try {
			return Priority.valueOf(v);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static HPos parseHPos(String s) {
		if (s == null)
			return null;
		String v = s.trim().toUpperCase();
		if (v.isEmpty())
			return null;
		try {
			return HPos.valueOf(v);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static VPos parseVPos(String s) {
		if (s == null)
			return null;
		String v = s.trim().toUpperCase();
		if (v.isEmpty())
			return null;
		try {
			return VPos.valueOf(v);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static Pos parsePos(String s) {
		if (s == null)
			return null;
		String v = s.trim().toUpperCase();
		if (v.isEmpty())
			return null;
		try {
			return Pos.valueOf(v);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Parses "t,r,b,l" or "t r b l" or "v h" or "all" formats: - "10" => Insets(10)
	 * - "10,20" => Insets(top/bottom=10, left/right=20) (also supports "10 20") -
	 * "10,20,30,40" => Insets(10,20,30,40)
	 */
	private static Insets parseInsets(String s) {
		if (s == null)
			return null;
		String v = s.trim();
		if (v.isEmpty())
			return null;

		// split by comma or whitespace
		String[] parts = v.split("[,\\s]+");
		try {
			if (parts.length == 1) {
				double a = Double.parseDouble(parts[0]);
				return new Insets(a);
			}
			if (parts.length == 2) {
				double vert = Double.parseDouble(parts[0]);
				double horz = Double.parseDouble(parts[1]);
				return new Insets(vert, horz, vert, horz);
			}
			if (parts.length == 4) {
				double t = Double.parseDouble(parts[0]);
				double r = Double.parseDouble(parts[1]);
				double b = Double.parseDouble(parts[2]);
				double l = Double.parseDouble(parts[3]);
				return new Insets(t, r, b, l);
			}
		} catch (NumberFormatException ignored) {
		}
		return null;
	}

	private static Double parseDoubleOrNull(String s) {
		if (s == null)
			return null;
		s = s.trim();
		if (s.isEmpty())
			return null;
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void setSingleContent(String slotName, Node existingContent, Consumer<Node> setter, Node newContent,
			FileObject childEntry, FileObject parentEntry) {
		boolean replace = parseBoolean(childEntry.getAttribute("replace", "false"), false);
		boolean clear = parseBoolean(childEntry.getAttribute("clear", "false"), false);

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

		boolean expanded = parseBoolean(def.getAttribute("expanded", "false"), false);
		tp.setExpanded(expanded);

		boolean collapsible = parseBoolean(def.getAttribute("collapsible", "true"), true);
		tp.setCollapsible(collapsible);

		// Optional: graphic via separate loader etc. (not here)
		return tp;
	}

	private static void applyAnchorConstraints(Node child, FileObject childEntry) {
		Double top = parseDoubleOrNull(childEntry.getAttribute("anchor.top", null));
		Double right = parseDoubleOrNull(childEntry.getAttribute("anchor.right", null));
		Double bottom = parseDoubleOrNull(childEntry.getAttribute("anchor.bottom", null));
		Double left = parseDoubleOrNull(childEntry.getAttribute("anchor.left", null));

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

		IUIAction a = ctx.getAction(actionKey.trim());

		if (node instanceof javafx.scene.control.ButtonBase bb) {
			ActionBinder.bind(bb, a);

			ctx.accelerators().bind(a);
			return;
		}

		// Optional: other Node types that fire actions:
		if (node instanceof javafx.scene.control.Hyperlink hl) {
			hl.setOnAction(e -> a.execute());
			hl.disableProperty().bind(Bindings.not(a.enabledProperty()));
			hl.visibleProperty().bind(a.visibleProperty());
			hl.managedProperty().bind(a.visibleProperty());
			if (a.textProperty() != null)
				hl.textProperty().bind(a.textProperty());
			if (a.iconSpecProperty() != null) {
				IconBinder.bindIcon(hl.graphicProperty()::setValue, a, 16);
			}
//			if (a.graphicProperty() != null)
//				hl.graphicProperty().bind(a.graphicProperty());

		}
	}

	private static void bindMenuActionIfPresent(javafx.scene.control.MenuItem mi, FileObject entry,
			UIBuildContext ctx) {
		String actionKey = entry.getAttribute("action", null);
		if (actionKey == null || actionKey.isBlank())
			return;

		IUIAction a = ctx.getAction(actionKey.trim());
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