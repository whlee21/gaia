package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.document.DocumentsResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientDocumentsSR extends BaseServiceLocatorSR implements DocumentsResource {
	protected String collection;

	@Inject
	public ClientDocumentsSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
	}

	@Post
	public State add(JsonRepresentation document) {
		State returnValue = null;
		RestletContainer<DocumentsResource> drRc = RestletUtil.wrap(DocumentsResource.class,
				serviceLocator.getServiceURI(ServiceType.DOCUMENT.name()), "/" + collection);
		DocumentsResource dr = (DocumentsResource) drRc.getWrapped();
		try {
			returnValue = dr.add(document);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(drRc);
		}
		return returnValue;
	}

	@Put
	public State update(JsonRepresentation document) {
		State returnValue = null;
		RestletContainer<DocumentsResource> drRc = RestletUtil.wrap(DocumentsResource.class,
				serviceLocator.getServiceURI(ServiceType.DOCUMENT.name()), "/" + collection);
		DocumentsResource dr = (DocumentsResource) drRc.getWrapped();
		try {
			returnValue = dr.update(document);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(drRc);
		}
		return returnValue;
	}

	@Delete
	public State remove() {
		State returnValue = null;
		RestletContainer<DocumentsResource> drRc = RestletUtil.wrap(DocumentsResource.class,
				serviceLocator.getServiceURI(ServiceType.DOCUMENT.name()), "/" + collection);
		DocumentsResource dr = (DocumentsResource) drRc.getWrapped();
		try {
			returnValue = dr.remove();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(drRc);
		}
		return returnValue;
	}
}
