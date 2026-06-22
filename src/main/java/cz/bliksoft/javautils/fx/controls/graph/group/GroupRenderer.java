package cz.bliksoft.javautils.fx.controls.graph.group;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.javautils.fx.controls.graph.render.JoinPointRenderer;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public abstract class GroupRenderer {

	private static final Color BOUNDARY_COLOR = Color.web("#999999");
	private static final Color COLLAPSED_FILL = Color.web("#f0f0ff");
	private static final Color HEADER_FILL = Color.web("#e8e8f0");
	private static final double HEADER_HEIGHT = 20;
	private static final double RESIZE_HANDLE_SIZE = 8;

	public static Region renderExpanded(Group group, Group parent) {
		double x = group.getX();
		double y = group.getY();
		double w = group.getWidth();
		double h = group.getHeight();

		if (w <= 0 || h <= 0)
			return new Pane();

		Rectangle border = new Rectangle(w, h);
		border.setFill(Color.rgb(232, 232, 240, 0.3));
		border.setStroke(BOUNDARY_COLOR);
		border.setStrokeWidth(1.5);
		border.getStrokeDashArray().addAll(6.0, 4.0);
		border.setArcWidth(8);
		border.setArcHeight(8);
		border.setMouseTransparent(true);

		Rectangle header = new Rectangle(w, HEADER_HEIGHT);
		header.setFill(HEADER_FILL);
		header.setStroke(Color.TRANSPARENT);
		header.setArcWidth(8);
		header.setArcHeight(8);
		header.setCursor(Cursor.MOVE);
		header.getProperties().put("groupHeader", group.getId());

		Label nameLabel = new Label(group.getName());
		nameLabel.getStyleClass().add("graph-group-label");
		nameLabel.setLayoutX(6);
		nameLabel.setLayoutY(2);
		nameLabel.setMouseTransparent(true);

		Pane container = new Pane(border, header, nameLabel);
		container.setManaged(false);
		container.setLayoutX(x);
		container.setLayoutY(y);
		container.setPrefSize(w, h);
		container.setPickOnBounds(false);
		container.getStyleClass().add("graph-group-expanded");
		container.getProperties().put("groupId", group.getId());
		container.getProperties().put("nodeId", group.getId());

		return container;
	}

	public static Region renderCollapsed(Group group) {
		double w = Math.max(group.getWidth(), 80);
		double h = Math.max(group.getHeight(), 50);

		Rectangle outer = new Rectangle(w, h);
		outer.setFill(COLLAPSED_FILL);
		outer.setStroke(BOUNDARY_COLOR);
		outer.setStrokeWidth(2);
		outer.setArcWidth(6);
		outer.setArcHeight(6);

		Rectangle inner = new Rectangle(w - 6, h - 6);
		inner.setFill(Color.TRANSPARENT);
		inner.setStroke(BOUNDARY_COLOR);
		inner.setStrokeWidth(1);
		inner.setArcWidth(4);
		inner.setArcHeight(4);

		Label nameLabel = new Label(group.getName());
		nameLabel.getStyleClass().add("graph-node-label");
		nameLabel.setStyle("-fx-font-weight: bold;");
		nameLabel.setMaxWidth(w - 10);
		nameLabel.setWrapText(true);

		String desc = group.getProperties() != null
				? String.valueOf(group.getProperties().getOrDefault("description", ""))
				: "";
		VBox labelBox;
		if (desc != null && !desc.isEmpty() && !"".equals(desc)) {
			Label descLabel = new Label(desc);
			descLabel.getStyleClass().add("graph-node-label");
			descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
			descLabel.setMaxWidth(w - 10);
			descLabel.setWrapText(true);
			labelBox = new VBox(2, nameLabel, descLabel);
		} else {
			labelBox = new VBox(nameLabel);
		}
		labelBox.setAlignment(Pos.CENTER);

		StackPane shape = new StackPane(outer, inner, labelBox);
		shape.setAlignment(Pos.CENTER);
		shape.setPrefSize(w, h);
		shape.getStyleClass().add("graph-node");

		Pane container = new Pane(shape);
		container.setManaged(false);
		container.setLayoutX(group.getX());
		container.setLayoutY(group.getY());
		container.setPrefSize(w, h);
		container.getProperties().put("groupId", group.getId());
		container.getProperties().put("nodeId", group.getId());
		container.getStyleClass().add("graph-group-collapsed");

		JoinPointRenderer.renderJoinPoints(container, group.getExposedJoinPoints(), w, h);

		return container;
	}
}
