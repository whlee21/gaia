package gaia.bigdata.api.client;

import gaia.bigdata.api.admin.AdminInfoResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Map;

import org.restlet.resource.Get;

import com.google.inject.Inject;

public class ClientAdminInfoSR extends BaseServiceLocatorSR implements AdminInfoResource {
	@Inject
	public ClientAdminInfoSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	@Get
	public Map<String, Map<String, Object>> info() {
		Map<String, Map<String, Object>> returnValue = null;
		RestletContainer<AdminInfoResource> wrapRc = RestletUtil.wrap(AdminInfoResource.class,
				getServiceURI(ServiceType.ADMIN.name()), "/info");
		AdminInfoResource wrap = (AdminInfoResource) wrapRc.getWrapped();
		try {
			returnValue = wrap.info();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(wrapRc);
		}
		return returnValue;
	}
}
