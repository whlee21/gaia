package gaia.bigdata.api.document;

import gaia.bigdata.ResultType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.restlet.resource.ResourceException;

public class BaseDocumentServerResource extends BaseServerResource {
	protected String collection;
	protected Set<DocumentService> docServices;
	protected Map<ResultType, DocumentService> dsMap;
	protected ExecutorService executorService;

	public BaseDocumentServerResource(Configuration configuration, Set<DocumentService> docServices,
			ExecutorService execService) {
		super(configuration);
		this.dsMap = new HashMap<ResultType, DocumentService>();
		this.docServices = docServices;
		this.executorService = execService;
		for (Iterator<DocumentService> iter = docServices.iterator(); iter.hasNext();) {
			DocumentService docService = (DocumentService) iter.next();
			EnumSet<ResultType> sRT = docService.getSupportedResultTypes();
			for (ResultType resultType : sRT) {
				DocumentService prev = (DocumentService) this.dsMap.put(resultType, docService);
				if (prev != null)
					throw new RuntimeException("Only 1 DocumentService per ResultType is allowed: " + resultType);
			}
		}
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
	}
}
