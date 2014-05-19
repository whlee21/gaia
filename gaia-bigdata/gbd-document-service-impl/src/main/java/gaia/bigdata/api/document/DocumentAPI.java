package gaia.bigdata.api.document;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.bigdata.services.ServiceType;

@Singleton
public class DocumentAPI extends API {
	@Inject
	public DocumentAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("/{collection}", DocumentsServerResource.class);

		attach("/{collection}/retrieval", DocumentsRetrievalServerResource.class);
		attach("/{collection}/deletion", DocumentsDeletionSR.class);
		attach("/{collection}/doc/{id}", DocumentServerResource.class);
	}

	public String getAPIRoot() {
		return "/documents";
	}

	public String getAPIName() {
		return ServiceType.DOCUMENT.name();
	}
}
