package gaia.commons.services;

import gaia.commons.api.Configuration;
import org.restlet.resource.ServerResource;

public abstract class BaseServerResource extends ServerResource {
	protected Configuration configuration;

	public BaseServerResource(Configuration configuration) {
		this.configuration = configuration;
	}

	public Configuration getConfiguration() {
		return configuration;
	}
}
