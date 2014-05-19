package gaia.bigdata.classification;

import com.google.inject.Inject;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.bigdata.services.ServiceType;

public class ClassifierAPI extends API {
	public static final String CLASSIFIER = "/classifier";

	@Inject
	public ClassifierAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("/{model}", ClassifierSR.class);
	}

	public String getAPIRoot() {
		return "/classifier";
	}

	public String getAPIName() {
		return ServiceType.CLASSIFIER.name();
	}
}
