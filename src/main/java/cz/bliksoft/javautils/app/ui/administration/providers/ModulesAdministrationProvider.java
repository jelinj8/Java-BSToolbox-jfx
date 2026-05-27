package cz.bliksoft.javautils.app.ui.administration.providers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.exceptions.ViewableException;
import cz.bliksoft.javautils.app.ui.administration.BSAppAdministrationMessages;
import cz.bliksoft.javautils.app.ui.interfaces.IAdministrationProvider;
import cz.bliksoft.javautils.modules.IModule;
import cz.bliksoft.javautils.modules.Modules;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

public class ModulesAdministrationProvider implements IAdministrationProvider {

	private static final Logger log = LogManager.getLogger();

	private Node component = null;

	private static class ModuleRow {
		final IModule module;
		final BooleanProperty enabled;

		ModuleRow(IModule module) {
			this.module = module;
			this.enabled = new SimpleBooleanProperty(module.isEnabled());
		}
	}

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

	@SuppressWarnings("unchecked")
	private Node buildComponent() {
		List<ModuleRow> rows = Modules.getModules().values().stream()
				.sorted(Comparator.comparingInt(IModule::getModuleLoadingOrder)).map(ModuleRow::new)
				.collect(Collectors.toList());

		rows.forEach(row -> row.enabled.addListener((obs, o, n) -> onEnabledChanged(row.module, n)));

		TableView<ModuleRow> table = new TableView<>();
		table.setEditable(false);

		TableColumn<ModuleRow, String> nameCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.column.name"));
		nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().module.getModuleName()));
		nameCol.setPrefWidth(200);

		TableColumn<ModuleRow, String> versionCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.column.version"));
		versionCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().module.getVersionInfo()));
		versionCol.setPrefWidth(200);

		TableColumn<ModuleRow, Boolean> enabledCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.column.enabled"));
		enabledCol.setCellValueFactory(d -> d.getValue().enabled.asObject());
		enabledCol.setCellFactory(col -> new TableCell<>() {
			private final CheckBox cb = new CheckBox();
			{
				cb.setOnAction(e -> {
					ModuleRow row = getTableRow().getItem();
					if (row != null)
						row.enabled.set(cb.isSelected());
				});
			}

			@Override
			protected void updateItem(Boolean item, boolean empty) {
				super.updateItem(item, empty);
				ModuleRow row = getTableRow() != null ? getTableRow().getItem() : null;
				if (empty || item == null || row == null) {
					setGraphic(null);
				} else {
					cb.setSelected(item);
					cb.setDisable(Modules.isForceEnabled(row.module.getClass().getName()));
					setGraphic(cb);
				}
			}
		});
		enabledCol.setPrefWidth(80);

		TableColumn<ModuleRow, Integer> loadOrderCol = new TableColumn<>(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.column.loadOrder"));
		loadOrderCol.setCellValueFactory(
				d -> new SimpleIntegerProperty(d.getValue().module.getModuleLoadingOrder()).asObject());
		loadOrderCol.setPrefWidth(80);

		table.getColumns().addAll(nameCol, versionCol, enabledCol, loadOrderCol);
		table.setItems(FXCollections.observableArrayList(rows));

		Label note = new Label(
				BSAppAdministrationMessages.getString("ModulesAdministrationProvider.note.restartRequired"));
		note.getStyleClass().add("administration-note");

		BorderPane pane = new BorderPane();
		pane.setCenter(table);
		pane.setBottom(note);
		return pane;
	}

	private void onEnabledChanged(IModule module, boolean enabled) {
		module.setEnabled(enabled);
		persistModuleStates();
	}

	private void persistModuleStates() {
		String disabled = Modules.getModules().values().stream()
				.filter(m -> !m.isEnabled() && !Modules.isForceEnabled(m.getClass().getName()))
				.map(m -> m.getClass().getName()).collect(Collectors.joining(";"));

		if (disabled.isEmpty())
			BSApp.removeGlobalEnvironmentProperty(BSApp.PREF_DISABLED_MODULES);
		else
			BSApp.setGlobalEnvironmentProperty(BSApp.PREF_DISABLED_MODULES, disabled);

		try {
			BSApp.saveGlobalProperties();
		} catch (ViewableException e) {
			log.error("Failed to save module state change to global properties.", e);
		}
	}
}
