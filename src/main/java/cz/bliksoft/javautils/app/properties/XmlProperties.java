package cz.bliksoft.javautils.app.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.bliksoft.javautils.app.exceptions.ViewableException;

public class XmlProperties extends Properties {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger log = null;
	private File path;

	// public final Properties properties = new Properties();

	public XmlProperties(File path) {
		super();
		this.path = path;

		if (path.exists()) {
			try (FileInputStream fis = new FileInputStream(path)) {
				this./* properties. */loadFromXML(fis);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void save() throws ViewableException {
		String dir = this.path.getParent();
		File savedir = new File(dir);
		savedir.mkdirs();
		getLogger().trace("Saving properties file {}", this.path);
		try {
			File tmpfile = new File(this.path.getPath() + ".tmp");
			try (FileOutputStream fs = new FileOutputStream(tmpfile)) {
				this./* properties. */storeToXML(fs, null);
			}
			path.delete();
			tmpfile.renameTo(path);
		} catch (Exception e) {
			getLogger().log(Level.ERROR, "Chyba při ukládání XML properties", e);
			throw new ViewableException("Chyba při ukládání nastavení", e);
		}
	}

	public File getPath() {
		return this.path;
	}

	public boolean isWritable() {
		File cfgFile = getPath();
		if (cfgFile.exists() && cfgFile.canWrite())
			return true;
		return cfgFile.getParentFile().canWrite();
	}

	private static Logger getLogger() {
		if (log == null)
			log = LogManager.getLogger();
		return log;
	}

	public Double getDouble(String k) {
		String v = getProperty(k);
		if (v == null)
			return null;
		try {
			return Double.parseDouble(v);
		} catch (Exception e) {
			return null;
		}
	}

	public void putDouble(String k, double v) {
		setProperty(k, Double.toString(v));
	}

	public Boolean getBool(String k) {
		String v = getProperty(k);
		return v == null ? null : Boolean.parseBoolean(v);
	}

	public void putBool(String k, boolean v) {
		setProperty(k, Boolean.toString(v));
	}
}
