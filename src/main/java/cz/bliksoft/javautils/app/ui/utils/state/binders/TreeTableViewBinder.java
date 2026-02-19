package cz.bliksoft.javautils.app.ui.utils.state.binders;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.util.*;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateBinder;

public final class TreeTableViewBinder implements FxStateBinder {

	@Override
	public boolean supports(Node n) {
		return n instanceof TreeTableView<?>;
	}

	@Override
	public void save(Node n, String pfx) {
		TreeTableView<?> tableAny = (TreeTableView<?>) n;

		ObservableList<TreeTableColumn<Object, ?>> cols = columnsAsObject(tableAny);
		Map<String, TreeTableColumn<Object, ?>> byId = indexById(cols);

		// order
		BSApp.getLocalProperties().put(pfx + ".cols.order", cols.stream().map(TreeTableViewBinder::colIdOrNull)
				.filter(Objects::nonNull).collect(Collectors.joining(",")));

		// per column
		for (var e : byId.entrySet()) {
			String id = e.getKey();
			TreeTableColumn<Object, ?> c = e.getValue();

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
			TreeTableColumn.SortType st = c.getSortType();
			if (st == null)
				return null;
			return id + ":" + st.name(); // ASCENDING / DESCENDING
		}).filter(Objects::nonNull).collect(Collectors.joining(";"));
		BSApp.getLocalProperties().put(pfx + ".sort", sort);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void restore(Node n, String pfx) {
		TreeTableView<?> tableAny = (TreeTableView<?>) n;

		Platform.runLater(() -> {
			ObservableList<TreeTableColumn<Object, ?>> cols = columnsAsObject(tableAny);
			Map<String, TreeTableColumn<Object, ?>> byId = indexById(cols);

			// order
			String rawOrder = BSApp.getLocalProperties().getProperty(pfx + ".cols.order");
			if (rawOrder != null && !rawOrder.isBlank()) {
				List<TreeTableColumn<Object, ?>> ordered = new ArrayList<>();
				Set<TreeTableColumn<Object, ?>> used = new HashSet<>();

				for (String id : split(rawOrder, ",")) {
					TreeTableColumn<Object, ?> c = byId.get(id);
					if (c != null && used.add(c))
						ordered.add(c);
				}
				for (TreeTableColumn<Object, ?> c : cols)
					if (used.add(c))
						ordered.add(c);

				cols.setAll(ordered);
				byId = indexById(cols); // refresh map
			}

			// per column
			for (var e : byId.entrySet()) {
				String id = e.getKey();
				TreeTableColumn<Object, ?> c = e.getValue();

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

					TreeTableColumn<Object, ?> c = byId.get(id);
					if (c == null)
						continue;

					TreeTableColumn.SortType st = parseSortType(type);
					if (st == null)
						continue;

					c.setSortType(st);
					tableAny.getSortOrder().add((TreeTableColumn) c); // raw add je ok
				}
				tableAny.sort();
			}
		});
	}

	// --- generics bridge ---

	@SuppressWarnings("unchecked")
	private static ObservableList<TreeTableColumn<Object, ?>> columnsAsObject(TreeTableView<?> table) {
		return (ObservableList<TreeTableColumn<Object, ?>>) (ObservableList<?>) table.getColumns();
	}

	// --- helpers ---

	private static Map<String, TreeTableColumn<Object, ?>> indexById(Collection<TreeTableColumn<Object, ?>> cols) {
		Map<String, TreeTableColumn<Object, ?>> m = new LinkedHashMap<>();
		for (TreeTableColumn<Object, ?> c : cols) {
			String id = colIdOrNullObj(c);
			if (id == null)
				continue;
			m.putIfAbsent(id, c);
		}
		return m;
	}

	private static String colIdOrNull(TreeTableColumn<Object, ?> c) {
		String id = c.getId();
		return (id == null || id.isBlank()) ? null : id.trim();
	}

	private static String colIdOrNullObj(TreeTableColumn<?, ?> c) {
		String id = c.getId();
		return (id == null || id.isBlank()) ? null : id.trim();
	}

	private static TreeTableColumn.SortType parseSortType(String s) {
		try {
			if ("ASC".equalsIgnoreCase(s))
				return TreeTableColumn.SortType.ASCENDING;
			if ("DESC".equalsIgnoreCase(s))
				return TreeTableColumn.SortType.DESCENDING;
			return TreeTableColumn.SortType.valueOf(s.toUpperCase(Locale.ROOT));
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
