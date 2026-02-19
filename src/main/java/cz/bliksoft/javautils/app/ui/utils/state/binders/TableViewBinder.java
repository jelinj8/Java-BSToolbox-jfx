package cz.bliksoft.javautils.app.ui.utils.state.binders;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.*;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateBinder;

public final class TableViewBinder implements FxStateBinder {

	@Override
	public boolean supports(Node n) {
		return n instanceof TableView<?>;
	}

	@Override
	public void save(Node n, String pfx) {
		TableView<?> tableAny = (TableView<?>) n;

		// Vyřeší generika: pracujeme s "erased" typem řádků jako Object
		ObservableList<TableColumn<Object, ?>> cols = columnsAsObject(tableAny);
		Map<String, TableColumn<Object, ?>> byId = indexById(cols);

		// order
		BSApp.getLocalProperties().put(pfx + ".cols.order", cols.stream().map(TableViewBinder::colIdOrNull)
				.filter(Objects::nonNull).collect(Collectors.joining(",")));

		// per column
		for (var e : byId.entrySet()) {
			String id = e.getKey();
			TableColumn<Object, ?> c = e.getValue();

			double w = c.getWidth();
			if (Double.isFinite(w) && w > 5)
				BSApp.getLocalProperties().putDouble(pfx + ".col." + esc(id) + ".w", w);
			BSApp.getLocalProperties().putBool(pfx + ".col." + esc(id) + ".vis", c.isVisible());
		}

		// sort
		String sort = tableAny.getSortOrder().stream().map(c -> {
			String id = colIdOrNullObj(c);
			if (id == null)
				return null;
			TableColumn.SortType st = c.getSortType();
			if (st == null)
				return null;
			return id + ":" + st.name(); // ASCENDING / DESCENDING
		}).filter(Objects::nonNull).collect(Collectors.joining(";"));
		BSApp.getLocalProperties().put(pfx + ".sort", sort);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void restore(Node n, String pfx) {
		TableView<?> tableAny = (TableView<?>) n;

		Platform.runLater(() -> {
			ObservableList<TableColumn<Object, ?>> cols = columnsAsObject(tableAny);
			Map<String, TableColumn<Object, ?>> byId = indexById(cols);

			// order
			String rawOrder = BSApp.getLocalProperties().getProperty(pfx + ".cols.order");
			if (rawOrder != null && !rawOrder.isBlank()) {
				List<TableColumn<Object, ?>> ordered = new ArrayList<>();
				Set<TableColumn<Object, ?>> used = new HashSet<>();

				for (String id : split(rawOrder, ",")) {
					TableColumn<Object, ?> c = byId.get(id);
					if (c != null && used.add(c))
						ordered.add(c);
				}
				for (TableColumn<Object, ?> c : cols)
					if (used.add(c))
						ordered.add(c);

				cols.setAll(ordered);
				byId = indexById(cols); // refresh map
			}

			// per column: visible + width
			for (var e : byId.entrySet()) {
				String id = e.getKey();
				TableColumn<Object, ?> c = e.getValue();

				Boolean vis = BSApp.getLocalProperties().getBool(pfx + ".col." + esc(id) + ".vis");
				if (vis != null)
					c.setVisible(vis);

				Double w = BSApp.getLocalProperties().getDouble(pfx + ".col." + esc(id) + ".w");
				if (w != null && Double.isFinite(w) && w > 5)
					c.setPrefWidth(w);
			}

			// sort
			tableAny.getSortOrder().clear();
			String rawSort = BSApp.getLocalProperties().getProperty(pfx + ".sort");
			if (rawSort != null && !rawSort.isBlank()) {
				for (String entry : split(rawSort, ";")) {
					int idx = entry.lastIndexOf(':');
					if (idx <= 0 || idx >= entry.length() - 1)
						continue;

					String id = entry.substring(0, idx);
					String type = entry.substring(idx + 1);

					TableColumn<Object, ?> c = byId.get(id);
					if (c == null)
						continue;

					TableColumn.SortType st = parseSortType(type);
					if (st == null)
						continue;

					c.setSortType(st);
					tableAny.getSortOrder().add((TableColumn) c); // raw add je ok
				}
				tableAny.sort();
			}
		});
	}

	// --- generics bridge ---

	@SuppressWarnings("unchecked")
	private static ObservableList<TableColumn<Object, ?>> columnsAsObject(TableView<?> table) {
		return (ObservableList<TableColumn<Object, ?>>) (ObservableList<?>) table.getColumns();
	}

	// --- helpers ---

	private static Map<String, TableColumn<Object, ?>> indexById(Collection<TableColumn<Object, ?>> cols) {
		Map<String, TableColumn<Object, ?>> m = new LinkedHashMap<>();
		for (TableColumn<Object, ?> c : cols) {
			String id = colIdOrNullObj(c);
			if (id == null)
				continue;
			m.putIfAbsent(id, c);
		}
		return m;
	}

	private static String colIdOrNull(TableColumn<Object, ?> c) {
		String id = c.getId();
		return (id == null || id.isBlank()) ? null : id.trim();
	}

	private static String colIdOrNullObj(TableColumn<?, ?> c) {
		String id = c.getId();
		return (id == null || id.isBlank()) ? null : id.trim();
	}

	private static TableColumn.SortType parseSortType(String s) {
		try {
			if ("ASC".equalsIgnoreCase(s))
				return TableColumn.SortType.ASCENDING;
			if ("DESC".equalsIgnoreCase(s))
				return TableColumn.SortType.DESCENDING;
			return TableColumn.SortType.valueOf(s.toUpperCase(Locale.ROOT));
		} catch (Exception e) {
			return null;
		}
	}

	private static List<String> split(String raw, String sep) {
		String[] parts = raw.split(sep);
		List<String> out = new ArrayList<>(parts.length);
		for (String p : parts) {
			String t = p.trim();
			if (!t.isEmpty())
				out.add(t);
		}
		return out;
	}

	private static String esc(String id) {
		// pokud máš id jen [a-zA-Z0-9_-], klidně return id;
		return id.replace("%", "%25").replace(".", "%2E").replace(":", "%3A");
	}
}
