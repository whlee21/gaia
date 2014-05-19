package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.connector.ConnectorResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Map;

import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientDataSourceSR extends BaseServiceLocatorSR implements ConnectorResource {
	protected String collection;
	protected String id;

	@Inject
	public ClientDataSourceSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
		id = getRequest().getAttributes().get("id").toString();
	}

	public State status() {
		State returnValue = null;
		RestletContainer<ConnectorResource> crRc = RestletUtil.wrap(ConnectorResource.class,
				serviceLocator.getServiceURI(ServiceType.CONNECTOR.name()), "/" + collection + "/" + id);
		ConnectorResource cr = (ConnectorResource) crRc.getWrapped();
		try {
			returnValue = cr.status();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(crRc);
		}
		return returnValue;
	}

	public State update(Map<String, Object> entity) {
		State result = null;
		RestletContainer<ConnectorResource> crRc = RestletUtil.wrap(ConnectorResource.class,
				serviceLocator.getServiceURI(ServiceType.CONNECTOR.name()), "/" + collection + "/" + id);
		ConnectorResource cr = (ConnectorResource) crRc.getWrapped();
		try {
			result = cr.update(entity);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(crRc);
		}
		return result;
	}

	public boolean remove() {
		boolean returnValue = false;
		RestletContainer<ConnectorResource> crRc = RestletUtil.wrap(ConnectorResource.class,
				serviceLocator.getServiceURI(ServiceType.CONNECTOR.name()), "/" + collection + "/" + id);
		ConnectorResource cr = (ConnectorResource) crRc.getWrapped();
		try {
			returnValue = cr.remove();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(crRc);
		}
		return returnValue;
	}

	public State execute(Map<String, Object> body) {
		State returnValue = null;
		RestletContainer<ConnectorResource> crRc = RestletUtil.wrap(ConnectorResource.class,
				serviceLocator.getServiceURI(ServiceType.CONNECTOR.name()), "/" + collection + "/" + id);
		ConnectorResource cr = (ConnectorResource) crRc.getWrapped();
		try {
			returnValue = cr.execute(body);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(crRc);
		}
		return returnValue;
	}
}
