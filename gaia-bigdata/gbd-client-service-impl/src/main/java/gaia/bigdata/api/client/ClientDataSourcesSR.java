package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.connector.ConnectorsResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.List;
import java.util.Map;

import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientDataSourcesSR extends BaseServiceLocatorSR implements ConnectorsResource {
	protected String collection;

	@Inject
	public ClientDataSourcesSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
	}

	public State create(Map<String, Object> entity) throws Exception {
		State returnValue = null;
		RestletContainer<ConnectorsResource> crRc = RestletUtil.wrap(ConnectorsResource.class,
				serviceLocator.getServiceURI(ServiceType.CONNECTOR.name()), "/" + collection);
		ConnectorsResource cr = (ConnectorsResource) crRc.getWrapped();
		try {
			returnValue = cr.create(entity);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(crRc);
		}
		return returnValue;
	}

	public List<State> listConnectors() throws Exception {
		List<State> returnValue = null;
		RestletContainer<ConnectorsResource> crRc = RestletUtil.wrap(ConnectorsResource.class,
				serviceLocator.getServiceURI(ServiceType.CONNECTOR.name()), "/" + collection);
		ConnectorsResource cr = (ConnectorsResource) crRc.getWrapped();
		try {
			returnValue = cr.listConnectors();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(crRc);
		}
		return returnValue;
	}
}
