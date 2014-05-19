package gaia.commons.server.api.services;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.security.core.context.SecurityContextHolder;

public class LogoutService {

	  @GET
	  @Produces("text/plain")
	  public Response performLogout() {
	    SecurityContextHolder.clearContext();
	    return Response.status(Response.Status.OK).build();
	  }
}
