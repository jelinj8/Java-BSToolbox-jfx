package cz.bliksoft.javautils.app.ui.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;
import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

/**
 * {@code core/actions} nodes naming their class ({@code class} attribute), each
 * configuring its own instance ({@link IConfigurableUIAction}).
 */
class ConfigurableUIActionTest {

	/** A configurable test action: key and text from its node. */
	public static class TestAction extends UIActionBase implements IConfigurableUIAction {
		private String key;
		String task;

		@Override
		public void configure(FileObject node) {
			key = node.getName();
			task = node.getAttribute("task");
			setText(node.getAttribute("title"));
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public void execute() {
		}
	}

	@Test
	void oneClassServesSeveralActions() throws Exception {
		String ns = "http://bliksoft.cz/XmlFilesystem";
		String xml = "<root xmlns=\"" + ns + "\"><file name=\"core\"><file name=\"actions\">\n" //
				+ "  <file name=\"TestConfiguredA\">\n" //
				+ "    <attribute name=\"class\" value=\"" + TestAction.class.getName() + "\"/>\n" //
				+ "    <attribute name=\"task\" value=\"taskA\"/><attribute name=\"title\" value=\"A\"/>\n" //
				+ "  </file>\n" //
				+ "  <file name=\"TestConfiguredB\">\n" //
				+ "    <attribute name=\"class\" value=\"" + TestAction.class.getName() + "\"/>\n" //
				+ "    <attribute name=\"task\" value=\"taskB\"/><attribute name=\"title\" value=\"B\"/>\n" //
				+ "  </file>\n" //
				+ "</file></file></root>";
		FileSystem.getDefault().importXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
				"configurableActions");

		IUIAction a = UIActions.getAction("TestConfiguredA");
		IUIAction b = UIActions.getAction("TestConfiguredB");
		assertTrue(a instanceof TestAction);
		assertTrue(b instanceof TestAction);
		assertEquals("taskA", ((TestAction) a).task);
		assertEquals("B", b.textProperty().get());
	}
}
