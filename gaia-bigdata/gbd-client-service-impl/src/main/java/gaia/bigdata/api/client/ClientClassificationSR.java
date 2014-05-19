package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassificationResource;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.classification.ClassifierModelStateResource;
import gaia.bigdata.api.classification.ClassifierResult;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ClientClassificationSR extends BaseServiceLocatorSR implements ClassificationResource {
	private static transient Logger log = LoggerFactory.getLogger(ClientClassificationSR.class);
	protected String modelName;

	@Inject
	public ClientClassificationSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		modelName = getRequest().getAttributes().get("model").toString();
	}

	public ClassifierResult classify(Map<String, Object> request) throws IOException {
		ClassifierResult result = null;

		ClassifierModel model = ModelHelper.lookupModel(serviceLocator, modelName);
		if (request != null) {
			if (model != null) {
				URIPayload serviceURI = getServiceURI("classifier." + modelName);
				if (serviceURI == null) {
					Collection<URIPayload> classifiers = serviceLocator.getServiceURIs(ServiceType.CLASSIFIER.name());
					for (URIPayload classifier : classifiers) {
						RestletContainer<ClassifierModelStateResource> state = RestletUtil.wrap(ClassifierModelStateResource.class,
								classifier, "/" + modelName);

						Object rep = request.get("replicationFactor");
						Integer repFactor = new Integer(1);
						if (rep != null) {
							repFactor = (Integer) rep;
						}
						State loadState = ((ClassifierModelStateResource) state.getWrapped()).load(Collections
								.<String, Object> singletonMap("replicationFactor", repFactor));
						if (loadState.getStatus().equals(gaia.bigdata.api.Status.SUCCEEDED)) {
							serviceURI = classifier;
							break;
						}
					}
				}
				if (serviceURI != null) {
					RestletContainer<ClassificationResource> rc = RestletUtil.wrap(ClassificationResource.class, serviceURI, "/"
							+ modelName);

					if (rc != null) {
						ClassificationResource cr = (ClassificationResource) rc.getWrapped();
						if (cr != null)
							try {
								result = cr.classify(request);
							} catch (Exception e) {
								log.error("Exception", e);
								setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
							}
					}
				} else {
					setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL,
							"Unable to load model on any of the available resources");
				}
			} else {
				setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, "Model named " + modelName
						+ " does not exist in the system.  Have you registered it?");
			}
		} else
			setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, "request is null");

		return result;
	}

	public ClassifierResult train(int label, Map<String, Object> train) throws UnsupportedOperationException {
		ClassifierResult result = null;
		if (train != null) {
			URIPayload serviceURI = getServiceURI("classifier." + modelName);
			RestletContainer<ClassificationResource> rc = RestletUtil.wrap(ClassificationResource.class, serviceURI, "/" + this.modelName);

			if (rc != null) {
				ClassificationResource cr = (ClassificationResource) rc.getWrapped();
				if (cr != null)
					try {
						result = cr.train(label, train);
					} catch (Exception e) {
						log.error("Exception", e);
						setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
					}
			}
		} else {
			setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, "request is null");
		}
		return result;
	}
}
