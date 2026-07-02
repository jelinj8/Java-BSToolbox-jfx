package cz.bliksoft.javautils.app.ui.administration;

import java.util.Optional;

import cz.bliksoft.javautils.app.BSAppJFX;
import cz.bliksoft.javautils.app.permissions.Permission;
import cz.bliksoft.javautils.app.permissions.Permissions;
import cz.bliksoft.javautils.app.ui.BSAppUI;
import cz.bliksoft.javautils.app.ui.utils.StageAutoSizer;
import cz.bliksoft.javautils.app.ui.actions.interfaces.IClose;
import cz.bliksoft.javautils.app.ui.actions.interfaces.ISaveAll;
import cz.bliksoft.javautils.app.ui.interfaces.IAdministrationProvider;
import cz.bliksoft.javautils.app.ui.interfaces.IStackedComponent;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateManager;
import cz.bliksoft.javautils.app.ui.utils.state.FxStateMeta;
import cz.bliksoft.javautils.context.Context;
import cz.bliksoft.javautils.context.IContextProvider;
import cz.bliksoft.javautils.context.holders.SingleContextHolder;
import cz.bliksoft.javautils.fx.customization.BSButtonTypes;
import cz.bliksoft.javautils.fx.tools.ImageUtils;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class AdministrationPanel extends SplitPane implements IContextProvider, IClose, IStackedComponent {

	public static final String ADMINISTRATION_FOLDER_NAME = "administration";

	// Sealed tree item types
	public sealed interface AdministrationTreeItem permits ProviderItem, GroupItem {
	}

	public record ProviderItem(IAdministrationProvider provider) implements AdministrationTreeItem {
	}

	public record GroupItem(String label, String iconSpec) implements AdministrationTreeItem {
	}

	private static boolean isOpen = false;

	public static boolean isOpen() {
		return isOpen;
	}

	private final Context levelContext = new Context("AdministrationPanel");
	private final SingleContextHolder providerContextHolder = new SingleContextHolder(
			"Administration provider context");

	private final TreeView<AdministrationTreeItem> tree = new TreeView<>();
	private final BorderPane contentArea = new BorderPane();
	private final BooleanProperty closeEnabled = new SimpleBooleanProperty(true);

	private final FxStateManager stateManager = new FxStateManager("AdministrationPanel");

	private IAdministrationProvider activeProvider = null;
	private ChangeListener<Boolean> saveStateListener = null;
	private boolean suppressSelectionEvent = false;

	public AdministrationPanel() {
		levelContext.addContext(providerContextHolder);
		levelContext.addValue(this);

		FxStateMeta.key(this, "split");

		tree.setShowRoot(false);
		tree.setCellFactory(tv -> new AdministrationTreeCell());
		tree.setMinWidth(120);

		getItems().addAll(tree, contentArea);
		SplitPane.setResizableWithParent(tree, false);
		setDividerPositions(0.25);

		buildTree();

		tree.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
			if (!suppressSelectionEvent)
				onTreeSelectionChanged(old, now);
		});
	}

	// --- Tree building ---

	private void buildTree() {
		TreeItem<AdministrationTreeItem> root = new TreeItem<>();
		FileObject adminFolder = FileSystem.getFile(BSAppJFX.CORE_CONFIG_FOLDER, ADMINISTRATION_FOLDER_NAME);
		if (adminFolder != null) {
			buildTreeItems(adminFolder, root, new AdministrationProviderFileLoader());
		}
		tree.setRoot(root);
	}

	private boolean buildTreeItems(FileObject folder, TreeItem<AdministrationTreeItem> parent,
			AdministrationProviderFileLoader loader) {
		boolean hasVisible = false;
		for (FileObject child : folder.getChildren()) {
			if (child.isDirectory()) {
				TreeItem<AdministrationTreeItem> groupItem = new TreeItem<>(
						new GroupItem(child.getLocalizedName(), child.getAttribute("icon", null)));
				groupItem.setExpanded(true);
				if (buildTreeItems(child, groupItem, loader)) {
					parent.getChildren().add(groupItem);
					hasVisible = true;
				}
			} else {
				IAdministrationProvider provider = loader.tryLoadFile(child);
				if (provider == null)
					continue;
				Class<? extends Permission> req = provider.getRequiredPermission();
				if (req != null && !Permissions.isAllowed(req))
					continue;
				parent.getChildren().add(new TreeItem<>(new ProviderItem(provider)));
				hasVisible = true;
			}
		}
		return hasVisible;
	}

	// --- Selection handling ---

	private void onTreeSelectionChanged(TreeItem<AdministrationTreeItem> old, TreeItem<AdministrationTreeItem> now) {
		IAdministrationProvider newProvider = (now != null && now.getValue() instanceof ProviderItem pi) ? pi.provider()
				: null;
		if (newProvider == activeProvider)
			return;

		if (!runSaveGuard(true)) {
			suppressSelectionEvent = true;
			tree.getSelectionModel().select(old);
			suppressSelectionEvent = false;
			return;
		}

		if (newProvider != null)
			activateProvider(newProvider);
		else
			deactivateProvider();
	}

	private void deactivateProvider() {
		detachSaveListener();
		activeProvider = null;
		contentArea.setTop(null);
		contentArea.setCenter(null);
		providerContextHolder.replaceContext(null);
		StageAutoSizer.autoSize();
	}

	private void activateProvider(IAdministrationProvider provider) {
		detachSaveListener();
		activeProvider = provider;

		contentArea.setTop(buildHeader(provider));
		contentArea.setCenter(provider.getAdministrationComponent());

		if (provider instanceof IContextProvider cp) {
			providerContextHolder.replaceContext(cp.getItemContext());
		} else {
			providerContextHolder.replaceContext(null);
		}

		attachSaveListener(provider);
		StageAutoSizer.autoSize();
	}

	private Node buildHeader(IAdministrationProvider provider) {
		String panelTitle = provider.getPanelTitle();
		Node largeIcon = provider.getLargeIcon();
		if ((panelTitle == null || panelTitle.isBlank()) && largeIcon == null)
			return null;
		HBox header = new HBox(8);
		header.getStyleClass().add("administration-panel-header");
		if (largeIcon != null)
			header.getChildren().add(largeIcon);
		if (panelTitle != null && !panelTitle.isBlank()) {
			Label title = new Label(panelTitle);
			title.getStyleClass().addAll("administration-panel-title", "ui-title");
			header.getChildren().add(title);
		}
		return header;
	}

	// --- Save listener ---

	private void attachSaveListener(IAdministrationProvider provider) {
		if (provider instanceof ISaveAll savable) {
			saveStateListener = (obs, o, n) -> {
				// hook for future save-state UI feedback (e.g. dirty indicator)
			};
			savable.getSaveAllEnabled().addListener(saveStateListener);
		}
	}

	private void detachSaveListener() {
		if (activeProvider == null || saveStateListener == null)
			return;
		if (activeProvider instanceof ISaveAll savable)
			savable.getSaveAllEnabled().removeListener(saveStateListener);
		saveStateListener = null;
	}

	// --- Save guard ---

	/**
	 * Checks for unsaved changes on the active provider and prompts the user.
	 * Returns true if it is safe to proceed (no changes, saved, or discarded). When
	 * {@code allowCancel} is false the dialog has no Cancel option and always
	 * returns true.
	 */
	private boolean runSaveGuard(boolean allowCancel) {
		if (activeProvider == null)
			return true;
		if (!(activeProvider instanceof ISaveAll savable))
			return true;
		if (!savable.getSaveAllEnabled().get())
			return true;

		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(BSAppAdministrationMessages.getString("AdministrationPanel.dialog.unsavedChanges.title"));
		alert.setContentText(
				BSAppAdministrationMessages.getString("AdministrationPanel.dialog.unsavedChanges.message"));
		alert.getButtonTypes()
				.setAll(allowCancel
						? new ButtonType[] { BSButtonTypes.SAVE, BSButtonTypes.DISCARD, BSButtonTypes.CANCEL }
						: new ButtonType[] { BSButtonTypes.SAVE, BSButtonTypes.DISCARD });

		Optional<ButtonType> result = alert.showAndWait();

		if (result.isPresent() && result.get() == BSButtonTypes.SAVE) {
			savable.saveAll();
			return true;
		}
		if (allowCancel && (result.isEmpty() || result.get() == BSButtonTypes.CANCEL))
			return false;
		return true;
	}

	// --- IClose ---

	@Override
	public void close() {
		if (runSaveGuard(true))
			BSAppUI.popUI();
	}

	@Override
	public BooleanProperty getCloseEnabled() {
		return closeEnabled;
	}

	// --- IStackedComponent ---

	@Override
	public void afterPush() {
		isOpen = true;
		stateManager.restoreState(this);
	}

	@Override
	public void beforePop() {
		stateManager.persistState(this);
		isOpen = false;
	}

	// --- IContextProvider ---

	@Override
	public Context getItemContext() {
		return levelContext;
	}

	// --- Tree cell ---

	private class AdministrationTreeCell extends TreeCell<AdministrationTreeItem> {
		@Override
		protected void updateItem(AdministrationTreeItem item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
			} else if (item instanceof ProviderItem pi) {
				setText(pi.provider().getTreeTitle());
				setGraphic(pi.provider().getSmallIcon());
			} else if (item instanceof GroupItem gi) {
				setText(gi.label());
				setGraphic(gi.iconSpec() != null ? ImageUtils.getIconNode(gi.iconSpec()) : null);
			}
		}
	}
}
