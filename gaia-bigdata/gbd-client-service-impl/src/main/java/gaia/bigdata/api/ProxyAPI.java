package gaia.bigdata.api;

import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.commons.services.ServiceLocator;
import org.restlet.security.Enroler;
import org.restlet.security.Verifier;

public abstract class ProxyAPI extends API {
	protected ServiceLocator serviceLocator;
	protected Verifier verifier;
	protected Enroler enroler;

	public ProxyAPI(ResourceFinder finder, Enroler enroler, Verifier verifier, ServiceLocator serviceLocator) {
		super(finder);
		this.enroler = enroler;
		this.verifier = verifier;
		this.serviceLocator = serviceLocator;
	}
}
