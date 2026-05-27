package cz.bliksoft.javautils.app.ui.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.exceptions.ViewableException;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;

/**
 * A slot that holds a single configurable {@link IWidget}.
 *
 * <p>
 * When both {@link #setPlacementGroup(String) placementGroup} and
 * {@link #setPlacementID(String) placementID} are set, the container
 * automatically loads the previously saved widget from local properties.
 *
 * <p>
 * Right-clicking the container (when {@link #isConfigurable() configurable})
 * shows a context menu to add, replace, or remove the current widget.
 *
 * <p>
 * Widget types are discovered from the XmlFilesystem under
 * {@code core/widgets}. Register a factory by adding a {@code <file>} entry
 * with the fully-qualified factory class name in that path.
 */
public class WidgetContainer extends StackPane {

	private static final Logger log = LogManager.getLogger();

	private static final String WIDGETS_FOLDER = "widgets";
	private static final String PROP_KEY_PREFIX = "widget.";

	// Shared across all containers — loaded once on first selectWidget() call
	private static Map<String, List<IWidgetFactory>> widgetFactories = null;

	// --- State ---

	private String placementID = null;
	private String placementGroup = null;
	private boolean configurable = true;
	private double constrainedMaxWidth = 0;
	private double constrainedMaxHeight = 0;
	private String defaultWidgetFactoryName = null;

	private IWidget contents = null;
	private String activeFactoryName = null;

	public WidgetContainer() {
		getStyleClass().add("widget-container");
		getStyleClass().add("widget-container-empty");

		setOnContextMenuRequested(e -> {
			ContextMenu menu = buildContextMenu();
			if (menu != null) {
				menu.show(this, e.getScreenX(), e.getScreenY());
			}
			e.consume();
		});
	}

	// --- Public API ---

	public IWidget getContents() {
		return contents;
	}

	/**
	 * Sets the widget displayed in this container. Triggers a save if both
	 * placement keys are set. Pass {@code null} to leave the container empty.
	 */
	public void setContents(IWidget widget) {
		setContentsInternal(widget);
		save();
	}

	public String getPlacementID() {
		return placementID;
	}

	public void setPlacementID(String id) {
		this.placementID = id;
		if (bothKeysSet())
			load();
	}

	public String getPlacementGroup() {
		return placementGroup;
	}

	public void setPlacementGroup(String group) {
		this.placementGroup = group;
		if (bothKeysSet())
			load();
	}

	public boolean isConfigurable() {
		return configurable;
	}

	public void setConfigurable(boolean configurable) {
		this.configurable = configurable;
	}

	/** Max-width constraint for the widget selector filter (0 = no constraint). */
	public double getConstrainedMaxWidth() {
		return constrainedMaxWidth;
	}

	public void setConstrainedMaxWidth(double maxWidth) {
		this.constrainedMaxWidth = maxWidth;
	}

	/** Max-height constraint for the widget selector filter (0 = no constraint). */
	public double getConstrainedMaxHeight() {
		return constrainedMaxHeight;
	}

	public void setConstrainedMaxHeight(double maxHeight) {
		this.constrainedMaxHeight = maxHeight;
	}

	/**
	 * Factory name ({@link IWidgetFactory#getName()}) to use when no widget has
	 * been persisted by the user. Set this to give the slot a meaningful default.
	 */
	public String getDefaultWidgetFactoryName() {
		return defaultWidgetFactoryName;
	}

	public void setDefaultWidgetFactoryName(String name) {
		this.defaultWidgetFactoryName = name;
	}

	// --- Selection dialog ---

	/**
	 * Opens the widget selector dialog. On confirmation, installs the selected
	 * widget and saves the choice.
	 */
	public void selectWidget() {
		ensureFactoriesLoaded();

		Map<String, List<IWidgetFactory>> fitting = filterBySize();
		if (fitting.isEmpty()) {
			log.warn("No widgets fit the size constraints of this container.");
			return;
		}

		Optional<IWidgetFactory> result = showSelectorDialog(fitting);
		result.ifPresent(factory -> {
			activeFactoryName = factory.getName();
			setContentsInternal(factory.create());
			save();
		});
	}

	// --- Internal ---

	private void setContentsInternal(IWidget widget) {
		if (contents != null) {
			getChildren().remove(contents.getComponent());
			contents.cleanup();
		}
		contents = widget;
		if (widget == null)
			activeFactoryName = null;
		getStyleClass().removeAll("widget-container-empty", "widget-container-filled");
		if (widget != null) {
			getChildren().add(widget.getComponent());
			getStyleClass().add("widget-container-filled");
		} else {
			getStyleClass().add("widget-container-empty");
		}
	}

	private ContextMenu buildContextMenu() {
		if (!configurable && contents == null)
			return null;

		if (!configurable)
			return contents.getContextMenu();

		ContextMenu menu = new ContextMenu();

		if (contents == null) {
			MenuItem add = new MenuItem(BSAppWidgetMessages.getString("WidgetContainer.action.add"));
			add.setOnAction(e -> selectWidget());
			menu.getItems().add(add);
		} else {
			MenuItem replace = new MenuItem(BSAppWidgetMessages.getString("WidgetContainer.action.replace"));
			replace.setOnAction(e -> selectWidget());
			MenuItem remove = new MenuItem(BSAppWidgetMessages.getString("WidgetContainer.action.remove"));
			remove.setOnAction(e -> removeWidget());
			menu.getItems().addAll(replace, remove);
		}

		if (defaultWidgetFactoryName != null) {
			MenuItem reset = new MenuItem(BSAppWidgetMessages.getString("WidgetContainer.action.reset"));
			reset.setOnAction(e -> resetWidget());
			menu.getItems().add(reset);
		}

		return menu;
	}

	private void removeWidget() {
		setContentsInternal(null);
		saveExplicitlyEmpty();
	}

	private void resetWidget() {
		if (bothKeysSet()) {
			BSApp.removeLocalProperty(propertyKey());
			try {
				BSApp.saveLocalProperties();
			} catch (ViewableException e) {
				log.error("Failed to save local properties after widget reset.", e);
			}
		}
		load();
	}

	private void load() {
		ensureFactoriesLoaded();

		String saved = BSApp.getLocalProperties().getProperty(propertyKey(), null);

		if (saved == null) {
			// No persisted choice → use default if configured
			if (defaultWidgetFactoryName != null) {
				IWidgetFactory factory = findFactory(defaultWidgetFactoryName);
				if (factory != null) {
					activeFactoryName = factory.getName();
					setContentsInternal(factory.create());
				} else {
					setContentsInternal(null);
				}
			} else {
				setContentsInternal(null);
			}
		} else if (saved.isEmpty()) {
			// Explicitly cleared by user
			setContentsInternal(null);
		} else {
			IWidgetFactory factory = findFactory(saved);
			if (factory != null) {
				activeFactoryName = factory.getName();
				setContentsInternal(factory.create());
			} else {
				log.warn("Widget factory '{}' not found for slot {}.{} — leaving empty.", saved, placementGroup,
						placementID);
				setContentsInternal(null);
			}
		}
	}

	private void save() {
		if (!bothKeysSet())
			return;
		String value = activeFactoryName;
		if (value == null)
			return;
		BSApp.setLocalProperty(propertyKey(), value);
		try {
			BSApp.saveLocalProperties();
		} catch (ViewableException e) {
			log.error("Failed to save widget selection for slot {}.{}.", placementGroup, placementID, e);
		}
	}

	private void saveExplicitlyEmpty() {
		if (!bothKeysSet())
			return;
		BSApp.setLocalProperty(propertyKey(), "");
		try {
			BSApp.saveLocalProperties();
		} catch (ViewableException e) {
			log.error("Failed to save widget removal for slot {}.{}.", placementGroup, placementID, e);
		}
	}

	private String propertyKey() {
		return PROP_KEY_PREFIX + placementGroup + "." + placementID;
	}

	private boolean bothKeysSet() {
		return placementGroup != null && !placementGroup.isBlank()
				&& placementID != null && !placementID.isBlank();
	}

	private IWidgetFactory findFactory(String name) {
		if (widgetFactories == null)
			return null;
		for (List<IWidgetFactory> list : widgetFactories.values()) {
			for (IWidgetFactory f : list) {
				if (name.equals(f.getName()))
					return f;
			}
		}
		return null;
	}

	private Map<String, List<IWidgetFactory>> filterBySize() {
		Map<String, List<IWidgetFactory>> result = new HashMap<>();
		for (Map.Entry<String, List<IWidgetFactory>> entry : widgetFactories.entrySet()) {
			List<IWidgetFactory> fits = new ArrayList<>();
			for (IWidgetFactory f : entry.getValue()) {
				if ((constrainedMaxWidth <= 0 || f.getMinWidth() <= constrainedMaxWidth)
						&& (constrainedMaxHeight <= 0 || f.getMinHeight() <= constrainedMaxHeight)) {
					fits.add(f);
				}
			}
			if (!fits.isEmpty())
				result.put(entry.getKey(), fits);
		}
		return result;
	}

	private static void ensureFactoriesLoaded() {
		if (widgetFactories != null)
			return;
		synchronized (WidgetContainer.class) {
			if (widgetFactories != null)
				return;
			widgetFactories = new HashMap<>();
			log.debug("Loading widget factories from filesystem.");
			FileObject folder = FileSystem.getFile(BSApp.CORE_CONFIG_FOLDER, WIDGETS_FOLDER);
			if (folder != null) {
				WidgetFactoryFileLoader loader = new WidgetFactoryFileLoader();
				for (FileObject fo : folder.getChildFiles()) {
					IWidgetFactory factory = loader.tryLoadFile(fo);
					if (factory == null)
						continue;
					widgetFactories.computeIfAbsent(factory.getCategory(), k -> new ArrayList<>()).add(factory);
					log.debug("Loaded widget factory {}", fo.getName());
				}
			}
		}
	}

	// --- Selector dialog ---

	private Optional<IWidgetFactory> showSelectorDialog(Map<String, List<IWidgetFactory>> factories) {
		TreeItem<Object> root = new TreeItem<>();
		for (Map.Entry<String, List<IWidgetFactory>> entry : factories.entrySet()) {
			TreeItem<Object> categoryNode = new TreeItem<>(entry.getKey());
			categoryNode.setExpanded(true);
			for (IWidgetFactory f : entry.getValue()) {
				categoryNode.getChildren().add(new TreeItem<>(f));
			}
			root.getChildren().add(categoryNode);
		}

		TreeView<Object> tree = new TreeView<>(root);
		tree.setShowRoot(false);
		tree.setPrefHeight(300);
		tree.setPrefWidth(280);
		tree.setCellFactory(tv -> new TreeCell<>() {
			@Override
			protected void updateItem(Object item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else if (item instanceof IWidgetFactory f) {
					setText(f.getLocalizedName());
				} else {
					setText(item.toString());
				}
			}
		});

		Dialog<IWidgetFactory> dialog = new Dialog<>();
		dialog.setTitle(BSAppWidgetMessages.getString("WidgetContainer.dialog.title"));
		dialog.getDialogPane().setContent(tree);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		// Disable OK until a factory leaf is selected
		Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
		okButton.setDisable(true);
		tree.getSelectionModel().selectedItemProperty().addListener((obs, o, now) -> {
			boolean isFactory = now != null && now.getValue() instanceof IWidgetFactory;
			okButton.setDisable(!isFactory);
		});

		dialog.setResultConverter(bt -> {
			if (bt == ButtonType.OK) {
				TreeItem<Object> sel = tree.getSelectionModel().getSelectedItem();
				if (sel != null && sel.getValue() instanceof IWidgetFactory f)
					return f;
			}
			return null;
		});

		// Inherit owner window
		if (getScene() != null && getScene().getWindow() != null) {
			DialogPane dp = dialog.getDialogPane();
			dp.getScene().getWindow().sizeToScene();
			dialog.initOwner(getScene().getWindow());
		}

		return dialog.showAndWait();
	}
}
