package gaia.bigdata.api.client;

import gaia.bigdata.api.admin.AdminEndpointsResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Collection;
import java.util.Map;

import com.google.inject.Inject;

public class ClientAdminEndpointsSR extends BaseServiceLocatorSR implements AdminEndpointsResource {
	@Inject
	public ClientAdminEndpointsSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	public Map<String, Collection<String>> endpoints() {
		Map<String, Collection<String>> returnValue = null;
		RestletContainer<AdminEndpointsResource> resRc = RestletUtil.wrap(AdminEndpointsResource.class,
				getServiceURI(ServiceType.ADMIN.name()), "/info/endpoints");
		AdminEndpointsResource res = (AdminEndpointsResource) resRc.getWrapped();
		try {
			returnValue = res.endpoints();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resRc);
		}
		return returnValue;
	}
}
