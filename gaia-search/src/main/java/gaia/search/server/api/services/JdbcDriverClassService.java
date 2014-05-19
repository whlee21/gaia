package gaia.search.server.api.services;

import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.resource.Resource;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.solr.core.CoreContainer;
import org.xnap.commons.i18n.I18nFactory;

public class JdbcDriverClassService  extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(JdbcDriverClassService.class);
	private String collection;
	private ConnectorManager cm;


	public JdbcDriverClassService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			ConnectorManager cm) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cm = cm;
	}

	@GET
	@Produces("text/plain")
	public Response getJdbcDriverClasses(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		List<String> classes = getCurrentClasses(cm, collection);
		return buildResponse(Response.Status.OK, classes); 
	}
	


	static List<String> getCurrentClasses(ConnectorManager cm, String collection) throws Exception {
		List<Resource> resources = cm.listResources("gaia.jdbc", collection, null);
		List<String> classes = new ArrayList<String>();
		for (Resource res : resources) {
			if ((res.getProperties() != null) && ("class".equals(res.getProperties().get("type")))) {
				classes.add(res.getName());
			}
		}
		return classes;
	}
}
