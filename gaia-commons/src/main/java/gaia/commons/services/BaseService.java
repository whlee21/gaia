package gaia.commons.services;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;

public abstract class BaseService {
	protected Configuration config;
	protected ServiceLocator locator;

	@Inject
	protected BaseService(Configuration config, ServiceLocator locator) {
		this.config = config;
		this.locator = locator;
	}

	public Configuration getConfig() {
		return config;
	}

	protected URIPayload getServiceURI(String name) {
		return locator.getServiceURI(name);
	}

	public String toString() {
		return "BaseService{type=" + getType() + "}";
	}

	public abstract String getType();
}
