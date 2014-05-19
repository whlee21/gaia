package gaia.search.ui.api;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.ui.api.services.parsers.RequestBodyParser;
import gaia.search.ui.security.authorization.User;
import gaia.search.ui.security.authorization.Users;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Inject;

@Path("/users/")
public class UserService extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(UserService.class);
	private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

	private Users users;

	@Inject
	public UserService(ObjectSerializer serializer, RequestBodyParser bodyParser, Users users) {
		super(serializer, bodyParser);
		this.users = users;
	}

	@GET
	@Produces("text/plain")
	public Response getUsers(@Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("hhokyung UserService Get Users Called...");
		LOG.debug("hhokyung getUsers ");
		return null;
	}

	@GET
	@Path("{userName}")
	@Produces("text/plain")
	public Response getUser(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("userName") String userName) {
		LOG.debug("hhokyung UserService Get User Called...");

		Map<String, Object> m = new HashMap<String, Object>();
		User u = users.getAnyUser(userName);
		if (null == u) {
			Error error = new Error("userName", Error.E_NOT_FOUND, "Cannot find user '" + userName + "'");
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, Collections.singletonList(error));
		} else {
			m.put("username", userName);
			m.put("roles", u.getRoles());
		}

		return buildResponse(Response.Status.OK, m);
	}

	@POST
	@Produces("text/plain")
	public Response createUser(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("hhokyung UserService @POST User Called...");
		// return handleRequest(headers, body, ui, Request.Type.POST,
		// createUserResource(null));
		return null;
	}

	@PUT
	@Path("{userName}")
	@Produces("text/plain")
	public Response updateUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("userName") String userName) {
		LOG.debug("hhokyung UserService @PUT User Called...");
		// return handleRequest(headers, body, ui, Request.Type.PUT,
		// createUserResource(userName));
		return null;
	}

	@DELETE
	@Path("{userName}")
	@Produces("text/plain")
	public Response deleteUser(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("userName") String userName) {

		// return handleRequest(headers, null, ui, Request.Type.DELETE,
		// createUserResource(userName));
		return null;
	}
}
