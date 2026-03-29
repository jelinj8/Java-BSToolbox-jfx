package cz.bliksoft.javautils.app;

import cz.bliksoft.javautils.fx.VersionInfo;
import cz.bliksoft.javautils.fx.tools.Styling;
import cz.bliksoft.javautils.modules.ModuleBase;
import cz.bliksoft.javautils.xmlfilesystem.singletons.Services;
import cz.bliksoft.javautils.xmlfilesystem.singletons.Singletons;

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

	@Override
	public void init() {
		super.init();
		Styling.safeRegister("/css/app.css");
		Services.loadServices();
		Singletons.loadSingletons();
	}

}
