package gaia.bigdata.api.client;

import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.data.ClassifierModelResource;

import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;
import gaia.bigdata.services.ServiceType;

public class ModelHelper {
	public static ClassifierModel lookupModel(ServiceLocator locator, String modelName) {
		ClassifierModel result = null;
		RestletContainer<ClassifierModelResource> rc = RestletUtil.wrap(ClassifierModelResource.class,
				locator.getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/models/" + modelName);

		if ((rc != null) && (rc.getWrapped() != null)) {
			result = ((ClassifierModelResource) rc.getWrapped()).getModel();
		}
		return result;
	}
}
