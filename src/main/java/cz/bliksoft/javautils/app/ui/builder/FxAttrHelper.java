package cz.bliksoft.javautils.app.ui.builder;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

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
	 * Accepts "inf", "infinite", "max" for Double.MAX_VALUE, "pref", "preferred"
	 * for Region.USE_PREF_SIZE/-infinity, otherwise parses double.
	 */
	public static Double sizeInfPref(String s, Double def) {
		if (s == null)
			return def;
		String v = s.trim().toLowerCase();
		if (v.equals("inf") || v.equals("infinite") || v.equals("max"))
			return Double.MAX_VALUE;
		if (v.equals("pref") || v.equals("preferred"))
			return Region.USE_PREF_SIZE;
		try {
			return Double.parseDouble(v);
		} catch (Exception e) {
			return def;
		}
	}

	public static Font getFont(FileObject file) {
		String family = file.getAttribute("font", null);
		Double size = file.getDouble("fontSize", null);
		Boolean bold = file.getBool("bold", null);
		Boolean italic = file.getBool("italic", null);

		if (family == null && size == null && bold == null && italic == null)
			return null;

		String fam = (family != null ? family : Font.getDefault().getFamily());
		double sz = (size != null ? size : Font.getDefault().getSize());

		FontWeight w = (bold != null && bold) ? FontWeight.BOLD : FontWeight.NORMAL;
		FontPosture p = (italic != null && italic) ? FontPosture.ITALIC : FontPosture.REGULAR;

		return Font.font(fam, w, p, sz);
	}

	public static Paint getPaint(FileObject file, String key) {
		String val = file.getAttribute(key, null);
		if (val == null || val.isBlank())
			return null;

		try {
			return Color.web(val.trim());
		} catch (IllegalArgumentException ex) {
			// neplatná barva → ignoruj
			return null;
		}
	}

	public static Cursor parseCursor(String v) {
		if (v == null)
			return null;
		String s = v.trim();
		if (s.isEmpty())
			return null;

		// nejběžnější názvy
		return switch (s.toUpperCase()) {
		case "DEFAULT" -> Cursor.DEFAULT;
		case "HAND" -> Cursor.HAND;
		case "TEXT" -> Cursor.TEXT;
		case "WAIT" -> Cursor.WAIT;
		case "CROSSHAIR" -> Cursor.CROSSHAIR;
		case "MOVE" -> Cursor.MOVE;
		case "E_RESIZE" -> Cursor.E_RESIZE;
		case "W_RESIZE" -> Cursor.W_RESIZE;
		case "N_RESIZE" -> Cursor.N_RESIZE;
		case "S_RESIZE" -> Cursor.S_RESIZE;
		case "NE_RESIZE" -> Cursor.NE_RESIZE;
		case "NW_RESIZE" -> Cursor.NW_RESIZE;
		case "SE_RESIZE" -> Cursor.SE_RESIZE;
		case "SW_RESIZE" -> Cursor.SW_RESIZE;
		case "H_RESIZE" -> Cursor.H_RESIZE;
		case "V_RESIZE" -> Cursor.V_RESIZE;
		case "DISAPPEAR" -> Cursor.DISAPPEAR;
		case "NONE" -> Cursor.NONE;
		default -> {
			// pokus o obecné parsování (javfx má Cursor.cursor(String))
			try {
				yield Cursor.cursor(s);
			} catch (Exception ex) {
				yield null;
			}
		}
		};
	}

	public static ContentDisplay parseContentDisplay(String v) {
		if (v == null)
			return null;
		try {
			return ContentDisplay.valueOf(v.trim().toUpperCase());
		} catch (Exception ex) {
			return null;
		}
	}
}
