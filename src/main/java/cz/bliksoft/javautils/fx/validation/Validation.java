package cz.bliksoft.javautils.fx.validation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.controlsfx.control.decoration.Decoration;
import org.controlsfx.control.decoration.Decorator;
import org.controlsfx.control.decoration.GraphicDecoration;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.SVGPath;

public class Validation {

	public static final Object VALIDATION_KEY = new Object();

	private static final Object DECORATION_KEY = new Object();

	private static final Object TOOLTIP_KEY = new Object();

	public static void setKey(Node node, Object key) {
		node.getProperties().put(VALIDATION_KEY, key);
	}

	public static Object getKey(Node node) {
		return node.getProperties().get(VALIDATION_KEY);
	}

	private final ValidationRegistry registry;

	public Validation() {
		registry = null;
	}

	public Validation(ValidationRegistry registry) {
		this.registry = Objects.requireNonNull(registry);
	}

	/**
	 * apply validation result on root node
	 * 
	 * @param root
	 * @param vr
	 */
	public void apply(Parent root, ValidationResult vr) {
		// 1) Build index: validationKey -> nodes
		Map<Object, List<Node>> byKey = root.lookupAll("*").stream().filter(n -> Validation.getKey(n) != null)
				.collect(Collectors.groupingBy(Validation::getKey));

		// 2) Clear previous decorations we own
		byKey.values().stream().flatMap(List::stream).forEach(this::clear);

		for (MessageGroup g : MessageGroup.from(vr)) {
			List<Node> targets = byKey.getOrDefault(g.key(), List.of());
			for (Node target : targets) {
				decorate(target, g.level(), g.tooltipText());
			}
		}
	}

	/**
	 * Apply validation result to registered nodes
	 * 
	 * @param vr
	 */
	public void apply(ValidationResult vr) {
		// Option A: clear only nodes that are currently registered (cheap)
		for (Node n : registry.allRegisteredLiveNodes()) {
			clear(n);
		}

		// apply new decorations
		for (MessageGroup g : MessageGroup.from(vr)) {
			for (Node target : registry.findTargets(g.key())) {
				decorate(target, g.level(), g.tooltipText());
			}
		}
	}

	public void clear(Node node) {
		Decoration old = (Decoration) node.getProperties().remove(DECORATION_KEY);
		if (old != null) {
			Decorator.removeDecoration(node, old);
		}

		Tooltip tip = (Tooltip) node.getProperties().remove(TOOLTIP_KEY);
		if (tip != null) {
			Tooltip.uninstall(node, tip);
		}
	}

	private void decorate(Node target, ValidationResultLevel level, String tooltipText) {
		clear(target);

		if (level == null || level == ValidationResultLevel.OK)
			return;

		Tooltip tip = new Tooltip(tooltipText);
		target.getProperties().put(TOOLTIP_KEY, tip);
		Tooltip.install(target, tip);

		Node icon = createIcon(level);
		icon.setMouseTransparent(true);
		Tooltip.install(icon, new Tooltip(tooltipText));

		Decoration deco = new GraphicDecoration(icon, Pos.BOTTOM_RIGHT, -8, 0);
		nodePutDecoration(target, deco);

		Decorator.addDecoration(target, deco);
	}

	private void nodePutDecoration(Node target, Decoration deco) {
		target.getProperties().put(DECORATION_KEY, deco);
	}

	private Node createIcon(ValidationResultLevel level) {
		// Simple SVG badges (swap paths as you like)
		SVGPath p = new SVGPath();
		p.getStyleClass().add("validation-icon");
		p.getStyleClass().add("validation-" + level.name().toLowerCase());

		switch (level) {
		case ERROR -> p.setContent("M12 2 L22 20 H2 Z"); // triangle
		case WARN -> p.setContent("M12 2 L22 20 H2 Z"); // triangle (different CSS)
		case INFO -> p.setContent("M12 2 A10 10 0 1 0 12 22 A10 10 0 1 0 12 2"); // circle
		default -> p.setContent(""); // OK -> none
		}
		return p;
	}

	private record MessageGroup(Object key, ValidationResultLevel level, String tooltipText) {

		static List<MessageGroup> from(ValidationResult vr) {

			return vr.messages().stream().filter(m -> m.key() != null)
					.collect(Collectors.groupingBy(ValidationMessage::key)).entrySet().stream().map(e -> {
						Object key = e.getKey();
						List<ValidationMessage> msgs = e.getValue();

						ValidationResultLevel maxLevel = msgs.stream().map(ValidationMessage::level)
								.max(Comparator.naturalOrder()).orElse(ValidationResultLevel.OK);

						String tip = msgs.stream().map(ValidationMessage::message)
								.filter(s -> s != null && !s.isBlank()).map(s -> "• " + s)
								.collect(Collectors.joining("\n"));

						return new MessageGroup(key, maxLevel, tip);
					}).filter(g -> g.level() != ValidationResultLevel.OK).toList();
		}
	}
}
