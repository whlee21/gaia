package gaia.search.server.api.services;

import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Inject;

@Path("/config/ssl/")
public class SslConfigService   extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(SslConfigService.class);
	private static final Logger LOG = LoggerFactory.getLogger(SslConfigService.class);

	private final String authRequireAuthorization;
	
	@Inject
	public SslConfigService(ObjectSerializer serializer, RequestBodyParser bodyParser, String authRequireAuthorization) {
		super(serializer, bodyParser);
		this.authRequireAuthorization = authRequireAuthorization;
	}

	@GET
	@Produces("text/plain")
	public Response getSslConfig(@Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("SslConfigService getSslConfig called..!!");
//		return handleRequest(headers, null, ui, Request.Type.GET, createSslConfigResource(null));
		return null;
	}
	
	@PUT
	@Produces("text/plain")
	public Response updateSslConfig(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("SslConfigService updateSslConfig called..!!");
//		return handleRequest(headers, body, ui, Request.Type.PUT, createSslConfigResource(null));
		return null;
	}
}