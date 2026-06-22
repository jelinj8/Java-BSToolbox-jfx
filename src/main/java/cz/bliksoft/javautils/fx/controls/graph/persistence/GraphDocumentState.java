package cz.bliksoft.javautils.fx.controls.graph.persistence;

import java.io.File;

import cz.bliksoft.dataflow.model.Graph;
import cz.bliksoft.javautils.fx.binding.ObjectStatus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GraphDocumentState {

	private final ObjectProperty<Graph> graph = new SimpleObjectProperty<>();
	private final ObjectProperty<File> file = new SimpleObjectProperty<>();
	private final ReadOnlyObjectWrapper<ObjectStatus> status = new ReadOnlyObjectWrapper<>(ObjectStatus.INITIAL);
	private final StringProperty title = new SimpleStringProperty("Untitled");

	public GraphDocumentState() {
		graph.addListener((obs, o, n) -> updateTitle());
		file.addListener((obs, o, n) -> updateTitle());
		status.addListener((obs, o, n) -> updateTitle());
	}

	public void newDocument() {
		Graph g = new Graph("Root");
		graph.set(g);
		file.set(null);
		status.set(ObjectStatus.NEW);
	}

	public void opened(Graph g, File f) {
		graph.set(g);
		file.set(f);
		status.set(ObjectStatus.SAVED);
	}

	public void saved(File f) {
		file.set(f);
		status.set(ObjectStatus.SAVED);
	}

	public void markModified() {
		ObjectStatus current = status.get();
		if (current == ObjectStatus.SAVED || current == ObjectStatus.NEW || current == ObjectStatus.INITIAL)
			status.set(ObjectStatus.MODIFIED);
	}

	public void refreshTitle() {
		updateTitle();
	}

	public boolean isModified() {
		return status.get() == ObjectStatus.MODIFIED;
	}

	private void updateTitle() {
		Graph g = graph.get();
		String name = g != null && g.getName() != null && !g.getName().isEmpty() ? g.getName() : "Untitled";
		File f = file.get();
		String path = f != null ? f.getName() : name;

		boolean mod = status.get() == ObjectStatus.MODIFIED || status.get() == ObjectStatus.NEW;
		title.set(mod ? "* " + path : path);
	}

	public ObjectProperty<Graph> graphProperty() {
		return graph;
	}

	public Graph getGraph() {
		return graph.get();
	}

	public ObjectProperty<File> fileProperty() {
		return file;
	}

	public File getFile() {
		return file.get();
	}

	public ReadOnlyObjectProperty<ObjectStatus> statusProperty() {
		return status.getReadOnlyProperty();
	}

	public ObjectStatus getStatus() {
		return status.get();
	}

	public StringProperty titleProperty() {
		return title;
	}

	public String getTitle() {
		return title.get();
	}
}
