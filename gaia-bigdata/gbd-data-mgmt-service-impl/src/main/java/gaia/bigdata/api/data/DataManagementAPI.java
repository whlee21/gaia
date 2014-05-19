package gaia.bigdata.api.data;

import gaia.bigdata.services.ServiceType;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DataManagementAPI extends API {
	@Inject
	public DataManagementAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("/collections", SDACollectionsServerResource.class);
		attach("/collections/{id}", SDACollectionServerResource.class);
		attach("/models", ClassifierModelsSR.class);
		attach("/models/{modelName}", ClassifierModelSR.class);
	}

	public String getAPIRoot() {
		return "/data";
	}

	public String getAPIName() {
		return ServiceType.DATA_MANAGEMENT.name();
	}
}
