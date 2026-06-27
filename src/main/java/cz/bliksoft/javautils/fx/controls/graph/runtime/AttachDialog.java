package cz.bliksoft.javautils.fx.controls.graph.runtime;

import java.util.List;
import java.util.Optional;

import cz.bliksoft.dataflow.engine.GraphInstance;
import cz.bliksoft.dataflow.manager.GraphExecutorManager;
import cz.bliksoft.dataflow.manager.ManagedGraph;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * Modal picker with cascading combos (manager → managed graph → run instance)
 * for the "Attach" action. Returns the selection, or empty if cancelled / no
 * managers are running.
 */
public final class AttachDialog {

	private AttachDialog() {
	}

	public static Optional<AttachModel.Selection> show(Window owner) {
		List<GraphExecutorManager> managers = AttachModel.managers();
		if (managers.isEmpty()) {
			info(owner, "No running graph managers found.");
			return Optional.empty();
		}

		ComboBox<GraphExecutorManager> mgrCombo = new ComboBox<>();
		mgrCombo.getItems().addAll(managers);
		mgrCombo.setConverter(new StringConverter<>() {
			@Override
			public String toString(GraphExecutorManager m) {
				return m == null ? "" : AttachModel.managerLabel(m, mgrCombo.getItems().indexOf(m));
			}

			@Override
			public GraphExecutorManager fromString(String s) {
				return null;
			}
		});

		ComboBox<ManagedGraph> graphCombo = new ComboBox<>();
		graphCombo.setConverter(simpleConverter(AttachModel::graphLabel));

		ComboBox<AttachModel.InstanceItem> instanceCombo = new ComboBox<>();
		instanceCombo.setConverter(simpleConverter(AttachModel.InstanceItem::label));

		mgrCombo.valueProperty().addListener((o, old, m) -> {
			graphCombo.getItems().setAll(m != null ? AttachModel.graphs(m) : List.of());
			graphCombo.getSelectionModel().selectFirst();
		});
		graphCombo.valueProperty().addListener((o, old, g) -> {
			instanceCombo.getItems().setAll(g != null ? AttachModel.instances(g) : List.of());
			instanceCombo.getSelectionModel().selectFirst();
		});

		mgrCombo.setMaxWidth(Double.MAX_VALUE);
		graphCombo.setMaxWidth(Double.MAX_VALUE);
		instanceCombo.setMaxWidth(Double.MAX_VALUE);

		GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(8);
		grid.setPadding(new Insets(10));
		grid.addRow(0, new Label("Manager:"), mgrCombo);
		grid.addRow(1, new Label("Graph:"), graphCombo);
		grid.addRow(2, new Label("Instance:"), instanceCombo);

		Dialog<AttachModel.Selection> dialog = new Dialog<>();
		dialog.setTitle("Attach to Managed Graph");
		dialog.initOwner(owner);
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(grid);
		dialog.getDialogPane().setPrefWidth(460);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
		okButton.disableProperty().bind(graphCombo.valueProperty().isNull());

		mgrCombo.getSelectionModel().selectFirst();

		dialog.setResultConverter(bt -> {
			if (bt != ButtonType.OK)
				return null;
			ManagedGraph graph = graphCombo.getValue();
			if (graph == null)
				return null;
			AttachModel.InstanceItem item = instanceCombo.getValue();
			GraphInstance instance = item != null ? item.instance() : null;
			GraphExecutorManager mgr = mgrCombo.getValue();
			String label = mgr != null ? AttachModel.managerLabel(mgr, mgrCombo.getItems().indexOf(mgr)) : "";
			return new AttachModel.Selection(label, graph, instance);
		});

		return dialog.showAndWait();
	}

	private static <T> StringConverter<T> simpleConverter(java.util.function.Function<T, String> labeller) {
		return new StringConverter<>() {
			@Override
			public String toString(T value) {
				return value == null ? "" : labeller.apply(value);
			}

			@Override
			public T fromString(String s) {
				return null;
			}
		};
	}

	private static void info(Window owner, String message) {
		Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
		alert.initOwner(owner);
		alert.setHeaderText(null);
		alert.setTitle("Attach");
		alert.showAndWait();
	}
}
