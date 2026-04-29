package cz.bliksoft.javautils.app.ui.utils.state;

import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.*;

import cz.bliksoft.javautils.app.ui.utils.state.binders.SplitPaneBinder;
import cz.bliksoft.javautils.app.ui.utils.state.binders.TableViewBinder;
import cz.bliksoft.javautils.app.ui.utils.state.binders.TreeTableViewBinder;

public final class FxStateManager {

	private final String windowKey;
	private final List<FxStateBinder> binders = new ArrayList<>();

	public FxStateManager(String windowKey) {
		this.windowKey = Objects.requireNonNull(windowKey);

		// zaregistruj bindery
		binders.add(new SplitPaneBinder());
		binders.add(new TableViewBinder());
		binders.add(new TreeTableViewBinder());
		// add další: TabPaneBinder, ScrollPaneBinder, ...
	}

	/**
	 * Uloží stav všech uzlů v podstromu, které mají state.key (nebo fallback id).
	 */
	public void persistState(Node root) {
		traverse(root, List.of(), Mode.SAVE);
	}

	/**
	 * Obnoví stav všech uzlů v podstromu, které mají state.key (nebo fallback id).
	 */
	public void restoreState(Node root) {
		traverse(root, List.of(), Mode.RESTORE);
	}

	private enum Mode {
		SAVE, RESTORE
	}

	private void traverse(Node n, List<String> ctxStack, Mode mode) {
		// copy-on-write stack
		List<String> nextStack = ctxStack;

		boolean reset = boolProp(n, FxStateMeta.RESET, false);
		String ctx = strProp(n, FxStateMeta.CTX);

		if (reset)
			nextStack = new ArrayList<>();
		if (ctx != null && !ctx.isBlank()) {
			if (nextStack == ctxStack)
				nextStack = new ArrayList<>(ctxStack);
			nextStack.add(ctx.trim());
		}

		String key = strProp(n, FxStateMeta.KEY);
		// if (key == null || key.isBlank())
		// key = n.getId(); // fallback (volitelně)
		if (key != null && !key.isBlank()) {
			String prefix = buildPrefix(windowKey, nextStack, key.trim());
			dispatch(n, prefix, mode);
		}

		if (n instanceof Parent p) {
			for (Node ch : p.getChildrenUnmodifiable()) {
				traverse(ch, nextStack, mode);
			}
		}
	}

	private void dispatch(Node n, String prefix, Mode mode) {
		for (FxStateBinder b : binders) {
			if (!b.supports(n))
				continue;
			if (mode == Mode.SAVE)
				b.save(n, prefix);
			else
				b.restore(n, prefix);
		}
	}

	private static String buildPrefix(String windowKey, List<String> ctxStack, String key) {
		if (ctxStack.isEmpty())
			return windowKey + "." + key;
		return windowKey + "." + String.join(".", ctxStack) + "." + key;
	}

	private static String strProp(Node n, String k) {
		Object v = n.getProperties().get(k);
		return v == null ? null : v.toString();
	}

	private static boolean boolProp(Node n, String k, boolean def) {
		Object v = n.getProperties().get(k);
		if (v == null)
			return def;
		if (v instanceof Boolean b)
			return b;
		return Boolean.parseBoolean(v.toString());
	}
}
