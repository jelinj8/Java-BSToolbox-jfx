package cz.bliksoft.javautils.app.ui.help;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.freemarker.FreemarkerGenerator;
import cz.bliksoft.javautils.modules.IModule;
import cz.bliksoft.javautils.modules.Modules;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;
import freemarker.cache.ClassTemplateLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebView;

public class HelpAboutPane extends TabPane {

	private static final Logger log = LogManager.getLogger(HelpAboutPane.class);

	public HelpAboutPane() {
		this((Object) null);
	}

	/**
	 * @param messages bundle exposed to templates as {@code msg} — templates call
	 *                 {@code ${msg.getString("key")}}
	 */
	public HelpAboutPane(ResourceBundle messages) {
		this((Object) messages);
	}

	/**
	 * @param messages map exposed to templates as {@code msg} — templates use
	 *                 {@code ${msg["key"]}}
	 */
	public HelpAboutPane(Map<String, String> messages) {
		this((Object) messages);
	}

	private HelpAboutPane(Object messages) {
		FreemarkerGenerator gen = new FreemarkerGenerator(
				new ClassTemplateLoader(HelpAboutPane.class, "/cz/bliksoft/javautils/app/templates/help")); //$NON-NLS-1$
		if (messages != null)
			gen.setVariable("msg", messages); //$NON-NLS-1$

		getTabs().addAll(buildAboutTab(gen), buildCreditsTab(gen));
		setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
	}

	private Tab buildAboutTab(FreemarkerGenerator gen) {
		WebView view = new WebView();
		try {
			List<IModule> modules = new ArrayList<>(Modules.getModules().values());
			modules.sort(Comparator.comparing(IModule::getModuleName, String.CASE_INSENSITIVE_ORDER));
			view.getEngine().loadContent(gen.generate("About.ftl", modules)); //$NON-NLS-1$
		} catch (Exception e) {
			log.error("Failed to render About.ftl", e);
		}
		return new Tab(BSAppHelpMessages.getString("HelpAboutPane.tab.about"), view); //$NON-NLS-1$
	}

	private Tab buildCreditsTab(FreemarkerGenerator gen) {
		WebView view = new WebView();
		try {
			Map<String, Object> data = new HashMap<>();
			data.put("credits", FileSystem.getFile("lib_credits")); //$NON-NLS-1$ //$NON-NLS-2$
			data.put("licences", FileSystem.getFile("licences")); //$NON-NLS-1$ //$NON-NLS-2$
			view.getEngine().loadContent(gen.generate("Credits.ftl", data)); //$NON-NLS-1$
		} catch (Exception e) {
			log.error("Failed to render Credits.ftl", e);
		}
		return new Tab(BSAppHelpMessages.getString("HelpAboutPane.tab.credits"), view); //$NON-NLS-1$
	}
}
