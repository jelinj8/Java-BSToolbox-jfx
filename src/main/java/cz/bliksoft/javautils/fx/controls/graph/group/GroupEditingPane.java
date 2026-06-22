package cz.bliksoft.javautils.fx.controls.graph.group;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.model.Group;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GroupEditingPane extends VBox {

	private Group rootGraph;
	private final GraphCanvas canvas;
	private final HBox breadcrumbBar = new HBox(4);
	private final Separator breadcrumbSeparator = new Separator();
	private final List<UUID> navigationStack = new ArrayList<>();

	public GroupEditingPane(Graph rootGraph, GraphCanvas canvas) {
		this.rootGraph = rootGraph;
		this.canvas = canvas;
		canvas.setRootGraph(rootGraph);

		breadcrumbBar.getStyleClass().add("graph-breadcrumb-bar");
		breadcrumbBar.setPadding(new Insets(4, 8, 4, 8));

		VBox.setVgrow(canvas, Priority.ALWAYS);
		getChildren().addAll(breadcrumbBar, breadcrumbSeparator, canvas);

		showRoot();
	}

	public void setRootGraph(Group graph) {
		this.rootGraph = graph;
		canvas.setRootGraph(graph);
		showRoot();
	}

	public Group getRootGraph() {
		return rootGraph;
	}

	public UUID getCurrentGroupId() {
		return navigationStack.isEmpty() ? null : navigationStack.getLast();
	}

	public Group getCurrentGroup() {
		if (navigationStack.isEmpty())
			return rootGraph;
		return rootGraph.findGroup(navigationStack.getLast());
	}

	public boolean isAtRoot() {
		return navigationStack.isEmpty();
	}

	public void showRoot() {
		navigationStack.clear();
		canvas.setGraph(rootGraph);
		updateBreadcrumbs();
	}

	public void enterGroup(UUID groupId) {
		Group group = rootGraph.findGroup(groupId);
		if (group == null)
			return;

		navigationStack.add(groupId);
		canvas.setGraph(group);
		updateBreadcrumbs();
	}

	public void navigateTo(int depth) {
		if (depth == 0) {
			showRoot();
			return;
		}
		while (navigationStack.size() > depth)
			navigationStack.removeLast();

		Group group = rootGraph.findGroup(navigationStack.getLast());
		if (group == null) {
			showRoot();
			return;
		}
		canvas.setGraph(group);
		updateBreadcrumbs();
	}

	private void updateBreadcrumbs() {
		breadcrumbBar.getChildren().clear();

		boolean atRoot = navigationStack.isEmpty();
		breadcrumbBar.setVisible(!atRoot);
		breadcrumbBar.setManaged(!atRoot);
		breadcrumbSeparator.setVisible(!atRoot);
		breadcrumbSeparator.setManaged(!atRoot);

		if (atRoot)
			return;

		String rootName = rootGraph.getName() != null && !rootGraph.getName().isEmpty() ? rootGraph.getName() : "Root";
		Hyperlink rootLink = new Hyperlink(rootName);
		rootLink.getStyleClass().add("graph-breadcrumb-link");
		rootLink.setOnAction(e -> showRoot());
		breadcrumbBar.getChildren().add(rootLink);

		for (int i = 0; i < navigationStack.size(); i++) {
			Label separator = new Label(" > ");
			separator.getStyleClass().add("graph-breadcrumb-separator");

			UUID groupId = navigationStack.get(i);
			Group group = rootGraph.findGroup(groupId);
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

	public List<UUID> getNavigationStack() {
		return List.copyOf(navigationStack);
	}

	public GraphCanvas getCanvas() {
		return canvas;
	}
}
