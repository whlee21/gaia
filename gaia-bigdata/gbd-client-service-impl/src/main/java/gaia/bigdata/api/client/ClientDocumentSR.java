package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.document.DocumentResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Map;

import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientDocumentSR extends BaseServiceLocatorSR implements DocumentResource {
	protected String id;
	protected String collection;

	@Inject
	public ClientDocumentSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
		id = getRequest().getAttributes().get("id").toString();
	}

	public Object retrieve(Map<String, Object> request) {
		JSONObject result = null;
		RestletContainer<DocumentResource> drRc = RestletUtil.wrap(DocumentResource.class,
				serviceLocator.getServiceURI(ServiceType.DOCUMENT.name()), "/" + collection + "/doc/" + id);
		DocumentResource dr = (DocumentResource) drRc.getWrapped();
		try {
			dr.retrieve(request);
			Representation rep = drRc.getClientResource().getResponseEntity();
			result = new JsonRepresentation(rep).getJsonObject();
		} catch (ResourceException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(drRc);
		}
		return result;
	}

	public State remove() {
		State returnValue = null;
		RestletContainer<DocumentResource> drRc = RestletUtil.wrap(DocumentResource.class,
				serviceLocator.getServiceURI(ServiceType.DOCUMENT.name()), "/" + collection + "/doc/" + id);
		DocumentResource dr = (DocumentResource) drRc.getWrapped();
		try {
			returnValue = dr.remove();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(drRc);
		}
		return returnValue;
	}

	public State update(JsonRepresentation document) {
		State returnValue = null;
		RestletContainer<DocumentResource> drRc = RestletUtil.wrap(DocumentResource.class,
				serviceLocator.getServiceURI(ServiceType.DOCUMENT.name()), "/" + collection + "/doc/" + id);
		DocumentResource dr = (DocumentResource) drRc.getWrapped();
		try {
			returnValue = dr.update(document);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(drRc);
		}
		return returnValue;
	}
}
