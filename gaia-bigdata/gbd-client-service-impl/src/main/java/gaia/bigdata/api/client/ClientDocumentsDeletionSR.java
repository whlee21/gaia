package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.document.DocumentsDeletionResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import org.json.JSONException;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientDocumentsDeletionSR extends BaseServiceLocatorSR implements DocumentsDeletionResource {
	private String collection;

	@Inject
	public ClientDocumentsDeletionSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
	}

	public State delete(JsonRepresentation document) throws JSONException {
		State returnValue = null;
		RestletContainer<DocumentsDeletionResource> drRc = RestletUtil.wrap(DocumentsDeletionResource.class,
				serviceLocator.getServiceURI(ServiceType.DOCUMENT.name()), "/" + collection + "/deletion");
		DocumentsDeletionResource drDeletionResource = (DocumentsDeletionResource) drRc.getWrapped();
		try {
			returnValue = drDeletionResource.delete(document);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(drRc);
		}
		return returnValue;
	}
}
