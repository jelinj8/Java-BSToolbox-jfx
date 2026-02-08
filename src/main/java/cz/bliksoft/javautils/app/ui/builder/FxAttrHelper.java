package cz.bliksoft.javautils.app.ui.builder;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public final class FxAttrHelper {
	private FxAttrHelper() {
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

	/**
	 * Parses "t,r,b,l" or "t r b l" or "v h" or "all" formats: - "10" => Insets(10)
	 * - "10,20" => Insets(top/bottom=10, left/right=20) (also supports "10 20") -
	 * "10,20,30,40" => Insets(10,20,30,40)
	 */
	public static Insets parseInsets(FileObject f, String key) {
		String s = f.getAttribute(key, null);
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

	public static Priority growPriority(FileObject f, String key) {
		String s = f.getAttribute(key, null);
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

	public static HPos hPos(FileObject f, String key) {
		String s = f.getAttribute(key, null);
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

	public static VPos vPos(FileObject f, String key) {
		String s = f.getAttribute(key, null);
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

	/**
	 * Accepts "inf", "infinite", "max" for Double.MAX_VALUE, otherwise parses
	 * double.
	 */
	public static Double sizeInf(String s, Double def) {
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

//	private static Pos parsePos(String s) {
//		if (s == null)
//			return null;
//		String v = s.trim().toUpperCase();
//		if (v.isEmpty())
//			return null;
//		try {
//			return Pos.valueOf(v);
//		} catch (IllegalArgumentException e) {
//			return null;
//		}
//	}

	/**
	 * Generic node-ish attributes that are safe to set in loaders too (optional).
	 */
	public static void applyCommon(Region r, FileObject f) {
		// sizing
		if (f.getAttribute("prefWidth", null) != null)
			r.setPrefWidth(f.getDouble("prefWidth", r.getPrefWidth()));
		if (f.getAttribute("prefHeight", null) != null)
			r.setPrefHeight(f.getDouble("prefHeight", r.getPrefHeight()));
		if (f.getAttribute("minWidth", null) != null)
			r.setMinWidth(f.getDouble("minWidth", r.getMinWidth()));
		if (f.getAttribute("minHeight", null) != null)
			r.setMinHeight(f.getDouble("minHeight", r.getMinHeight()));
		if (f.getAttribute("maxWidth", null) != null)
			r.setMaxWidth(sizeInf(f.getAttribute("maxWidth", null), r.getMaxWidth()));
		if (f.getAttribute("maxHeight", null) != null)
			r.setMaxHeight(sizeInf(f.getAttribute("maxHeight", null), r.getMaxHeight()));

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
			c.setDisable(f.getBool("disable", c.isDisable()));
		if (f.getAttribute("focusTraversable", null) != null)
			c.setFocusTraversable(f.getBool("focusTraversable", c.isFocusTraversable()));
	}

	public static void applyPane(Pane p, FileObject f) {
		// (nothing universal here beyond Region sizing, which Pane inherits)
	}
}
