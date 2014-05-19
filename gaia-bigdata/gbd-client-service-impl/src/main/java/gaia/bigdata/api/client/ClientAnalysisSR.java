package gaia.bigdata.api.client;

import gaia.bigdata.api.analytics.MetricRequest;
import gaia.bigdata.api.analytics.MetricResource;
import gaia.bigdata.api.analytics.MetricResponse;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientAnalysisSR extends BaseServiceLocatorSR implements MetricResource {
	private String collection;

	@Inject
	public ClientAnalysisSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
	}

	public MetricResponse getMetrics(MetricRequest req) {
		MetricResponse returnValue = null;
		RestletContainer<MetricResource> mrRc = RestletUtil.wrap(MetricResource.class,
				getServiceURI(ServiceType.ANALYTICS.name()), "/" + collection);
		MetricResource mr = (MetricResource) mrRc.getWrapped();
		try {
			returnValue = mr.getMetrics(req);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(mrRc);
		}
		return returnValue;
	}
}
