package gaia.bigdata.api.connector.gaiasearch;

import gaia.bigdata.api.State;
import gaia.bigdata.api.connector.ConnectorResource;
import gaia.bigdata.api.connector.ConnectorService;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.util.Map;

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ConnectorServerResource extends BaseServerResource implements ConnectorResource {
	private static transient Logger log = LoggerFactory.getLogger(ConnectorServerResource.class);
	protected ConnectorService connectorService;
	protected String collection;
	protected String id;

	@Inject
	public ConnectorServerResource(Configuration configuration, ConnectorService connectorService) {
		super(configuration);
		this.connectorService = connectorService;
	}

	protected void doInit() throws ResourceException {
		collection = ((String) getRequest().getAttributes().get("collection"));
		id = ((String) getRequest().getAttributes().get("id"));
	}

	public State status() {
		return connectorService.lookup(collection, id);
	}

	public State update(Map<String, Object> entity) {
		return connectorService.update(collection, id, entity);
	}

	public boolean remove() {
		return connectorService.remove(collection, id);
	}

	public State execute(Map<String, Object> body) {
		State state = new State(id, collection);
		return connectorService.execute(state);
	}
}
