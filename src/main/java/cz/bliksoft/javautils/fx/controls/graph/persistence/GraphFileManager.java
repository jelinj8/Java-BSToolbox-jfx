package cz.bliksoft.javautils.fx.controls.graph.persistence;

import java.io.File;
import java.util.Properties;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.dataflow.xml.GraphMigrationRegistry;
import cz.bliksoft.dataflow.xml.GraphSerializer;
import cz.bliksoft.dataflow.xml.GraphValidator;
import cz.bliksoft.javautils.app.BSApp;
import cz.bliksoft.javautils.fx.controls.graph.GraphCanvas;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class GraphFileManager {

	private static final String EXTENSION = ".bsgraph";
	private static final FileChooser.ExtensionFilter FILTER = new FileChooser.ExtensionFilter("Graph Files",
			"*" + EXTENSION);

	private static final String PROP_LAST_DIR = "graph.editor.lastDirectory";

	private final GraphCanvas canvas;
	private final GraphDocumentState documentState;

	public GraphFileManager(GraphCanvas canvas, GraphDocumentState documentState) {
		this.canvas = canvas;
		this.documentState = documentState;
	}

	public void newDocument() {
		documentState.newDocument();
		canvas.setGraph(documentState.getGraph());
		canvas.getCommandHistory().clear();
	}

	public boolean save(Window owner) {
		File file = documentState.getFile();
		if (file == null)
			return saveAs(owner);
		return saveToFile(file);
	}

	public boolean saveAs(Window owner) {
		FileChooser chooser = createChooser("Save Graph");
		File current = documentState.getFile();
		if (current != null) {
			chooser.setInitialDirectory(current.getParentFile());
			chooser.setInitialFileName(current.getName());
		}
		File file = chooser.showSaveDialog(owner);
		if (file == null)
			return false;
		if (!file.getName().endsWith(EXTENSION))
			file = new File(file.getPath() + EXTENSION);
		rememberDirectory(file);
		return saveToFile(file);
	}

	public boolean open(Window owner) {
		FileChooser chooser = createChooser("Open Graph");
		File current = documentState.getFile();
		if (current != null)
			chooser.setInitialDirectory(current.getParentFile());
		File file = chooser.showOpenDialog(owner);
		if (file == null)
			return false;
		rememberDirectory(file);
		return loadFromFile(file);
	}

	public boolean loadFromFile(File file) {
		try {
			Graph graph = GraphSerializer.unmarshal(file);
			GraphMigrationRegistry.getInstance().migrateIfNeeded(graph);

			java.util.List<String> repairs = GraphValidator.repair(graph);
			if (!repairs.isEmpty()) {
				String msg = "Graph auto-repaired:\n" + String.join("\n", repairs);
				org.apache.logging.log4j.LogManager.getLogger(GraphFileManager.class).info(msg);
			}

			java.util.List<String> errors = GraphValidator.validate(graph);
			if (!errors.isEmpty()) {
				String msg = "Graph validation warnings:\n" + String.join("\n", errors);
				org.apache.logging.log4j.LogManager.getLogger(GraphFileManager.class).warn(msg);
			}

			documentState.opened(graph, file);
			canvas.setGraph(graph);
			canvas.getCommandHistory().clear();
			rememberDirectory(file);
			return true;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load graph from " + file, e);
		}
	}

	private boolean saveToFile(File file) {
		try {
			Graph graph = documentState.getGraph();
			if (graph == null)
				return false;
			graph.setVersion(GraphMigrationRegistry.CURRENT_VERSION);
			GraphSerializer.marshal(graph, file);
			documentState.saved(file);
			return true;
		} catch (Exception e) {
			throw new RuntimeException("Failed to save graph to " + file, e);
		}
	}

	private FileChooser createChooser(String title) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(title);
		chooser.getExtensionFilters().add(FILTER);
		chooser.setSelectedExtensionFilter(FILTER);
		File lastDir = getLastDirectory();
		if (lastDir != null && lastDir.isDirectory())
			chooser.setInitialDirectory(lastDir);
		return chooser;
	}

	private void rememberDirectory(File file) {
		if (file == null)
			return;
		File dir = file.isDirectory() ? file : file.getParentFile();
		if (dir == null || !dir.isDirectory())
			return;
		Properties props = getLocalProps();
		if (props != null)
			props.put(PROP_LAST_DIR, dir.getAbsolutePath());
	}

	private File getLastDirectory() {
		Properties props = getLocalProps();
		if (props == null)
			return null;
		String path = props.getProperty(PROP_LAST_DIR);
		if (path == null || path.isBlank())
			return null;
		File dir = new File(path);
		return dir.isDirectory() ? dir : null;
	}

	private static Properties getLocalProps() {
		try {
			return BSApp.getLocalProperties();
		} catch (Exception e) {
			return null;
		}
	}

	public GraphDocumentState getDocumentState() {
		return documentState;
	}
}
