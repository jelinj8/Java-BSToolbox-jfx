package cz.bliksoft.javautils.app;

import cz.bliksoft.javautils.VersionInfo;
import cz.bliksoft.javautils.modules.ModuleBase;

public class BaseAppModule extends ModuleBase {

	@Override
	public String getModuleName() {
		return "Base app module";
	}

	@Override
	public int getModuleLoadingOrder() {
		return -10000;
	}

	@Override
	public String getVersionInfo() {
		return new VersionInfo().getDisplayVersion();
	}
}
