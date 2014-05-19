package gaia.bigdata.api.client;

import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.data.ClassifierModelsResource;
import gaia.bigdata.api.data.ModelsListingResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Collection;

import org.restlet.data.Form;

import com.google.inject.Inject;

public class ClientModelListSR extends BaseServiceLocatorSR implements ModelsListingResource {
	@Inject
	public ClientModelListSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	public Collection<ClassifierModel> listModels() {
		Collection<ClassifierModel> result = null;
		Form form = getRequest().getResourceRef().getQueryAsForm();
		RestletContainer<ClassifierModelsResource> rc;
		if (form != null) {
			rc = RestletUtil.wrap(ClassifierModelsResource.class, getServiceURI(ServiceType.DATA_MANAGEMENT.name()),
					"/models", form);
		} else {
			rc = RestletUtil.wrap(ClassifierModelsResource.class, getServiceURI(ServiceType.DATA_MANAGEMENT.name()),
					"/models");
		}
		try {
			ClassifierModelsResource cmr = (ClassifierModelsResource) rc.getWrapped();
			result = cmr.listModels();
		} finally {
			RestletUtil.release(rc);
		}
		return result;
	}
}
