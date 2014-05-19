package gaia.commons.services;

import gaia.commons.api.Configuration;

public class BaseServiceLocatorSR extends BaseServerResource {
	protected ServiceLocator serviceLocator;

	public BaseServiceLocatorSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration);
		this.serviceLocator = serviceLocator;
	}

	protected URIPayload getServiceURI(String name) {
		return serviceLocator.getServiceURI(name);
	}
}
