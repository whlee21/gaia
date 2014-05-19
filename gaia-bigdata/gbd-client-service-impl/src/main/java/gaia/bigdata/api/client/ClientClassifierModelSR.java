package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.data.ClassifierModelResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientClassifierModelSR extends BaseServiceLocatorSR implements ClassifierModelResource {
	private String modelName;

	@Inject
	public ClientClassifierModelSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		modelName = getRequest().getAttributes().get("model").toString();
	}

	public ClassifierModel getModel() {
		RestletContainer<ClassifierModelResource> cmr = RestletUtil.wrap(ClassifierModelResource.class,
				getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/models/" + modelName);
		try {
			return ((ClassifierModelResource) cmr.getWrapped()).getModel();
		} finally {
			RestletUtil.release(cmr);
		}
	}

	public State updateModel(ClassifierModel model) {
		RestletContainer<ClassifierModelResource> cmr = RestletUtil.wrap(ClassifierModelResource.class,
				getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/models/" + modelName);
		try {
			return ((ClassifierModelResource) cmr.getWrapped()).updateModel(model);
		} finally {
			RestletUtil.release(cmr);
		}
	}

	public State deleteModel() {
		RestletContainer<ClassifierModelResource> cmr = RestletUtil.wrap(ClassifierModelResource.class,
				getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/models/" + modelName);
		try {
			return ((ClassifierModelResource) cmr.getWrapped()).deleteModel();
		} finally {
			RestletUtil.release(cmr);
		}
	}
}
