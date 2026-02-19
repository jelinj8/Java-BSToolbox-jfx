package cz.bliksoft.javautils.app.ui.utils.state;

import javafx.scene.Node;

public final class FxStateMeta {
	private FxStateMeta() {
	}

	public static final String CTX = "state.ctx"; // dědičný kontext segment
	public static final String KEY = "state.key"; // komponenta, která se persistuje
	public static final String RESET = "state.ctx.reset"; // boolean: reset kontextu pro podstrom

	public static <T extends Node> T ctx(T n, String ctx) {
		n.getProperties().put(CTX, ctx);
		return n;
	}

	public static <T extends Node> T key(T n, String key) {
		n.getProperties().put(KEY, key);
		return n;
	}

	public static <T extends Node> T resetCtx(T n, boolean reset) {
		n.getProperties().put(RESET, reset);
		return n;
	}
}
