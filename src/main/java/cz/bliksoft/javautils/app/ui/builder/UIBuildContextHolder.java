package cz.bliksoft.javautils.app.ui.builder;

public final class UIBuildContextHolder {
    private static final ThreadLocal<UIBuildContext> TL = new ThreadLocal<>();
    private UIBuildContextHolder() {}

    public static void set(UIBuildContext ctx) { TL.set(ctx); }
    public static void clear() { TL.remove(); }
    public static UIBuildContext get() { return TL.get(); }
}
