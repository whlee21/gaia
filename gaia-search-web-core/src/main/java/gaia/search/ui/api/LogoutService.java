package gaia.search.ui.api;

import gaia.api.ObjectSerializer;
import gaia.search.ui.api.services.parsers.RequestBodyParser;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Inject;

/**
 * Service performing logout of current user
 */
@Path("/logout")
public class LogoutService extends BaseService {

	private static final Logger LOG = LoggerFactory.getLogger(LogoutService.class);
	
	@Inject
  public LogoutService(ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(serializer, bodyParser);
	}

	@GET
  @Produces("text/plain")
  public Response performLogout() {
    SecurityContextHolder.clearContext();
    return buildResponse(Response.Status.OK);
  }

}