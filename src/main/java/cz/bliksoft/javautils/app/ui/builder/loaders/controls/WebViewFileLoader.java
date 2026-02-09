package cz.bliksoft.javautils.app.ui.builder.loaders.controls;

import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebViewFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {
		WebView view = new WebView();
		WebEngine engine = view.getEngine();

		String url = file.getAttribute("url", null);
		String html = file.getAttribute("html", null);

		if (url != null && !url.isBlank()) {
			engine.load(url);
		} else if (html != null) {
			engine.loadContent(html);
		}

		Boolean contextMenuEnabled = file.getBool("contextMenuEnabled", null);
		if (contextMenuEnabled == null) {
			// alternativní název atributu (když už někde existuje)
			contextMenuEnabled = file.getBool("contextMenu", null);
		}
		if (contextMenuEnabled != null) {
			view.setContextMenuEnabled(contextMenuEnabled);
		}

		return view;
	}

	@Override
	public String getExtension() {
		return "WebView";
	}
}
