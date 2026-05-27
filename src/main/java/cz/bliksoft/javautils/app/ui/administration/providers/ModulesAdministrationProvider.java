package cz.bliksoft.javautils.app.ui.administration.providers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages;
import cz.bliksoft.javautils.app.ui.interfaces.IAdministrationProvider;
import cz.bliksoft.javautils.modules.IModule;
import cz.bliksoft.javautils.modules.Modules;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ModulesAdministrationProvider implements IAdministrationProvider {

	private Node component = null;

	@Override
	public String getKey() {
		return "modules";
	}

	@Override
	public String getTreeTitle() {
		return BSAppAdministrationMessages.getString("ModulesAdministrationProvider.treeTitle");
	}

	@Override
	public String getPanelTitle() {
		return BSAppAdministrationMessages.getString("ModulesAdministrationProvider.panelTitle");
	}

	@Override
	public Node getAdministrationComponent() {
		if (component == null)
			component = buildComponent();
		return component;
	}

	private Node buildComponent() {
		TableView<IModule> table = new TableView<>();
		table.setEditable(false);

		TableColumn<IModule, String> nameCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.column.name"));
		nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getModuleName()));
		nameCol.setPrefWidth(200);

		TableColumn<IModule, String> versionCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.column.version"));
		versionCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVersionInfo()));
		versionCol.setPrefWidth(200);

		TableColumn<IModule, Boolean> enabledCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.column.enabled"));
		enabledCol.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isEnabled()).asObject());
		enabledCol.setPrefWidth(80);

		TableColumn<IModule, Integer> loadOrderCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.column.loadOrder"));
		loadOrderCol
				.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getModuleLoadingOrder()).asObject());
		loadOrderCol.setPrefWidth(80);

		table.getColumns().addAll(nameCol, versionCol, enabledCol, loadOrderCol);

		List<IModule> sorted = Modules.getModules().values().stream()
				.sorted(Comparator.comparingInt(IModule::getModuleLoadingOrder)).collect(Collectors.toList());
		table.setItems(FXCollections.observableArrayList(sorted));

		return table;
	}
}
