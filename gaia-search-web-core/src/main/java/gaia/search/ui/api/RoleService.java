package gaia.search.ui.api;

import gaia.api.ObjectSerializer;
import gaia.search.ui.api.services.parsers.RequestBodyParser;

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

import org.xnap.commons.i18n.I18nFactory;

public class RoleService   extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(RoleService.class);
	private final String collectionName;

	public RoleService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName) {
		super(serializer, bodyParser);
		this.collectionName = collectionName;
	}
	
	@GET
	@Produces("text/plain")
	public Response getRoles(@Context HttpHeaders headers, @Context UriInfo ui) {
		
		System.out.println("**** getRoles collectionName:"+collectionName);
		
//		return handleRequest(headers, null, ui, Request.Type.GET, createRoleResource(collectionName, null));
		return null;
	}
	

	@GET
	@Path("{roleName}")
	@Produces("text/plain")
	public Response getRole(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("roleName") String roleName) {

		System.out.println("**** getRole collectionName:"+collectionName);
		System.out.println("**** getRole roleName:"+roleName);
//		return handleRequest(headers, null, ui, Request.Type.GET, createRoleResource(collectionName, roleName));
		return null;
	}
	
	@POST
	@Produces("text/plain")
	public Response createRole(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

		System.out.println("**** createField collectionName:"+collectionName);
//		return handleRequest(headers, body, ui, Request.Type.POST, createRoleResource(collectionName, null));
		return null;
	}

	@PUT
	@Path("{roleName}")
	@Produces("text/plain")
	public Response updateRole(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("roleName") String roleName) {
		System.out.println("**** updateRole roleName:"+roleName);
//		return handleRequest(headers, body, ui, Request.Type.PUT, createRoleResource(collectionName, roleName));
		return null;
	}

	@DELETE
	@Path("{roleName}")
	@Produces("text/plain")
	public Response deleteRole(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("roleName") String roleName) {
		System.out.println("**** updateField roleName:"+roleName);
//		return handleRequest(headers, null, ui, Request.Type.DELETE, createRoleResource(collectionName, roleName));
		return null;
	}
}
