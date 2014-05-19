package gaia.bigdata.api.id;

import gaia.commons.api.APIModule;

public class IdAPIModule extends APIModule {
	protected void defineBindings() {
		bind(IdGeneratorService.class).to(UUID4IdGeneratorService.class);
	}
}
