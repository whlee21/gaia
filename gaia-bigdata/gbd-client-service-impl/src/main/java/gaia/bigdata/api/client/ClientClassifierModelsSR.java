package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.data.ClassifierModelsResource;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;
import gaia.bigdata.services.ServiceType;

public class ClientClassifierModelsSR extends ClientModelListSR implements ClassifierModelsResource {
	@Inject
	public ClientClassifierModelsSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	public State add(ClassifierModel toAdd) {
		State result = null;
		RestletContainer<ClassifierModelsResource> rc = RestletUtil.wrap(ClassifierModelsResource.class,
				getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/models");
		try {
			ClassifierModelsResource cmr = (ClassifierModelsResource) rc.getWrapped();
			result = cmr.add(toAdd);
		} finally {
			RestletUtil.release(rc);
		}
		return result;
	}
}
