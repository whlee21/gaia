package gaia.hello.server.api.services;

import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.api.resources.ResourceInstanceFactory;
import gaia.commons.server.api.services.BaseService;
import gaia.commons.server.api.services.Request;
import gaia.commons.server.controller.spi.Resource;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("/hello")
public class HelloService extends BaseService {

	@Inject
	public HelloService(ResourceInstanceFactory resourceFactory) {
		super(resourceFactory);
		// TODO Auto-generated constructor stub
	}

	private static final Logger LOG = LoggerFactory.getLogger(HelloService.class);
	
//	public HelloService() {
//		ResourceInstanceFactory m_resourceFactory = new ResourceInstanceFactoryImpl();
//	}
	
//	public HelloService() {
//		super();
//		LOG.debug("whlee21 HelloService()");
//		this.m_resourceFactory = new ResourceInstanceFactoryImpl();
//	}
	
	@GET
	@Produces("text/plain")
	public Response getMessage(@Context HttpHeaders headers, @Context UriInfo ui) {
		return handleRequest(headers, null, ui, Request.Type.GET, createHelloResource());
	}

	ResourceInstance createHelloResource() {
		Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
		return createResource(Resource.Type.Hello, mapIds);
	}
}