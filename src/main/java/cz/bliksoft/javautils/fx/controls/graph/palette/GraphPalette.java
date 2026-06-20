package cz.bliksoft.javautils.fx.controls.graph.palette;

import java.util.List;
import java.util.Map;

import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.dataflow.types.NodeType;
import cz.bliksoft.dataflow.types.NodeTypeRegistry;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import cz.bliksoft.javautils.fx.controls.graph.command.CreateNodeCommand;
import cz.bliksoft.javautils.fx.controls.graph.group.GroupBuilder;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class GraphPalette extends VBox {

	private static final String DRAG_NODE_TYPE_KEY = "graph-node-type-id";

	private final VBox categoriesBox = new VBox();
	private final TextField searchField = new TextField();
	private GraphCanvas targetCanvas;

	public GraphPalette() {
		getStyleClass().add("graph-palette");
		setSpacing(4);
		setPadding(new Insets(4));

		searchField.setPromptText("Search nodes...");
		searchField.getStyleClass().add("graph-palette-search");
		searchField.textProperty().addListener((obs, o, n) -> rebuildPalette());

		getChildren().addAll(searchField, categoriesBox);
	}

	public void setTargetCanvas(GraphCanvas canvas) {
		this.targetCanvas = canvas;
		setupDropTarget(canvas);
	}

	public void refresh() {
		rebuildPalette();
	}

	private void rebuildPalette() {
		categoriesBox.getChildren().clear();
		String filter = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";

		Map<String, List<NodeType>> byCategory = NodeTypeRegistry.getInstance().getByCategory();
		for (var entry : byCategory.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {

			String category = entry.getKey().isEmpty() ? "Other" : entry.getKey();
			List<NodeType> types = entry.getValue();

			VBox content = new VBox(2);
			for (NodeType type : types) {
				if (!filter.isEmpty() && !matchesFilter(type, filter))
					continue;
				content.getChildren().add(createPaletteEntry(type));
			}

			if (content.getChildren().isEmpty())
				continue;

			TitledPane section = new TitledPane(category, content);
			section.setExpanded(true);
			section.setAnimated(false);
			categoriesBox.getChildren().add(section);
		}
	}

	private boolean matchesFilter(NodeType type, String filter) {
		if (type.getDisplayName().toLowerCase().contains(filter))
			return true;
		if (type.getCategory() != null && type.getCategory().toLowerCase().contains(filter))
			return true;
		return type.getTypeId().toLowerCase().contains(filter);
	}

	private HBox createPaletteEntry(NodeType type) {
		Label label = new Label(type.getDisplayName());
		label.getStyleClass().add("graph-palette-entry-label");

		HBox entry = new HBox(6, label);
		entry.getStyleClass().add("graph-palette-entry");
		entry.setPadding(new Insets(4, 8, 4, 8));

		if (type.getShapeType() != null) {
			Tooltip.install(entry, new Tooltip(type.getDisplayName() + " (" + type.getShapeType() + ")"));
		}

		entry.setOnDragDetected(e -> {
			Dragboard db = entry.startDragAndDrop(TransferMode.COPY);
			ClipboardContent cc = new ClipboardContent();
			cc.putString(type.getTypeId());
			db.setContent(cc);
			e.consume();
		});

		entry.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
				createNodeAtViewportCenter(type);
				e.consume();
			}
		});

		return entry;
	}

	private void createNodeAtViewportCenter(NodeType type) {
		if (targetCanvas == null || targetCanvas.getGraph() == null)
			return;

		double viewCenterX = (targetCanvas.getWidth() / 2 - targetCanvas.getScrollX()) / targetCanvas.getZoom();
		double viewCenterY = (targetCanvas.getHeight() / 2 - targetCanvas.getScrollY()) / targetCanvas.getZoom();
		double nodeX = viewCenterX - type.getDefaultWidth() / 2;
		double nodeY = viewCenterY - type.getDefaultHeight() / 2;

		if (targetCanvas.isSnapToGrid()) {
			nodeX = targetCanvas.snapX(nodeX);
			nodeY = targetCanvas.snapY(nodeY);
		}

		Node node = type.createNode(nodeX, nodeY);
		CreateNodeCommand cmd = new CreateNodeCommand(targetCanvas.getGraph(), node);
		targetCanvas.getCommandHistory().execute(cmd);
		addToContainingGroup(targetCanvas, node);
		targetCanvas.refreshGraph();
		targetCanvas.getSelectionModel().select(node.getId());
		targetCanvas.updateSelectionVisuals();
	}

	private void setupDropTarget(GraphCanvas canvas) {
		canvas.setOnDragOver(e -> {
			if (e.getDragboard().hasString())
				e.acceptTransferModes(TransferMode.COPY);
			e.consume();
		});

		canvas.setOnDragDropped(e -> {
			String typeId = e.getDragboard().getString();
			NodeType type = NodeTypeRegistry.getInstance().get(typeId);
			if (type == null || canvas.getGraph() == null) {
				e.setDropCompleted(false);
				e.consume();
				return;
			}

			javafx.geometry.Point2D local = canvas.getContentPane().screenToLocal(e.getScreenX(), e.getScreenY());
			double nodeX, nodeY;
			if (local != null) {
				nodeX = local.getX() - type.getDefaultWidth() / 2;
				nodeY = local.getY() - type.getDefaultHeight() / 2;
			} else {
				nodeX = e.getX();
				nodeY = e.getY();
			}

			if (canvas.isSnapToGrid()) {
				nodeX = canvas.snapX(nodeX);
				nodeY = canvas.snapY(nodeY);
			}

			Node node = type.createNode(nodeX, nodeY);
			CreateNodeCommand cmd = new CreateNodeCommand(canvas.getGraph(), node);
			canvas.getCommandHistory().execute(cmd);
			addToContainingGroup(canvas, node);
			canvas.refreshGraph();
			canvas.getSelectionModel().select(node.getId());
			canvas.updateSelectionVisuals();

			e.setDropCompleted(true);
			e.consume();
		});
	}

	private void addToContainingGroup(GraphCanvas canvas, Node node) {
		var sel = canvas.getSelectionModel().getSelection();
		if (sel.size() == 1) {
			Group selectedGroup = GroupBuilder.findGroupById(canvas.getGraph(), sel.iterator().next());
			if (selectedGroup != null && !selectedGroup.isCollapsed()) {
				GroupBuilder.addNodeToGroup(selectedGroup, node);
				return;
			}
		}
		Group group = GroupBuilder.findExpandedGroupAtPoint(canvas.getGraph(), node.getX() + node.getWidth() / 2,
				node.getY() + node.getHeight() / 2);
		if (group != null)
			GroupBuilder.addNodeToGroup(group, node);
	}
}
