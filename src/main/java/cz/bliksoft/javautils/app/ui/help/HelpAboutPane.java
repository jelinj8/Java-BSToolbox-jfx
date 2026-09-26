package cz.bliksoft.javautils.app.ui.help;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.app.BSAppJFX;
import cz.bliksoft.javautils.modules.IModule;
import cz.bliksoft.javautils.modules.Modules;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextFlow;

/**
 * The About dialog's content, built from plain controls (no WebView, so
 * applications don't need {@code javafx-web} for it).
 *
 * <p>
 * <b>About</b> tab: the application name ({@link BSApp#getAppName()}), the
 * optional localized {@code description}/{@code copyright}/{@code url}
 * attributes of {@code core/ui/about}, and the loaded modules with their
 * versions.
 *
 * <p>
 * <b>Credits</b> tab: every {@code lib_credits/*} entry, which modules merge in
 * for the libraries and resources they bring, in XmlFilesystem order - the ids
 * are lowercase names, so a library credited by several modules is one entry,
 * and {@code BaseAppModule.xml} declares {@code lib_credits} with
 * {@code sorted="true"}, so the list is alphabetical (with common-java-utils
 * 0.10 and older every module has to declare it, see {@code doc/Help.md}). An entry has localized {@code name},
 * {@code url}, {@code comment} and {@code licence}: one or more
 * comma-separated ids of {@code licences/*} entries (localized {@code name},
 * {@code url}), e.g. {@code APACHE2,LGPL3} for a dual-licensed library.
 */
public class HelpAboutPane extends TabPane {

	private static final Logger log = LogManager.getLogger(HelpAboutPane.class);

	private static final double GAP = 8;

	public HelpAboutPane() {
		getTabs().addAll(new Tab(BSAppHelpMessages.getString("HelpAboutPane.tab.about"), scroll(buildAbout())), //$NON-NLS-1$
				new Tab(BSAppHelpMessages.getString("HelpAboutPane.tab.credits"), scroll(buildCredits()))); //$NON-NLS-1$
		setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
	}

	private Node buildAbout() {
		VBox box = new VBox(GAP);
		Label title = new Label(BSApp.getAppName());
		title.setFont(Font.font(null, FontWeight.BOLD, Font.getDefault().getSize() * 1.8));
		box.getChildren().add(title);

		FileObject about = FileSystem.getFile(BSApp.CORE_CONFIG_FOLDER, "ui", "about"); //$NON-NLS-1$ //$NON-NLS-2$
		if (about != null) {
			addWrapped(box, about.getLocalizedAttribute("description", null)); //$NON-NLS-1$
			addWrapped(box, about.getLocalizedAttribute("copyright", null)); //$NON-NLS-1$
			String url = about.getLocalizedAttribute("url", null); //$NON-NLS-1$
			if (hasText(url))
				box.getChildren().add(link(url, url));
		}

		Label modulesTitle = new Label(BSAppHelpMessages.getString("HelpAboutPane.modules")); //$NON-NLS-1$
		modulesTitle.setStyle("-fx-font-weight: bold;"); //$NON-NLS-1$
		GridPane modules = new GridPane();
		modules.setHgap(2 * GAP);
		modules.setVgap(2);
		List<IModule> list = new ArrayList<>(Modules.getModules().values());
		list.sort(Comparator.comparing(IModule::getModuleName, String.CASE_INSENSITIVE_ORDER));
		int row = 0;
		for (IModule m : list)
			modules.addRow(row++, new Label(m.getModuleName()), new Label(m.getVersionInfo()));
		VBox.setMargin(modulesTitle, new Insets(GAP, 0, 0, 0));
		box.getChildren().addAll(modulesTitle, modules);
		return box;
	}

	private Node buildCredits() {
		VBox box = new VBox(GAP * 1.5);
		addWrapped(box, BSAppHelpMessages.getString("HelpAboutPane.credits.intro")); //$NON-NLS-1$
		FileObject credits = FileSystem.getFile("lib_credits"); //$NON-NLS-1$
		if (credits == null)
			return box;
		FileObject licences = FileSystem.getFile("licences"); //$NON-NLS-1$
		// deduplicated and ordered by the XmlFilesystem: a library credited by several
		// modules under the same (lowercase) id is one merged entry, sorted="true"
		for (FileObject c : credits.getChildren())
			box.getChildren().add(credit(c, licences));
		return box;
	}

	private Node credit(FileObject c, FileObject licences) {
		String name = c.getLocalizedAttribute("name", c.getName()); //$NON-NLS-1$
		String url = c.getLocalizedAttribute("url", null); //$NON-NLS-1$
		Node title = hasText(url) ? link(name, url) : new Label(name);
		title.setStyle("-fx-font-weight: bold;"); //$NON-NLS-1$
		VBox entry = new VBox(1, title);

		addWrapped(entry, c.getLocalizedAttribute("comment", null)); //$NON-NLS-1$

		String licence = c.getAttribute("licence", null); //$NON-NLS-1$
		if (hasText(licence)) {
			// Labels, not Text: a Text keeps its black fill regardless of the theme
			TextFlow flow = new TextFlow(new Label(BSAppHelpMessages.getString("HelpAboutPane.licence") + " ")); //$NON-NLS-1$ //$NON-NLS-2$
			String[] ids = licence.split(","); //$NON-NLS-1$
			for (int i = 0; i < ids.length; i++) {
				if (i > 0)
					flow.getChildren()
							.add(new Label(" " + BSAppHelpMessages.getString("HelpAboutPane.licence.or") + " ")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				flow.getChildren().add(licence(ids[i].strip(), licences));
			}
			entry.getChildren().add(flow);
		}
		return entry;
	}

	private Node licence(String id, FileObject licences) {
		FileObject l = licences != null ? licences.getFile(id) : null;
		if (l == null) {
			log.warn("lib_credits refers to an undefined licence '{}'", id); //$NON-NLS-1$
			return new Label(id);
		}
		String name = l.getLocalizedAttribute("name", id); //$NON-NLS-1$
		String url = l.getLocalizedAttribute("url", null); //$NON-NLS-1$
		return hasText(url) ? link(name, url) : new Label(name);
	}

	private static Hyperlink link(String text, String url) {
		Hyperlink l = new Hyperlink(text);
		l.setPadding(Insets.EMPTY);
		l.setOnAction(e -> {
			try {
				BSAppJFX.getApplication().getHostServices().showDocument(url);
			} catch (Exception ex) {
				log.error("Failed to open URL in browser: {}", url, ex); //$NON-NLS-1$
			}
		});
		return l;
	}

	private static void addWrapped(VBox box, String text) {
		if (!hasText(text))
			return;
		Label l = new Label(text);
		l.setWrapText(true);
		box.getChildren().add(l);
	}

	private static ScrollPane scroll(Node content) {
		ScrollPane sp = new ScrollPane(content);
		sp.setFitToWidth(true);
		sp.setPadding(new Insets(12));
		return sp;
	}

	private static boolean hasText(String s) {
		return s != null && !s.isBlank();
	}
}
