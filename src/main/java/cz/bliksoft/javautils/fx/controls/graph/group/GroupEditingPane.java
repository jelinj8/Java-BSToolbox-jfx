package cz.bliksoft.javautils.fx.controls.graph.group;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Edge;
import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.dataflow.model.JoinPoint;
import cz.bliksoft.dataflow.model.Node;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GroupEditingPane extends VBox {

	private final Graph rootGraph;
	private final GraphCanvas canvas;
	private final HBox breadcrumbBar = new HBox(4);
	private final List<UUID> navigationStack = new ArrayList<>();

	public GroupEditingPane(Graph rootGraph, GraphCanvas canvas) {
		this.rootGraph = rootGraph;
		this.canvas = canvas;

		breadcrumbBar.getStyleClass().add("graph-breadcrumb-bar");
		breadcrumbBar.setPadding(new Insets(4, 8, 4, 8));

		VBox.setVgrow(canvas, Priority.ALWAYS);
		getChildren().addAll(breadcrumbBar, new Separator(), canvas);

		showRoot();
	}

	public void showRoot() {
		navigationStack.clear();
		canvas.setGraph(rootGraph);
		updateBreadcrumbs();
	}

	public void enterGroup(UUID groupId) {
		Group group = GroupBuilder.findGroupById(rootGraph, groupId);
		if (group == null)
			return;

		navigationStack.add(groupId);
		Graph subGraph = buildSubGraph(group);
		canvas.setGraph(subGraph);
		updateBreadcrumbs();
	}

	public void navigateTo(int depth) {
		if (depth == 0) {
			showRoot();
			return;
		}
		while (navigationStack.size() > depth)
			navigationStack.removeLast();

		UUID currentGroupId = navigationStack.get(navigationStack.size() - 1);
		Group group = GroupBuilder.findGroupById(rootGraph, currentGroupId);
		if (group == null) {
			showRoot();
			return;
		}
		Graph subGraph = buildSubGraph(group);
		canvas.setGraph(subGraph);
		updateBreadcrumbs();
	}

	private void updateBreadcrumbs() {
		breadcrumbBar.getChildren().clear();

		Hyperlink rootLink = new Hyperlink(rootGraph.getName());
		rootLink.getStyleClass().add("graph-breadcrumb-link");
		rootLink.setOnAction(e -> showRoot());
		breadcrumbBar.getChildren().add(rootLink);

		for (int i = 0; i < navigationStack.size(); i++) {
			Label separator = new Label(" > ");
			separator.getStyleClass().add("graph-breadcrumb-separator");

			UUID groupId = navigationStack.get(i);
			Group group = GroupBuilder.findGroupById(rootGraph, groupId);
			String name = group != null ? group.getName() : "?";

			final int depth = i + 1;
			if (i < navigationStack.size() - 1) {
				Hyperlink link = new Hyperlink(name);
				link.getStyleClass().add("graph-breadcrumb-link");
				link.setOnAction(e -> navigateTo(depth));
				breadcrumbBar.getChildren().addAll(separator, link);
			} else {
				Label current = new Label(name);
				current.getStyleClass().add("graph-breadcrumb-current");
				breadcrumbBar.getChildren().addAll(separator, current);
			}
		}
	}

	private Graph buildSubGraph(Group group) {
		Graph subGraph = new Graph(group.getName());
		subGraph.setId(group.getId());

		for (Node node : rootGraph.getNodes()) {
			if (group.getMemberNodeIds().contains(node.getId()))
				subGraph.getNodes().add(node);
		}

		java.util.Set<UUID> memberJpIds = new java.util.HashSet<>();
		for (Node node : subGraph.getNodes()) {
			for (JoinPoint jp : node.getJoinPoints())
				memberJpIds.add(jp.getId());
		}

		for (Edge edge : rootGraph.getEdges()) {
			if (group.getMemberEdgeIds().contains(edge.getId()))
				subGraph.getEdges().add(edge);
			else if (memberJpIds.contains(edge.getSourceJoinPointId())
					&& memberJpIds.contains(edge.getTargetJoinPointId()))
				subGraph.getEdges().add(edge);
		}

		for (Group nested : rootGraph.getGroups()) {
			if (!nested.getId().equals(group.getId())
					&& group.getMemberNodeIds().containsAll(nested.getMemberNodeIds()))
				subGraph.getGroups().add(nested);
		}

		return subGraph;
	}

	public List<UUID> getNavigationStack() {
		return List.copyOf(navigationStack);
	}

	public boolean isAtRoot() {
		return navigationStack.isEmpty();
	}

	public GraphCanvas getCanvas() {
		return canvas;
	}
}
