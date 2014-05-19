package gaia.bigdata.classification;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierStateResource;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ClassifierStateSR extends BaseServerResource implements ClassifierStateResource {
	private static transient Logger log = LoggerFactory.getLogger(ClassifierStateSR.class);
	private ClassifierService service;

	@Inject
	public ClassifierStateSR(Configuration configuration, ClassifierService service) {
		super(configuration);
		this.service = service;
	}

	public State info() {
		State result = new State();
		result.addProperty("loadedModels", service.loadedModels());
		return result;
	}
}
