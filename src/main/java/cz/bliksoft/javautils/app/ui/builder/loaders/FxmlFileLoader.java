package cz.bliksoft.javautils.app.ui.builder.loaders;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.ui.builder.ISlotPublisher;
import cz.bliksoft.javautils.app.ui.builder.UIBuildContext;
import cz.bliksoft.javautils.app.ui.builder.UIBuildContextHolder;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class FxmlFileLoader extends FileLoader {

	private static Logger log = LogManager.getLogger();

	@Override
	public Object loadObject(FileObject file) {
		String path = file.getAttribute("path", null);
		if (path == null || path.isBlank())
			throw new IllegalArgumentException("FXML file missing 'path' attribute: " + file.getName());

		URL url;
		if (path.startsWith("classpath:")) {
			String res = path.substring("classpath:".length());
			if (!res.startsWith("/"))
				res = "/" + res;
			url = getClass().getResource(res);
			if (url == null)
				throw new IllegalArgumentException("FXML classpath resource not found: " + res);
		} else {
			// treat as filesystem path
			try {
				url = java.nio.file.Paths.get(path).toUri().toURL();
			} catch (MalformedURLException e) {
				log.error("Bad FXML URL: " + path, e);
				return null;
			}
		}

		FXMLLoader loader = new FXMLLoader(url);

		// Optional: controller override by attribute (otherwise fx:controller from FXML
		// is used)
		String controllerClass = file.getAttribute("controller", null);
		if (controllerClass != null && !controllerClass.isBlank()) {
			Object ctrl;
			try {
				ctrl = Class.forName(controllerClass.trim()).getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				log.error("Failed to instantiate FXML controllerClass " + controllerClass, e);
				return null;
			}
			loader.setController(ctrl);
		}

		Parent root;
		try {
			root = loader.load();
		} catch (IOException e) {
			log.error("Failed to load FXML hierarchy from " + path, e);
			return null;
		}

		Object controller = loader.getController();
		if (controller instanceof ISlotPublisher sp) {
			UIBuildContext ctx = UIBuildContextHolder.get();
			if (ctx != null) {
				sp.publishSlots(ctx.slotResolver, root);
			}
		}

		return root;
	}

	@Override
	public String getExtension() {
		return "FXML";
	}
}
