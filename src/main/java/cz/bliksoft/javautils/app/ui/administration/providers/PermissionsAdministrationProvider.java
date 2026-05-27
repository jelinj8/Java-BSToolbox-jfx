package cz.bliksoft.javautils.app.ui.administration.providers;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.app.permissions.Permission;
import cz.bliksoft.javautils.app.permissions.Permissions;
import cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages;
import cz.bliksoft.javautils.app.ui.interfaces.IAdministrationProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class PermissionsAdministrationProvider implements IAdministrationProvider {

	private Node component = null;

	@Override
	public String getKey() {
		return "permissions";
	}

	@Override
	public String getTreeTitle() {
		return BSAppAdministrationMessages.getString("PermissionsAdministrationProvider.treeTitle");
	}

	@Override
	public String getPanelTitle() {
		return BSAppAdministrationMessages.getString("PermissionsAdministrationProvider.panelTitle");
	}

	@Override
	public Node getAdministrationComponent() {
		if (component == null)
			component = buildComponent();
		return component;
	}

	private Node buildComponent() {
		TableView<Permission> table = new TableView<>();
		table.setEditable(false);

		TableColumn<Permission, String> aliasCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("PermissionsAdministrationProvider.column.alias"));
		aliasCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAlias()));
		aliasCol.setPrefWidth(150);

		TableColumn<Permission, String> nameCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("PermissionsAdministrationProvider.column.name"));
		nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
		nameCol.setPrefWidth(200);

		TableColumn<Permission, String> categoryCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("PermissionsAdministrationProvider.column.category"));
		categoryCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
		categoryCol.setPrefWidth(150);

		TableColumn<Permission, String> descriptionCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("PermissionsAdministrationProvider.column.description"));
		descriptionCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getShortDescription()));
		descriptionCol.setPrefWidth(300);

		table.getColumns().addAll(aliasCol, nameCol, categoryCol, descriptionCol);

		List<Permission> permissions = Permissions.getFullSet().stream().map(Permissions::getPermission)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(Permission::getCategory).thenComparing(Permission::getAlias))
				.collect(Collectors.toList());
		table.setItems(FXCollections.observableArrayList(permissions));

		return table;
	}
}
