package cz.bliksoft.javautils.app.ui.builder.loaders.custom;

import cz.bliksoft.javautils.exceptions.InitializationException;
import cz.bliksoft.javautils.xmlfilesystem.FileLoader;
import cz.bliksoft.javautils.xmlfilesystem.FileObject;

public class GenericUIFileLoader extends FileLoader {

	@Override
	public Object loadObject(FileObject file) {

		String controlClass = file.getAttribute("class");
		Class<?> c;
		try {
			c = Class.forName(controlClass);
			Object result = c.getDeclaredConstructor().newInstance();
			return result;
		} catch (Exception e) {
			throw new InitializationException("Failed to load UIClass " + controlClass, e);
		}

	}

	@Override
	public String getExtension() {
		return "UIClass";
	}
}
