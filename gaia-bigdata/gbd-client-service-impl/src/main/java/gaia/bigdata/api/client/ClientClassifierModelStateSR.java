package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModelStateResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ClientClassifierModelStateSR extends BaseServiceLocatorSR implements ClassifierModelStateResource {
	private static transient Logger log = LoggerFactory.getLogger(ClientClassifierModelStateSR.class);
	private String modelName;

	@Inject
	public ClientClassifierModelStateSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		modelName = ((String) getRequest().getAttributes().get("model"));
	}

	public State load(Map<String, Object> params) {
		List<URIPayload> serviceURIs = new ArrayList<URIPayload>(
				serviceLocator.getServiceURIs(ServiceType.CLASSIFIER_STATE.name()));
		State result = new State(modelName, modelName);
		if ((serviceURIs == null) || (serviceURIs.isEmpty())) {
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			result.setErrorMsg("Unable to load model into any available ClassifierServices: " + modelName);
			return result;
		}
		Collections.shuffle(serviceURIs);

		int succeeded = 0;
		int repFactor = Math.min(3, serviceURIs.size());
		if (params != null) {
			Object rep = params.get("replicationFactor");
			if (rep != null) {
				repFactor = ((Integer) rep).intValue();
			}
		}

		for (URIPayload serviceURI : serviceURIs) {
			log.info("Attempting to load model to: {}", serviceURI);
			RestletContainer<ClassifierModelStateResource> csr = RestletUtil.wrap(ClassifierModelStateResource.class,
					serviceURI, "/" + modelName, Collections.singletonMap("replicationFactor", String.valueOf(repFactor)));
			State tmp = null;
			try {
				tmp = ((ClassifierModelStateResource) csr.getWrapped()).load(params);
				if ((tmp != null) && (gaia.bigdata.api.Status.SUCCEEDED.equals(tmp.getStatus()))) {
					log.info("Loaded model: " + modelName + " at " + serviceURI);
					result.addChild(tmp);
					succeeded++;
				}
			} catch (IOException e) {
				tmp = new State(modelName, modelName);
				tmp.setStatus(gaia.bigdata.api.Status.FAILED);
				tmp.setErrorMsg("Unable to load model into: " + serviceURI + modelName);
				tmp.setThrowable(e);
				result.addChild(tmp);
				log.warn("Couldn't load model for " + serviceURI, e);
			}

			if (succeeded >= repFactor) {
				break;
			}
		}
		if (succeeded == 0) {
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			result.setErrorMsg("Unable to load model into any available ClassifierServices: " + modelName);
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL);
		} else if (succeeded < repFactor) {
			result.setStatus(gaia.bigdata.api.Status.INCOMPLETE);
			result.setErrorMsg("Model was loaded " + succeeded + " times.  There are " + serviceURIs.size()
					+ " classification service instances available.");
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL);
		} else {
			result.setStatus(gaia.bigdata.api.Status.SUCCEEDED);
		}

		return result;
	}

	public State unload() {
		Collection<URIPayload> serviceURIs = serviceLocator.getServiceURIs("classifier." + modelName);
		State result = new State(modelName, modelName);
		List<URIPayload> classiferStateURIs;
		if (serviceURIs != null) {
			classiferStateURIs = new ArrayList<URIPayload>(serviceLocator.getServiceURIs(ServiceType.CLASSIFIER_STATE
					.name()));
			for (URIPayload serviceURI : serviceURIs) {
				URIPayload altered = null;

				for (URIPayload classiferStateURI : classiferStateURIs) {
					if ((serviceURI.uri.getHost().equals(classiferStateURI.uri.getHost()))
							&& (serviceURI.uri.getPort() == classiferStateURI.uri.getPort())) {
						altered = classiferStateURI;
						break;
					}
				}
				if (altered != null) {
					log.info("Unloading model from service URI: {}", altered);
					RestletContainer<ClassifierModelStateResource> csr = RestletUtil.wrap(ClassifierModelStateResource.class,
							altered, "/" + modelName);
					State tmp = ((ClassifierModelStateResource) csr.getWrapped()).unload();
					if (!tmp.getStatus().equals(gaia.bigdata.api.Status.SUCCEEDED)) {
						result.setStatus(gaia.bigdata.api.Status.FAILED);
					}
					result.addChild(tmp);
					classiferStateURIs.remove(altered);
				}
			}
		} else {
			result.setStatus(gaia.bigdata.api.Status.NON_EXISTENT);
			result.setErrorMsg("The model is not loaded anywhere");
		}
		Collection<State> children = result.getChildren();
		int size = children.size();
		int count = 0;
		for (State child : children) {
			if (child.getStatus().equals(gaia.bigdata.api.Status.SUCCEEDED)) {
				count++;
			}
		}
		if (count == size) {
			result.setStatus(gaia.bigdata.api.Status.SUCCEEDED);
		}
		return result;
	}
}
