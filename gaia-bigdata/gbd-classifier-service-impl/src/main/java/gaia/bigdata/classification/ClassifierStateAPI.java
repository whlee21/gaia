package gaia.bigdata.classification;

import com.google.inject.Inject;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.bigdata.services.ServiceType;

public class ClassifierStateAPI extends API {
	@Inject
	public ClassifierStateAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("", ClassifierStateSR.class);
		attach("/{model}", ClassifierModelStateSR.class);
	}

	public String getAPIRoot() {
		return "/classifierstate";
	}

	public String getAPIName() {
		return ServiceType.CLASSIFIER_STATE.name();
	}
}
