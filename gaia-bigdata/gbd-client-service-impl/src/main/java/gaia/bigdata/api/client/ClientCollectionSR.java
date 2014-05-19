package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.data.SDACollectionResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientCollectionSR extends BaseServiceLocatorSR implements SDACollectionResource {
	protected String collection;

	@Inject
	public ClientCollectionSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
	}

	@Get
	public State retrieve() {
		State returnValue = null;
		RestletContainer<SDACollectionResource> resourceRc = RestletUtil.wrap(SDACollectionResource.class,
				serviceLocator.getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/collections/" + collection);
		SDACollectionResource resource = (SDACollectionResource) resourceRc.getWrapped();
		try {
			returnValue = resource.retrieve();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}

	@Delete
	public State remove() {
		State returnValue = null;
		RestletContainer<SDACollectionResource> resourceRc = RestletUtil.wrap(SDACollectionResource.class,
				serviceLocator.getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/collections/" + collection);
		SDACollectionResource resource = (SDACollectionResource) resourceRc.getWrapped();
		try {
			returnValue = resource.remove();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}
}
