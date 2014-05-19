package gaia.commons.util;

import org.restlet.resource.ClientResource;

public class RestletContainer<T> {
	private ClientResource clientResource;
	private T wrapped;

	public RestletContainer(ClientResource clientResource, T wrapped) {
		this.clientResource = clientResource;
		this.wrapped = wrapped;
	}

	public ClientResource getClientResource() {
		return clientResource;
	}

	public T getWrapped() {
		return wrapped;
	}
}
