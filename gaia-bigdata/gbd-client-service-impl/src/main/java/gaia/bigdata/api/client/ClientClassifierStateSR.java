package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.bigdata.api.classification.ClassifierStateResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ClientClassifierStateSR extends BaseServiceLocatorSR implements ClassifierStateResource {
	private static transient Logger log = LoggerFactory.getLogger(ClientClassifierStateSR.class);

	@Inject
	public ClientClassifierStateSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	public State info() {
		State result = new State();
		Collection<URIPayload> serviceURIs = serviceLocator.getServiceURIs(ServiceType.CLASSIFIER_STATE.name());
		if (serviceURIs != null) {
			for (URIPayload serviceURI : serviceURIs) {
				RestletContainer<ClassifierStateResource> csr = RestletUtil.wrap(ClassifierStateResource.class, serviceURI, "");

				State tmp = ((ClassifierStateResource) csr.getWrapped()).info();
				if (tmp != null) {
					result.addChild(tmp);
				}
			}
			result.setStatus(Status.SUCCEEDED);
		}
		return result;
	}
}
