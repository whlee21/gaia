package gaia.api;

import gaia.crawl.ConnectorManager;
import gaia.crawl.resource.Resource;

import java.util.ArrayList;
import java.util.List;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class JDBCDriversClassesServerResource extends ServerResource implements JDBCDriversClassesResource {
	private static transient Logger LOGGER = LoggerFactory.getLogger(JDBCDriversClassesServerResource.class);
	private String collection;
	private ConnectorManager cm;

	@Inject
	public JDBCDriversClassesServerResource(ConnectorManager cm) {
		this.cm = cm;
	}

	public void doInit() {
		collection = ((String) getRequestAttributes().get("coll_name"));
	}

	@Get("json")
	public List<String> retrieve() throws Exception {
		List<String> classes = getCurrentClasses(cm, collection);
		setStatus(Status.SUCCESS_OK);
		return classes;
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
