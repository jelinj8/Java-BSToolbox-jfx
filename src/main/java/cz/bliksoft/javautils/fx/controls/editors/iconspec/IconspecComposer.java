package cz.bliksoft.javautils.fx.controls.editors.iconspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cz.bliksoft.javautils.app.iconspec.IconspecMessages;
import cz.bliksoft.javautils.app.ui.interfaces.ITitleProvider;
import cz.bliksoft.javautils.fx.controls.codebooks.CodebookField;
import cz.bliksoft.javautils.fx.controls.codebooks.providers.basic.IconCodebookPopupProvider;
import cz.bliksoft.javautils.fx.controls.editors.multivalue.ListEditor;
import cz.bliksoft.javautils.fx.tools.IconspecCommand;
import cz.bliksoft.javautils.fx.tools.IconspecUtils;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import cz.bliksoft.javautils.images.iconspec.ImageFilter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.collections.ObservableList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Interactive composer panel for ImageUtils iconspec strings.
 *
 * <p>
 * The left side shows a {@link ListEditor} of spec steps (split by {@code #});
 * the right side shows a structured editor for the selected step. A live
 * preview is updated above the list on every change.
 *
 * <p>
 * The main input/output is {@link #iconspecProperty()}.
 */
public class IconspecComposer extends VBox {

	private static final String LBL_WIDTH = IconspecMessages.getString("IconspecComposer.param.width");
	private static final String LBL_HEIGHT = IconspecMessages.getString("IconspecComposer.param.height");
	private static final String LBL_SCALE = IconspecMessages.getString("IconspecComposer.param.scale");
	private static final String LBL_COLOR = IconspecMessages.getString("IconspecComposer.param.color");
	private static final String LBL_FILL = IconspecMessages.getString("IconspecComposer.param.fill");

	private static final Set<String> PREVIEW_SKIP_CMDS = Set.of(IconspecCommand.PUT_CACHE.cmdString,
			IconspecCommand.GET_CACHE.cmdString, IconspecCommand.CLEAR_CACHE.cmdString);

	private static final String NOCACHE_TOKEN = "*" + IconspecCommand.NOCACHE.cmdString;

	private static final List<String> DRAW_SHAPES = List.of("line", "circle", "square", "rectangle");
	private static final Map<String, String[]> DRAW_SHAPE_PARAMS = Map.of("line",
			new String[] { "x1", "y1", "x2", "y2" }, "circle", new String[] { "cx", "cy", "r" }, "square",
			new String[] { "x", "y", "side" }, "rectangle", new String[] { "x", "y", "w", "h" });
	private static final int DRAW_MAX_GEOM = 4;

	// ── State ──────────────────────────────────────────────────────────────────

	private final StringProperty iconspecProperty = new SimpleStringProperty("");
	private boolean suppressSync = false;
	private boolean updatingIconspec = false;
	private boolean updatingIconspecField = false;

	// ── Left panel ─────────────────────────────────────────────────────────────

	private final ImageView listPreview = makePreviewView();
	private final Label listPreviewSizeLabel = new Label();
	private final ListEditor<String> listEditor = new ListEditor<>();

	// ── Right panel — image editor ─────────────────────────────────────────────

	private final ImageView itemPreview = makePreviewView();
	private final Label itemPreviewSizeLabel = new Label();
	private final CodebookField<String> fileField = new CodebookField<>(new IconCodebookPopupProvider());
	private final Label[] imgParamLabels = new Label[5];
	private final TextField[] imgParamFields = new TextField[5];
	private final HBox[] imgParamRows = new HBox[5];
	private final VBox imageEditorPane;

	// ── Right panel — command editor ───────────────────────────────────────────

	private final Label commandNameLabel = new Label();
	private final VBox commandParamsBox = new VBox(6);
	private List<Supplier<String>> commandParamSuppliers = new ArrayList<>();
	private Supplier<String> commandSpecBuilder = null;
	private final VBox commandEditorPane;

	// ── Right panel ────────────────────────────────────────────────────────────

	private final Label emptyPlaceholder = new Label(IconspecMessages.getString("IconspecComposer.emptyPlaceholder"));
	private final StackPane rightPane = new StackPane();
	private final SplitPane splitPane = new SplitPane();
	private final TextField iconspecField = new TextField();

	// ──────────────────────────────────────────────────────────────────────────

	public IconspecComposer() {
		// Image param controls (5 rows; labels updated dynamically per file type)
		String[] defaultLabels = { LBL_WIDTH, LBL_HEIGHT, LBL_SCALE, LBL_COLOR, LBL_FILL };
		for (int i = 0; i < 5; i++) {
			imgParamLabels[i] = new Label(defaultLabels[i] + ":");
			imgParamLabels[i].setMinWidth(70);
			imgParamFields[i] = new TextField();
			HBox.setHgrow(imgParamFields[i], Priority.ALWAYS);
			addParamValidation(imgParamFields[i]);
			imgParamFields[i].textProperty().addListener((obs, o, n) -> {
				if (!suppressSync)
					onImageParamChanged();
			});
			imgParamRows[i] = new HBox(8, imgParamLabels[i], imgParamFields[i]);
			imgParamRows[i].setAlignment(Pos.CENTER_LEFT);
		}

		imageEditorPane = buildImageEditorPane();
		commandEditorPane = buildCommandEditorPane();

		rightPane.getChildren().addAll(emptyPlaceholder, imageEditorPane, commandEditorPane);
		showEditor(null);

		VBox.setVgrow(rightPane, Priority.ALWAYS);
		VBox rightColumn = new VBox(4, rightPane, buildTokenPanel());
		splitPane.getItems().addAll(buildLeftPanel(), rightColumn);
		splitPane.setDividerPositions(0.38);
		VBox.setVgrow(splitPane, Priority.ALWAYS);

		iconspecField.setPromptText("iconspec");

		setSpacing(4);
		setPadding(new Insets(0, 0, 4, 0));
		getChildren().addAll(splitPane, iconspecField);

		wireEvents();
	}

	// ── Layout builders ────────────────────────────────────────────────────────

	private VBox buildLeftPanel() {
		List<ListEditor.AddChoice<String>> choices = new ArrayList<>();
		choices.add(new ListEditor.AddChoice<>(title(IconspecMessages.getString("IconspecComposer.addChoice.image")),
				() -> "svg/file.svg|16"));
		for (IconspecCommand cmd : IconspecCommand.values()) {
			String item = "*" + cmd.cmdString;
			choices.add(new ListEditor.AddChoice<>(title("*" + cmd.cmdString), () -> item));
		}
		listEditor.setAddItemChoices(choices);
		listEditor.setOrderingEnabled(true);

		HBox previewBox = new HBox(listPreview);
		previewBox.setAlignment(Pos.CENTER);
		previewBox.setPadding(new Insets(4));
		previewBox.setMinHeight(80);
		listPreviewSizeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
		VBox previewPanel = new VBox(2, previewBox, listPreviewSizeLabel);
		previewPanel.setAlignment(Pos.CENTER);
		VBox.setVgrow(listEditor, Priority.ALWAYS);
		return new VBox(4, previewPanel, listEditor);
	}

	private VBox buildImageEditorPane() {
		VBox paramsBox = new VBox(4);
		paramsBox.getChildren().addAll(imgParamRows);

		HBox fileRow = new HBox(8, new Label(IconspecMessages.getString("IconspecComposer.fileRow.label")), fileField);
		fileRow.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(fileField, Priority.ALWAYS);

		HBox previewRow = new HBox(itemPreview);
		previewRow.setAlignment(Pos.CENTER_LEFT);
		previewRow.setMinHeight(80);
		itemPreviewSizeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");

		VBox pane = new VBox(8, previewRow, itemPreviewSizeLabel, fileRow, paramsBox);
		pane.setPadding(new Insets(8));
		return pane;
	}

	private VBox buildCommandEditorPane() {
		commandNameLabel.getStyleClass().add("ui-title");
		HBox header = new HBox(8, new Label(IconspecMessages.getString("IconspecComposer.commandHeader.label")),
				commandNameLabel);
		header.setAlignment(Pos.CENTER_LEFT);
		VBox pane = new VBox(8, header, commandParamsBox);
		pane.setPadding(new Insets(8));
		return pane;
	}

	@SuppressWarnings("unchecked")
	private VBox buildTokenPanel() {
		// ImageUtils tokens (e.g. scale) shown as-is; IconspecUtils vars marked with *
		List<Map.Entry<String, String>> combined = new ArrayList<>();
		ImageUtils.getRegisteredTokens().forEach((k, v) -> combined.add(Map.entry(k, v)));
		IconspecUtils.getVars().forEach((k, v) -> combined.add(Map.entry("*" + k, v)));
		combined.sort(Map.Entry.comparingByKey());

		ObservableList<Map.Entry<String, String>> entries = javafx.collections.FXCollections
				.observableArrayList(combined);

		TableView<Map.Entry<String, String>> table = new TableView<>(entries);
		table.setEditable(false);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPrefHeight(200);
		table.setPlaceholder(new Label(IconspecMessages.getString("IconspecComposer.tokenPanel.placeholder")));

		TableColumn<Map.Entry<String, String>, String> keyCol = new TableColumn<>(
				IconspecMessages.getString("IconspecComposer.tokenPanel.tokenColumn"));
		keyCol.setCellValueFactory(r -> new javafx.beans.property.SimpleStringProperty(r.getValue().getKey()));

		TableColumn<Map.Entry<String, String>, String> valCol = new TableColumn<>(
				IconspecMessages.getString("IconspecComposer.tokenPanel.valueColumn"));
		valCol.setCellValueFactory(r -> new javafx.beans.property.SimpleStringProperty(r.getValue().getValue()));

		table.getColumns().addAll(keyCol, valCol);

		// On double-click copy ${token} — strip the * marker to get the real key
		table.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
			if (e.getClickCount() == 2) {
				Map.Entry<String, String> sel = table.getSelectionModel().getSelectedItem();
				if (sel != null) {
					String key = sel.getKey();
					if (key.startsWith("*"))
						key = key.substring(1);
					copyToClipboard("${" + key + "}");
				}
			}
		});

		Label header = new Label(IconspecMessages.getString("IconspecComposer.tokenPanel.header"));
		header.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");

		VBox panel = new VBox(2, header, table);
		panel.setPadding(new Insets(4, 8, 4, 8));
		return panel;
	}

	private static void copyToClipboard(String text) {
		ClipboardContent cc = new ClipboardContent();
		cc.putString(text);
		Clipboard.getSystemClipboard().setContent(cc);
	}

	// ── Event wiring ───────────────────────────────────────────────────────────

	private void wireEvents() {
		listEditor.selectedItemProperty().addListener((obs, o, n) -> onListSelectionChanged(n));

		listEditor.getItems().addListener((ListChangeListener<String>) change -> {
			if (!suppressSync)
				rebuildIconspecFromList();
		});

		fileField.valueProperty().addListener((obs, o, n) -> {
			if (!suppressSync && n != null)
				onFileFieldChanged(n);
		});

		iconspecProperty.addListener((obs, o, n) -> {
			if (!updatingIconspecField)
				iconspecField.setText(n != null ? n : "");
			if (!updatingIconspec)
				loadIconspecIntoList(n);
		});

		iconspecField.setOnAction(e -> commitIconspecField());
		iconspecField.focusedProperty().addListener((obs, o, focused) -> {
			if (!focused)
				commitIconspecField();
		});
	}

	// ── List ↔ iconspec sync ───────────────────────────────────────────────────

	private void rebuildIconspecFromList() {
		updatingIconspec = true;
		try {
			iconspecProperty.set(String.join("#", listEditor.getItems()));
		} finally {
			updatingIconspec = false;
		}
		updateListPreview();
	}

	private void loadIconspecIntoList(String spec) {
		suppressSync = true;
		try {
			List<String> parts = (spec == null || spec.isBlank()) ? List.of()
					: Arrays.stream(spec.split("#", -1)).filter(s -> !s.isBlank()).collect(Collectors.toList());
			listEditor.loadFrom(parts);
		} finally {
			suppressSync = false;
		}
		updateListPreview();
		String sel = listEditor.getSelectedItem();
		if (sel != null)
			onListSelectionChanged(sel);
		else
			showEditor(null);
	}

	// Updates the currently selected list entry without triggering a full reload.
	private void updateSelectedListItem(String newValue) {
		suppressSync = true;
		try {
			listEditor.updateSelectedItem(newValue);
		} finally {
			suppressSync = false;
		}
		rebuildIconspecFromList();
	}

	// ── Editor population ──────────────────────────────────────────────────────

	private void onListSelectionChanged(String item) {
		updateListPreview();
		if (item == null) {
			showEditor(null);
			return;
		}
		if (item.startsWith("*")) {
			String cmdPart = item.substring(1);
			String[] parts = cmdPart.split("\\|", -1);
			IconspecCommand cmd = IconspecCommand.fromCmdString(parts[0]);
			showEditor("command");
			populateCommandEditor(cmd, Arrays.copyOfRange(parts, 1, parts.length));
		} else {
			showEditor("image");
			populateImageEditor(item);
		}
	}

	private void populateImageEditor(String item) {
		suppressSync = true;
		try {
			if (isOpaque(item)) {
				fileField.setValue(item);
				setParamConfig(null, new boolean[] { false, false, false, false, false },
						new boolean[] { false, false, false, false, false });
			} else {
				String[] parts = item.split("\\|", -1);
				String file = parts[0];
				fileField.setValue(file);

				for (int i = 0; i < 5; i++)
					imgParamFields[i].setText((i + 1 < parts.length) ? parts[i + 1] : "");

				boolean svg = file.toLowerCase().endsWith(".svg");
				boolean empty = "EMPTY".equalsIgnoreCase(file);

				if (svg) {
					setParamConfig(new String[] { LBL_WIDTH, LBL_HEIGHT, LBL_SCALE, LBL_COLOR, LBL_FILL },
							new boolean[] { true, true, true, true, true },
							new boolean[] { true, true, true, true, true });
				} else if (empty) {
					setParamConfig(new String[] { LBL_WIDTH, LBL_HEIGHT, LBL_COLOR, null, null },
							new boolean[] { true, true, true, false, false },
							new boolean[] { true, true, true, false, false });
				} else {
					setParamConfig(new String[] { LBL_WIDTH, LBL_HEIGHT, LBL_SCALE, LBL_COLOR, LBL_FILL },
							new boolean[] { true, true, true, true, true },
							new boolean[] { false, false, false, false, false });
				}
			}
		} finally {
			suppressSync = false;
		}
		updateItemPreview(item);
	}

	private void setParamConfig(String[] labels, boolean[] visible, boolean[] enabled) {
		for (int i = 0; i < 5; i++) {
			if (labels != null && i < labels.length && labels[i] != null)
				imgParamLabels[i].setText(labels[i] + ":");
			boolean vis = i < visible.length && visible[i];
			boolean en = i < enabled.length && enabled[i];
			imgParamRows[i].setVisible(vis);
			imgParamRows[i].setManaged(vis);
			imgParamFields[i].setDisable(!en);
		}
	}

	private void populateCommandEditor(IconspecCommand cmd, String[] params) {
		commandParamsBox.getChildren().clear();
		commandParamSuppliers.clear();
		commandSpecBuilder = null;

		if (cmd == null) {
			commandNameLabel.setText("?");
			commandParamsBox.getChildren()
					.add(new Label(IconspecMessages.getString("IconspecComposer.command.unknown")));
			return;
		}

		commandNameLabel.setText(cmd.cmdString);
		boolean isCacheCmd = cmd == IconspecCommand.PUT_CACHE || cmd == IconspecCommand.GET_CACHE
				|| cmd == IconspecCommand.CLEAR_CACHE;

		if (cmd.paramNames.length == 0) {
			commandParamsBox.getChildren()
					.add(new Label(IconspecMessages.getString("IconspecComposer.command.noParams")));
			return;
		}

		if (cmd == IconspecCommand.FILTER || cmd == IconspecCommand.COMPOSE_FILTER) {
			buildFilterCommandEditor(params);
			return;
		}

		if (cmd == IconspecCommand.DRAW) {
			buildDrawCommandEditor(params);
			return;
		}

		for (int i = 0; i < cmd.paramNames.length; i++) {
			String currentValue = (i < params.length) ? params[i] : "";
			Label lbl = new Label(cmd.paramNames[i] + ":");
			lbl.setMinWidth(70);

			Node control;
			Supplier<String> supplier;

			if ((cmd == IconspecCommand.COMPOSE_OVER || cmd == IconspecCommand.COMPOSE_OUT) && i == 0) {
				ChoiceBox<String> cb = new ChoiceBox<>(FXCollections.observableArrayList("", "C", "E"));
				String cv = currentValue.isEmpty() ? "" : currentValue.toUpperCase();
				cb.setValue(cb.getItems().contains(cv) ? cv : "");
				cb.valueProperty().addListener((obs, o, n) -> {
					if (!suppressSync)
						onCommandParamChanged();
				});
				control = cb;
				supplier = () -> cb.getValue() != null ? cb.getValue() : "";
			} else if (cmd == IconspecCommand.ANCHOR && i == 0) {
				ChoiceBox<String> cb = new ChoiceBox<>(
						FXCollections.observableArrayList("BR", "TL", "TR", "BL", "C", "N"));
				String cv = currentValue.isEmpty() ? "BR" : currentValue.toUpperCase();
				cb.setValue(cb.getItems().contains(cv) ? cv : "BR");
				cb.valueProperty().addListener((obs, o, n) -> {
					if (!suppressSync)
						onCommandParamChanged();
				});
				control = cb;
				supplier = () -> cb.getValue() != null ? cb.getValue() : "BR";
			} else {
				TextField tf = new TextField(currentValue);
				if (isCacheCmd)
					tf.setPromptText(IconspecMessages.getString("IconspecComposer.command.cacheKeyPrompt"));
				addParamValidation(tf);
				tf.textProperty().addListener((obs, o, n) -> {
					if (!suppressSync)
						onCommandParamChanged();
				});
				control = tf;
				supplier = tf::getText;
			}

			commandParamSuppliers.add(supplier);
			HBox row = new HBox(8, lbl, control);
			row.setAlignment(Pos.CENTER_LEFT);
			HBox.setHgrow(control, Priority.ALWAYS);
			commandParamsBox.getChildren().add(row);
		}

		if (isCacheCmd) {
			Label note = new Label(IconspecMessages.getString("IconspecComposer.command.noPreviewEffect"));
			note.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");
			commandParamsBox.getChildren().add(note);
		}
	}

	private void buildFilterCommandEditor(String[] params) {
		// params[0] = filter name, params[1..3] = positional filter params
		List<String> filterNames = Arrays.stream(ImageFilter.values()).map(f -> f.filterName)
				.collect(Collectors.toList());
		String currentFilter = (params.length > 0 && !params[0].isEmpty()) ? params[0].toLowerCase()
				: filterNames.get(0);

		ChoiceBox<String> filterCb = new ChoiceBox<>(FXCollections.observableArrayList(filterNames));
		filterCb.setValue(currentFilter);

		Label filterLbl = new Label(IconspecMessages.getString("IconspecComposer.filter.label"));
		filterLbl.setMinWidth(70);
		HBox filterRow = new HBox(8, filterLbl, filterCb);
		filterRow.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(filterCb, Priority.ALWAYS);
		commandParamsBox.getChildren().add(filterRow);
		commandParamSuppliers.add(() -> filterCb.getValue() != null ? filterCb.getValue() : "");

		// Up to N positional param rows (max across all filters)
		int maxParams = Arrays.stream(ImageFilter.values()).mapToInt(f -> f.paramNames.length).max().orElse(3);
		Label[] pLabels = new Label[maxParams];
		TextField[] pFields = new TextField[maxParams];
		HBox[] pRows = new HBox[maxParams];

		// MIRROR filter uses a ChoiceBox for its single "direction" param
		String initDir = (params.length > 1 && !params[1].isEmpty()) ? params[1].toUpperCase() : "H";
		ChoiceBox<String> dirCb = new ChoiceBox<>(FXCollections.observableArrayList("H", "V"));
		dirCb.setValue(dirCb.getItems().contains(initDir) ? initDir : "H");
		dirCb.setVisible(false);
		dirCb.setManaged(false);
		HBox.setHgrow(dirCb, Priority.ALWAYS);
		dirCb.valueProperty().addListener((obs, o, n) -> {
			if (!suppressSync)
				onCommandParamChanged();
		});

		for (int i = 0; i < maxParams; i++) {
			pLabels[i] = new Label("p" + (i + 1) + ":");
			pLabels[i].setMinWidth(70);
			pFields[i] = new TextField(params.length > i + 1 ? params[i + 1] : "");
			addParamValidation(pFields[i]);
			final int fi = i;
			pFields[i].textProperty().addListener((obs, o, n) -> {
				if (!suppressSync)
					onCommandParamChanged();
			});
			if (i == 0) {
				commandParamSuppliers.add(() -> dirCb.isVisible() ? (dirCb.getValue() != null ? dirCb.getValue() : "H")
						: pFields[0].getText());
				pRows[0] = new HBox(8, pLabels[0], pFields[0], dirCb);
			} else {
				commandParamSuppliers.add(() -> pFields[fi].getText());
				pRows[i] = new HBox(8, pLabels[i], pFields[i]);
			}
			pRows[i].setAlignment(Pos.CENTER_LEFT);
			HBox.setHgrow(pFields[i], Priority.ALWAYS);
			commandParamsBox.getChildren().add(pRows[i]);
		}

		// Updates labels and visibility to match the selected filter
		Runnable syncToFilter = () -> {
			String fname = filterCb.getValue();
			ImageFilter f = fname != null ? ImageFilter.fromName(fname) : null;
			boolean isMirror = f == ImageFilter.MIRROR;
			int count = f != null ? f.paramNames.length : maxParams;
			for (int i = 0; i < maxParams; i++) {
				boolean show = i < count;
				pLabels[i].setText((f != null && i < f.paramNames.length ? f.paramNames[i] : "p" + (i + 1)) + ":");
				pRows[i].setVisible(show);
				pRows[i].setManaged(show);
				if (i == 0) {
					pFields[0].setVisible(!isMirror);
					pFields[0].setManaged(!isMirror);
					dirCb.setVisible(isMirror);
					dirCb.setManaged(isMirror);
				}
			}
		};

		filterCb.valueProperty().addListener((obs, o, n) -> {
			syncToFilter.run();
			if (!suppressSync)
				onCommandParamChanged();
		});
		syncToFilter.run(); // initialise for the current filter
	}

	private void buildDrawCommandEditor(String[] params) {
		// params[0] = shape, params[1..geomCount] = geom values, params[geomCount+1] =
		// t
		String currentShape = (params.length > 0 && DRAW_SHAPES.contains(params[0])) ? params[0] : DRAW_SHAPES.get(0);
		String[] initGeomNames = DRAW_SHAPE_PARAMS.get(currentShape);
		int initGeomCount = initGeomNames != null ? initGeomNames.length : DRAW_MAX_GEOM;

		ChoiceBox<String> shapeCb = new ChoiceBox<>(FXCollections.observableArrayList(DRAW_SHAPES));
		shapeCb.setValue(currentShape);
		Label shapeLbl = new Label(IconspecMessages.getString("IconspecComposer.shape.label"));
		shapeLbl.setMinWidth(70);
		HBox shapeRow = new HBox(8, shapeLbl, shapeCb);
		shapeRow.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(shapeCb, Priority.ALWAYS);
		commandParamsBox.getChildren().add(shapeRow);

		Label[] gLabels = new Label[DRAW_MAX_GEOM];
		TextField[] gFields = new TextField[DRAW_MAX_GEOM];
		HBox[] gRows = new HBox[DRAW_MAX_GEOM];
		for (int i = 0; i < DRAW_MAX_GEOM; i++) {
			String initVal = (i < initGeomCount && params.length > i + 1) ? params[i + 1] : "";
			gLabels[i] = new Label("p" + (i + 1) + ":");
			gLabels[i].setMinWidth(70);
			gFields[i] = new TextField(initVal);
			addParamValidation(gFields[i]);
			gFields[i].textProperty().addListener((obs, o, n) -> {
				if (!suppressSync)
					onCommandParamChanged();
			});
			gRows[i] = new HBox(8, gLabels[i], gFields[i]);
			gRows[i].setAlignment(Pos.CENTER_LEFT);
			HBox.setHgrow(gFields[i], Priority.ALWAYS);
			commandParamsBox.getChildren().add(gRows[i]);
		}

		// t: stroke width override, comes right after geom params in spec
		String initT = (params.length > initGeomCount + 1) ? params[initGeomCount + 1] : "";
		Label tLbl = new Label(IconspecMessages.getString("IconspecComposer.stroke.label"));
		tLbl.setMinWidth(70);
		TextField tField = new TextField(initT);
		tField.setPromptText(IconspecMessages.getString("IconspecComposer.stroke.prompt"));
		addParamValidation(tField);
		tField.textProperty().addListener((obs, o, n) -> {
			if (!suppressSync)
				onCommandParamChanged();
		});
		HBox tRow = new HBox(8, tLbl, tField);
		tRow.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(tField, Priority.ALWAYS);
		commandParamsBox.getChildren().add(tRow);

		Runnable syncToShape = () -> {
			String sname = shapeCb.getValue();
			String[] sParams = sname != null ? DRAW_SHAPE_PARAMS.get(sname) : null;
			int count = sParams != null ? sParams.length : DRAW_MAX_GEOM;
			for (int i = 0; i < DRAW_MAX_GEOM; i++) {
				boolean show = i < count;
				gLabels[i].setText((sParams != null && i < sParams.length ? sParams[i] : "p" + (i + 1)) + ":");
				gRows[i].setVisible(show);
				gRows[i].setManaged(show);
			}
		};

		shapeCb.valueProperty().addListener((obs, o, n) -> {
			syncToShape.run();
			if (!suppressSync)
				onCommandParamChanged();
		});
		syncToShape.run();

		// Build spec with t immediately after geom params (position varies by shape)
		commandSpecBuilder = () -> {
			String sname = shapeCb.getValue();
			if (sname == null || sname.isEmpty())
				sname = "line";
			String[] sParams = DRAW_SHAPE_PARAMS.get(sname);
			int geomCount = sParams != null ? sParams.length : 0;
			String[] allParams = new String[1 + geomCount + 1]; // shape + geoms + t
			allParams[0] = sname;
			for (int i = 0; i < geomCount; i++)
				allParams[i + 1] = gFields[i].getText();
			allParams[1 + geomCount] = tField.getText();
			return "*" + joinSpec("DRAW", allParams);
		};
	}

	// ── Editor → list item reconstruction ─────────────────────────────────────

	private void onImageParamChanged() {
		String item = listEditor.getSelectedItem();
		if (item == null || item.startsWith("*"))
			return;
		String rebuilt = buildImageItemString(item);
		updateSelectedListItem(rebuilt);
		updateItemPreview(rebuilt);
	}

	private String buildImageItemString(String originalItem) {
		if (isOpaque(originalItem)) {
			String val = fileField.getValue();
			return val != null ? val : originalItem;
		}
		String file = fileField.getValue();
		if (file == null || file.isBlank())
			return "";
		if (!isSvgOrEmpty(file))
			return file;

		int maxParams = "EMPTY".equalsIgnoreCase(file) ? 3 : 5;
		String[] params = new String[maxParams];
		for (int i = 0; i < maxParams && i < imgParamFields.length; i++)
			params[i] = imgParamFields[i].getText();

		return joinSpec(file, params);
	}

	private void onFileFieldChanged(String newFile) {
		String item = listEditor.getSelectedItem();
		if (item == null || item.startsWith("*"))
			return;

		if (isOpaque(item) || isOpaque(newFile)) {
			updateSelectedListItem(newFile);
			updateItemPreview(newFile);
			return;
		}

		boolean svg = newFile.toLowerCase().endsWith(".svg");
		boolean empty = "EMPTY".equalsIgnoreCase(newFile);
		boolean editable = svg || empty;

		String rebuilt;
		if (editable) {
			int maxParams = empty ? 3 : 5;
			String[] params = new String[maxParams];
			for (int i = 0; i < maxParams && i < imgParamFields.length; i++)
				params[i] = imgParamFields[i].getText();
			rebuilt = joinSpec(newFile, params);

			suppressSync = true;
			try {
				if (empty) {
					setParamConfig(new String[] { LBL_WIDTH, LBL_HEIGHT, LBL_COLOR, null, null },
							new boolean[] { true, true, true, false, false },
							new boolean[] { true, true, true, false, false });
				} else {
					setParamConfig(new String[] { LBL_WIDTH, LBL_HEIGHT, LBL_SCALE, LBL_COLOR, LBL_FILL },
							new boolean[] { true, true, true, true, true },
							new boolean[] { true, true, true, true, true });
				}
			} finally {
				suppressSync = false;
			}
		} else {
			rebuilt = newFile;
			suppressSync = true;
			try {
				setParamConfig(new String[] { LBL_WIDTH, LBL_HEIGHT, LBL_SCALE, LBL_COLOR, LBL_FILL },
						new boolean[] { true, true, true, true, true },
						new boolean[] { false, false, false, false, false });
			} finally {
				suppressSync = false;
			}
		}

		updateSelectedListItem(rebuilt);
		updateItemPreview(rebuilt);
	}

	private void onCommandParamChanged() {
		String item = listEditor.getSelectedItem();
		if (item == null || !item.startsWith("*"))
			return;
		String rebuilt;
		if (commandSpecBuilder != null) {
			rebuilt = commandSpecBuilder.get();
		} else {
			String cmdName = item.substring(1).split("\\|", 2)[0];
			String[] paramValues = commandParamSuppliers.stream().map(Supplier::get).toArray(String[]::new);
			rebuilt = "*" + joinSpec(cmdName, paramValues);
		}
		updateSelectedListItem(rebuilt);
	}

	// ── Preview ────────────────────────────────────────────────────────────────

	private void updateListPreview() {
		List<String> items = listEditor.getItems();
		if (items.isEmpty()) {
			listPreview.setImage(null);
			updateSizeLabel(listPreviewSizeLabel, null);
			return;
		}
		int selIdx = listEditor.getSelectedIndex();
		int upTo = selIdx >= 0 ? selIdx : items.size() - 1; // no selection → show full spec
		List<String> previewParts = new ArrayList<>();
		for (int i = 0; i <= upTo && i < items.size(); i++) {
			String it = items.get(i);
			if (it.startsWith("*")) {
				String cmdName = it.substring(1).split("\\|", 2)[0];
				if (PREVIEW_SKIP_CMDS.contains(cmdName))
					continue;
			}
			previewParts.add(it);
		}
		if (previewParts.isEmpty()) {
			listPreview.setImage(null);
			updateSizeLabel(listPreviewSizeLabel, null);
			return;
		}
		previewParts.add(NOCACHE_TOKEN);
		String spec = IconspecUtils.substituteSpec(String.join("#", previewParts));
		Image img = ImageUtils.getImageIfPossible(spec, false);
		listPreview.setImage(img);
		updateSizeLabel(listPreviewSizeLabel, img);
	}

	private void updateItemPreview(String item) {
		if (item == null || item.isBlank() || item.startsWith("*")) {
			itemPreview.setImage(null);
			updateSizeLabel(itemPreviewSizeLabel, null);
			return;
		}
		String spec = IconspecUtils.substituteSpec(item + "#" + NOCACHE_TOKEN);
		Image img = ImageUtils.getImageIfPossible(spec, false);
		itemPreview.setImage(img);
		updateSizeLabel(itemPreviewSizeLabel, img);
	}

	private static void updateSizeLabel(Label label, Image img) {
		if (img == null) {
			label.setText("");
		} else {
			label.setText((int) img.getWidth() + " × " + (int) img.getHeight() + " px");
		}
	}

	// ── Helpers ────────────────────────────────────────────────────────────────

	private record TitleStr(String text) implements ITitleProvider {
		@Override
		public String getTitle() {
			return text;
		}
	}

	private static ITitleProvider title(String text) {
		return new TitleStr(text);
	}

	private static boolean isOpaque(String spec) {
		return spec != null && (spec.startsWith("[P]:") || spec.startsWith("[PI]:") || spec.startsWith("[PS]:"));
	}

	private static boolean isSvgOrEmpty(String file) {
		return file != null && (file.toLowerCase().endsWith(".svg") || "EMPTY".equalsIgnoreCase(file));
	}

	/** Joins {@code base|p0|p1...} trimming trailing empty slots. */
	private static String joinSpec(String base, String[] params) {
		if (params == null || params.length == 0)
			return base;
		int lastNonEmpty = -1;
		for (int i = params.length - 1; i >= 0; i--) {
			if (params[i] != null && !params[i].isEmpty()) {
				lastNonEmpty = i;
				break;
			}
		}
		if (lastNonEmpty < 0)
			return base;
		StringBuilder sb = new StringBuilder(base);
		for (int i = 0; i <= lastNonEmpty; i++) {
			sb.append('|');
			if (params[i] != null)
				sb.append(params[i]);
		}
		return sb.toString();
	}

	/** Installs a listener that strips {@code |} and {@code #} from user input. */
	private static void addParamValidation(TextField tf) {
		tf.textProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null && (newVal.contains("|") || newVal.contains("#")))
				tf.setText(newVal.replace("|", "").replace("#", ""));
		});
	}

	private void showEditor(String which) {
		for (Node n : rightPane.getChildren()) {
			n.setVisible(false);
			n.setManaged(false);
		}
		Node toShow = "image".equals(which) ? imageEditorPane
				: "command".equals(which) ? commandEditorPane : emptyPlaceholder;
		toShow.setVisible(true);
		toShow.setManaged(true);
	}

	/**
	 * A large source image (e.g. a big raster file, or an SVG rendered at a large
	 * declared size) must never grow the preview past this box - an unconstrained
	 * {@link ImageView} reports the raw image's pixel size as its own layout size,
	 * which then balloons every containing row/pane up through the composer and
	 * the dialog it's hosted in (see {@code IconspecComposerDialog}), with no way
	 * to shrink it back. Fixed {@code fitWidth}/{@code fitHeight} plus
	 * {@code preserveRatio} scales any image down to fit within this box instead
	 * (never up - {@link ImageView} doesn't upscale past the fit size only when
	 * the source is already smaller, which is fine for a thumbnail preview).
	 */
	private static final double PREVIEW_WIDTH = 120;
	private static final double PREVIEW_HEIGHT = 80;

	private static ImageView makePreviewView() {
		ImageView view = new ImageView();
		view.setPreserveRatio(true);
		view.setFitWidth(PREVIEW_WIDTH);
		view.setFitHeight(PREVIEW_HEIGHT);
		return view;
	}

	private void commitIconspecField() {
		updatingIconspecField = true;
		try {
			setIconspec(iconspecField.getText());
		} finally {
			updatingIconspecField = false;
		}
	}

	// ── Public API ─────────────────────────────────────────────────────────────

	public StringProperty iconspecProperty() {
		return iconspecProperty;
	}

	public String getIconspec() {
		return iconspecProperty.get();
	}

	public void setIconspec(String spec) {
		iconspecProperty.set(spec != null ? spec : "");
	}
}
