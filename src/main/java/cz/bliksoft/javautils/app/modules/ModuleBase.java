package cz.bliksoft.javautils.app.modules;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Properties;

import cz.bliksoft.javautils.StringUtils;

/**
 *
 */
public abstract class ModuleBase implements IModule {

	private boolean moduleEnabled = true;

	@Override
	public InputStream getRootXml() {
		String path = getClass().getPackage().getName().replace('.', '/');
		return ClassLoader.getSystemResourceAsStream(path + "/root.xml"); //$NON-NLS-1$
	}

	@Override
	public void init() {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void cleanup() {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void install() {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public HashMap<String, String> getTranslations() {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return moduleEnabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		moduleEnabled = enabled;
	}

	@Override
	public String getVersionInfo() {
		Class<?> clazz = this.getClass();
		String className = clazz.getSimpleName() + ".class";
		try {
			URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI().resolve("git.properties");
			try (InputStream is = uri.toURL().openStream()) {
				Properties props = new Properties();
				props.load(is);
				String version = props.getProperty("git.build.version");
				String closestTagCommitCount = props.getProperty("git.closest.tag.commit.count");
				String closestTag = props.getProperty("git.closest.tag.name");
				String commitId = props.getProperty("git.commit.id.abbrev");
				String branch = props.getProperty("git.branch");
				String tags = props.getProperty("git.tags");
				StringBuilder sb = new StringBuilder();
				sb.append(version);
				if (StringUtils.hasNonWhitespaceText(closestTag)) {
					sb.append(" (");
					sb.append(closestTag);
					sb.append("+");
					sb.append(closestTagCommitCount);
					sb.append(")");
				}
				sb.append(" [");
				sb.append(branch);
				sb.append(":");
				sb.append(commitId);
				sb.append("]");
				if (StringUtils.hasNonWhitespaceText(tags)) {
					sb.append(" ");
					sb.append(tags);
				}
				return sb.toString();
			} catch (Exception e) {
				return "Failed to read version info.";
			}

		} catch (URISyntaxException e) {
			e.printStackTrace();
			String path = clazz.getResource(className).toString();
			return path + ":" + className;
		}

//		// Properties p = PropertiesUtils.
//
//		String className = clazz.getSimpleName() + ".class"; //$NON-NLS-1$
//		String classPath = clazz.getResource(className).toString();
//		String revision = BSFWMessages.getString("ModuleBase.Unpackaged"); //$NON-NLS-1$
//		String tag = ""; //$NON-NLS-1$
//		if (classPath.startsWith("jar")) //$NON-NLS-1$
//		{
//			try {
//				String manifestPath = classPath.substring(0, classPath.lastIndexOf('!') + 1) + "/META-INF/MANIFEST.MF"; //$NON-NLS-1$
//				Manifest manifest;
//				manifest = new Manifest(new URL(manifestPath).openStream());
//				Attributes attr = manifest.getMainAttributes();
//				revision = "(" + attr.getValue("GIT-REVISION") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//				tag = attr.getValue("GIT-TAGSTRING"); //$NON-NLS-1$
//			} catch (MalformedURLException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		String result = (tag == null ? "?" : tag);
//		return (result.length() > 0 ? result + " " + revision : revision); //$NON-NLS-1$
	}

	@Override
	public int getModuleLoadingOrder() {
		return 0;
	}
}
