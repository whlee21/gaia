package gaia.search.server.api.services;

import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Inject;

@Path("/alerts/")
public class AlertService extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(AlertService.class);
	private static final Logger LOG = LoggerFactory.getLogger(AlertService.class);

	@Inject
	public AlertService(ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(serializer, bodyParser);
	}

	@GET
	@Produces("text/plain")
	public Response getAlerts(@Context HttpHeaders headers, @Context UriInfo ui, @QueryParam("username") String userName) {
		LOG.debug("hhokyung AlertService getAlerts query param(username): " + userName);
		// return handleRequest(headers, null, ui, Request.Type.GET,
		// createAlertResource(null, null));
		return null;
	}

	@GET
	@Path("{alertId}")
	@Produces("text/plain")
	public Response getAlert(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("alertId") String alertId) {
		LOG.debug("hhokyung AlertService getAlert alertId: " + alertId);
		// return handleRequest(headers, null, ui, Request.Type.GET,
		// createAlertResource(null, alertId));
		return null;
	}

	// @GET
	// @Path("{alertId}/check")
	// @Produces("text/plain")
	// public Response getAlertCheck(@Context HttpHeaders headers, @Context
	// UriInfo ui,
	// @PathParam("alertId") String alertId) {
	//
	// LOG.debug("hhokyung AlertService getAlertCheck alertId: "+alertId);
	// return handleRequest(headers, null, ui, Request.Type.GET,
	// createAlertResource("-1", alertId));
	// }

	@Path("{alertId}/check")
	public AlertCheckService getAlertRunHandler(@PathParam("alertId") String alertId) {
		LOG.debug("hhokyung AlertService getAlertRunHandler alertId: " + alertId);
		// return new AlertCheckService(m_resourceFactory, alertId);
		return null;
	}

	@POST
	@Produces("text/plain")
	public Response createAlert(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
		// return handleRequest(headers, body, ui, Request.Type.POST,
		// createAlertResource(null, null));
		return null;
	}

	@PUT
	@Path("{alertId}")
	@Produces("text/plain")
	public Response updateAlert(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("alertId") String alertId) {
		// return handleRequest(headers, body, ui, Request.Type.PUT,
		// createAlertResource(null, alertId));
		return null;
	}

	@DELETE
	@Path("{alertId}")
	@Produces("text/plain")
	public Response deleteAlert(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("alertId") String alertId) {
		// return handleRequest(headers, null, ui, Request.Type.DELETE,
		// createAlertResource(null, alertId));
		return null;
	}

	// ResourceInstance createAlertResource(String userName, String alertId) {
	// Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
	// mapIds.put(Resource.Type.User, userName);
	// mapIds.put(Resource.Type.Alert, alertId);
	//
	// LOG.debug("hhokyung AlertService createAlertResource[Alert]: userName["+userName+"] alertId["+alertId+"]");
	//
	// return createResource(Resource.Type.Alert, mapIds);
	// }
}
