package gaia.bigdata.api.client;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;
import gaia.bigdata.services.ServiceType;

import gaia.bigdata.api.State;
import gaia.bigdata.api.data.SDACollectionsResource;

import java.util.List;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.Post;

public class ClientCollectionsSR extends BaseServiceLocatorSR implements SDACollectionsResource {
	@Inject
	public ClientCollectionsSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	@Get
	public List<State> listCollections() {
		List<State> returnValue = null;
		RestletContainer<SDACollectionsResource> resourceRc = RestletUtil.wrap(SDACollectionsResource.class,
				serviceLocator.getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/collections");
		SDACollectionsResource resource = (SDACollectionsResource) resourceRc.getWrapped();
		try {
			returnValue = resource.listCollections();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}

	@Post
	public State add(Map<String, Object> body) {
		State returnValue = null;
		RestletContainer<SDACollectionsResource> resourceRc = RestletUtil.wrap(SDACollectionsResource.class,
				serviceLocator.getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/collections");
		SDACollectionsResource resource = (SDACollectionsResource) resourceRc.getWrapped();
		try {
			returnValue = resource.add(body);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}
}
