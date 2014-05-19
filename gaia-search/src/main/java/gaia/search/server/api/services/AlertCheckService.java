package gaia.search.server.api.services;

import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class AlertCheckService   extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(AlertCheckService.class);	
	private static final Logger LOG = LoggerFactory.getLogger(AlertCheckService.class);
	
	private String alertId;

	public AlertCheckService(ObjectSerializer serializer, RequestBodyParser bodyParser, String alertId) {
		super(serializer, bodyParser);
		this.alertId = alertId;
	}
	
	@GET
	@Produces("text/plain")
	public Response getCollectionInfos(@Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("hhokyung AlertService getCollectionInfos alertId : "+alertId);
//		return handleRequest(headers, null, ui, Request.Type.GET, createAlertRunResource(alertId));
		return null;
	}

//	protected ResourceInstance createAlertRunResource(String alertId) {
//		
//		LOG.debug("hhokyung AlertService createAlertResource[AlertRun]: alertId["+alertId+"]");
//		return createResource(Resource.Type.AlertCheck,
//				Collections.singletonMap(Resource.Type.AlertCheck, alertId));
//	}
}
