package gaia.bigdata.api.connector.gaiasearch;

import gaia.bigdata.api.connector.ConnectorAPI;
import gaia.bigdata.api.connector.ConnectorResource;
import gaia.bigdata.api.connector.ConnectorService;
import gaia.bigdata.api.connector.ConnectorsResource;
import gaia.commons.api.API;
import gaia.commons.api.APIModule;

public class ConnectorAPIModule extends APIModule {
	protected void defineBindings() {
		bind(ConnectorService.class).to(GaiaConnectorService.class);
		bind(ConnectorResource.class).to(ConnectorServerResource.class);
		bind(ConnectorsResource.class).to(ConnectorsServerResource.class);
	}

	protected Class<? extends API> getAPIClass() {
		return ConnectorAPI.class;
	}
}
