package gaia.commons.api.ping;

import gaia.commons.api.APIModule;

public class PingAPIModule extends APIModule {
	protected void defineBindings() {
		bind(PingResource.class).to(PingServerResource.class);
	}
}
