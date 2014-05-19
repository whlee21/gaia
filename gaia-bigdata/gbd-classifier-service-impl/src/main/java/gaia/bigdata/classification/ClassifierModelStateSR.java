package gaia.bigdata.classification;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.classification.ClassifierModelStateResource;
import gaia.bigdata.api.data.ClassifierModelResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.io.IOException;
import java.util.Map;

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ClassifierModelStateSR extends BaseServiceLocatorSR implements ClassifierModelStateResource {
	private static transient Logger log = LoggerFactory.getLogger(ClassifierModelStateSR.class);
	private ClassifierService service;
	private String modelName;

	@Inject
	public ClassifierModelStateSR(Configuration configuration, ServiceLocator serviceLocator, ClassifierService service) {
		super(configuration, serviceLocator);
		this.service = service;
	}

	protected void doInit() throws ResourceException {
		modelName = ((String) getRequest().getAttributes().get("model"));
	}

	public State load(Map<String, Object> params) throws UnsupportedOperationException, IOException {
		State result = new State(modelName, modelName);

		ClassifierModel model = getModel(modelName);
		if (model == null) {
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			result.setErrorMsg("The model " + modelName + " does not exist");
			setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST);
			return result;
		}
		if (!service.isSupported(model)) {
			result.setStatus(gaia.bigdata.api.Status.NOT_SUPPORTED);
			result.setErrorMsg("The classification service does not support this type of models");
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL);
			return result;
		}
		log.info("Loading model: {}", model);
		if (service.hasRoom(model) > 0) {
			try {
				service.loadModel(model);
				result.setStatus(gaia.bigdata.api.Status.SUCCEEDED);
				log.info("Loaded model: {}", model);
			} catch (IOException e) {
				log.error("Exception", e);
				setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL);
				result.setStatus(gaia.bigdata.api.Status.FAILED);
				result.setThrowable(e);
				result.setErrorMsg("The classification service was unable to load the model: " + model);
				return result;
			}
		} else {
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			result
					.setErrorMsg("The classification service does not have room for more models, please try a different server");
			return result;
		}
		return result;
	}

	private ClassifierModel getModel(String modelName) {
		RestletContainer<ClassifierModelResource> cmr = RestletUtil.wrap(ClassifierModelResource.class,
				serviceLocator.getServiceURI(ServiceType.DATA_MANAGEMENT.name()), "/models/" + modelName);

		return ((ClassifierModelResource) cmr.getWrapped()).getModel();
	}

	public State unload() throws UnsupportedOperationException {
		State result = new State(modelName, modelName);

		ClassifierModel model = service.getModel(modelName);
		if (model == null) {
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			result.setErrorMsg("The model " + modelName + " does not exist on this server");
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL);
			return result;
		}
		log.info("Unloading {}", model);
		try {
			service.unloadModel(model);
			result.setStatus(gaia.bigdata.api.Status.SUCCEEDED);
			log.info("Unloaded model: {}", model);
		} catch (IOException e) {
			log.error("Exception", e);
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL);
			result.setThrowable(e);
			result.setErrorMsg("The classification service was unable to unload the model: " + model);
			return result;
		}
		return result;
	}
}
