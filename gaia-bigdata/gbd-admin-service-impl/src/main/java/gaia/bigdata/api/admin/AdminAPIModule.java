package gaia.bigdata.api.admin;

import gaia.bigdata.management.SystemInfoService;
import gaia.bigdata.management.sda.SDASystemInfoService;
import gaia.commons.api.APIModule;

public class AdminAPIModule extends APIModule {
	protected void defineBindings() {
		bind(SystemInfoService.class).to(SDASystemInfoService.class);
	}
}
