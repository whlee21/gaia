package gaia.bigdata.api.connector.gaiasearch;

import gaia.bigdata.api.State;
import gaia.bigdata.api.connector.ConnectorService;
import gaia.bigdata.api.connector.ConnectorsResource;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.util.List;
import java.util.Map;

import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ConnectorsServerResource extends BaseServerResource implements ConnectorsResource {
	private static transient Logger log = LoggerFactory.getLogger(ConnectorsServerResource.class);
	protected ConnectorService connService;
	protected String collection;

	@Inject
	public ConnectorsServerResource(Configuration config, ConnectorService connectorService) {
		super(config);
		this.connService = connectorService;
	}

	protected void doInit() throws ResourceException {
		collection = ((String) getRequest().getAttributes().get("collection"));
	}

	@Post
	public State create(Map<String, Object> entity) {
		State result = null;
		result = connService.create(collection, entity);
		log.info("Created Connector: " + result);
		return result;
	}

	public List<State> listConnectors() {
		return connService.list(collection);
	}
}
