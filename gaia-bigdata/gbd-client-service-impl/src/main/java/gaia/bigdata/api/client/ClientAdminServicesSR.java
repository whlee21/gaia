package gaia.bigdata.api.client;

import gaia.bigdata.api.admin.AdminServicesResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Collection;
import java.util.Map;

import com.google.inject.Inject;

public class ClientAdminServicesSR extends BaseServiceLocatorSR implements AdminServicesResource {
	@Inject
	public ClientAdminServicesSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	public Map<String, Collection<Object>> collectServices() {
		Map<String, Collection<Object>> returnValue = null;
		RestletContainer<AdminServicesResource> wrapRc = RestletUtil.wrap(AdminServicesResource.class,
				getServiceURI(ServiceType.ADMIN.name()), "/info/services");
		AdminServicesResource wrap = (AdminServicesResource) wrapRc.getWrapped();
		try {
			returnValue = wrap.collectServices();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(wrapRc);
		}
		return returnValue;
	}
}
