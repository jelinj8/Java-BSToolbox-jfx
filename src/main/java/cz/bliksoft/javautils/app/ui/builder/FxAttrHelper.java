package cz.bliksoft.javautils.app.ui.builder;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public final class FxAttrHelper {
	private FxAttrHelper() {
	}

	public static boolean bool(FileObject f, String key, boolean def) {
		String v = f.getAttribute(key, null);
		if (v == null)
			return def;
		v = v.trim();
		if (v.isEmpty())
			return def;
		return Boolean.parseBoolean(v);
	}

	public static int i(FileObject f, String key, int def) {
		String v = f.getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Integer.parseInt(v.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public static double d(FileObject f, String key, double def) {
		String v = f.getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Double.parseDouble(v.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public static Insets insets(FileObject f, String key) {
		String s = f.getAttribute(key, null);
		if (s == null)
			return null;
		s = s.trim();
		if (s.isEmpty())
			return null;

		String[] parts = s.split("[,\\s]+");
		try {
			if (parts.length == 1) {
				double a = Double.parseDouble(parts[0]);
				return new Insets(a);
			}
			if (parts.length == 2) {
				double v = Double.parseDouble(parts[0]);
				double h = Double.parseDouble(parts[1]);
				return new Insets(v, h, v, h);
			}
			if (parts.length == 4) {
				double t = Double.parseDouble(parts[0]);
				double r = Double.parseDouble(parts[1]);
				double b = Double.parseDouble(parts[2]);
				double l = Double.parseDouble(parts[3]);
				return new Insets(t, r, b, l);
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	public static Pos pos(FileObject f, String key, Pos def) {
		String v = f.getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Pos.valueOf(v.trim().toUpperCase());
		} catch (Exception e) {
			return def;
		}
	}

	public static Side side(FileObject f, String key, Side def) {
		String v = f.getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Side.valueOf(v.trim().toUpperCase());
		} catch (Exception e) {
			return def;
		}
	}

	public static Orientation orientation(FileObject f, String key, Orientation def) {
		String v = f.getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Orientation.valueOf(v.trim().toUpperCase());
		} catch (Exception e) {
			return def;
		}
	}

	/** Accepts "#RRGGBB", "red", "rgba(...)" etc. */
	public static Color color(FileObject f, String key, Color def) {
		String v = f.getAttribute(key, null);
		if (v == null)
			return def;
		try {
			return Color.web(v.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public static void applyRegionSizing(javafx.scene.layout.Region r, FileObject f) {
		if (f.getAttribute("prefWidth", null) != null)
			r.setPrefWidth(d(f, "prefWidth", r.getPrefWidth()));
		if (f.getAttribute("prefHeight", null) != null)
			r.setPrefHeight(d(f, "prefHeight", r.getPrefHeight()));
		if (f.getAttribute("minWidth", null) != null)
			r.setMinWidth(d(f, "minWidth", r.getMinWidth()));
		if (f.getAttribute("minHeight", null) != null)
			r.setMinHeight(d(f, "minHeight", r.getMinHeight()));
		if (f.getAttribute("maxWidth", null) != null)
			r.setMaxWidth(sizeInf(f.getAttribute("maxWidth", null), r.getMaxWidth()));
		if (f.getAttribute("maxHeight", null) != null)
			r.setMaxHeight(sizeInf(f.getAttribute("maxHeight", null), r.getMaxHeight()));

		Insets p = insets(f, "padding");
		if (p != null)
			r.setPadding(p);
	}

	private static double sizeInf(String s, double def) {
		if (s == null)
			return def;
		String v = s.trim().toLowerCase();
		if (v.equals("inf") || v.equals("infinite") || v.equals("max"))
			return Double.MAX_VALUE;
		try {
			return Double.parseDouble(v);
		} catch (Exception e) {
			return def;
		}
	}

	public static String s(FileObject f, String key, String def) {
		String v = f.getAttribute(key, null);
		if (v == null)
			return def;
		v = v.trim();
		return v.isEmpty() ? def : v;
	}

	/**
	 * Generic node-ish attributes that are safe to set in loaders too (optional).
	 */
	public static void applyCommon(Region r, FileObject f) {
		// sizing
		if (f.getAttribute("prefWidth", null) != null)
			r.setPrefWidth(d(f, "prefWidth", r.getPrefWidth()));
		if (f.getAttribute("prefHeight", null) != null)
			r.setPrefHeight(d(f, "prefHeight", r.getPrefHeight()));
		if (f.getAttribute("minWidth", null) != null)
			r.setMinWidth(d(f, "minWidth", r.getMinWidth()));
		if (f.getAttribute("minHeight", null) != null)
			r.setMinHeight(d(f, "minHeight", r.getMinHeight()));
		if (f.getAttribute("maxWidth", null) != null)
			r.setMaxWidth(sizeInf(s(f, "maxWidth", null), r.getMaxWidth()));
		if (f.getAttribute("maxHeight", null) != null)
			r.setMaxHeight(sizeInf(s(f, "maxHeight", null), r.getMaxHeight()));

		Insets p = insets(f, "padding");
		if (p != null)
			r.setPadding(p);
	}

	public static void applyLabeled(Labeled l, FileObject f) {
		String text = f.getAttribute("text", null);
		if (text != null)
			l.setText(text);
	}

	public static void applyControl(Control c, FileObject f) {
		if (f.getAttribute("disable", null) != null)
			c.setDisable(bool(f, "disable", c.isDisable()));
		if (f.getAttribute("focusTraversable", null) != null)
			c.setFocusTraversable(bool(f, "focusTraversable", c.isFocusTraversable()));
	}

	public static void applyPane(Pane p, FileObject f) {
		// (nothing universal here beyond Region sizing, which Pane inherits)
	}
}
