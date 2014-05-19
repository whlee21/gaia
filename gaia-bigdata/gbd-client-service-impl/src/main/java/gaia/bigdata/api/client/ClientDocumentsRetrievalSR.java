package gaia.bigdata.api.client;

import gaia.bigdata.ResultType;
import gaia.bigdata.api.document.DocumentsRetrievalResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Map;

import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientDocumentsRetrievalSR extends BaseServiceLocatorSR implements DocumentsRetrievalResource {
	protected String collection;

	@Inject
	public ClientDocumentsRetrievalSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
	}

	@Post
	public Map<ResultType, Object> retrieve(Map<String, Object> request) {
		Map<ResultType, Object> returnValue = null;
		RestletContainer<DocumentsRetrievalResource> drRc = RestletUtil.wrap(DocumentsRetrievalResource.class,
				serviceLocator.getServiceURI(ServiceType.DOCUMENT.name()), "/" + collection + "/retrieval");
		DocumentsRetrievalResource dr = (DocumentsRetrievalResource) drRc.getWrapped();
		try {
			returnValue = dr.retrieve(request);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(drRc);
		}
		return returnValue;
	}
}
