package gaia.bigdata.api.client;

import gaia.bigdata.api.admin.AdminStatsResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.net.URI;
import java.util.Map;

import com.google.inject.Inject;

public class ClientAdminStatsSR extends BaseServiceLocatorSR implements AdminStatsResource {
	@Inject
	public ClientAdminStatsSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	public Map<URI, Map<String, Object>> getStatistics() {
		Map<URI, Map<String, Object>> returnValue = null;
		RestletContainer<AdminStatsResource> wrapRc = RestletUtil.wrap(AdminStatsResource.class,
				getServiceURI(ServiceType.ADMIN.name()), "/info/statistics");
		AdminStatsResource wrap = (AdminStatsResource) wrapRc.getWrapped();
		try {
			returnValue = wrap.getStatistics();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(wrapRc);
		}
		return returnValue;
	}
}
