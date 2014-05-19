package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.admin.collection.SolrCmdHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrServerException;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ActivityHistoryServerResource extends ServerResource implements ActivityHistoryResource {
	private static final Object ID = "id";
	private static transient Logger LOG = LoggerFactory.getLogger(ActivityHistoryServerResource.class);
	private String id;
	private ScheduledSolrCommand cmd;
	private CollectionManager cm;
	private String collection;
	private SolrCmdHistory cmdHistory;

	@Inject
	public ActivityHistoryServerResource(CollectionManager cm, SolrCmdHistory cmdHistory) {
		this.cm = cm;
		this.cmdHistory = cmdHistory;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));

		setExisting(cm.getCollectionNames().contains(collection));
		try {
			id = ((String) getRequest().getAttributes().get(ID));
			cmd = cm.getScheduledSolrCommand(collection, id);
		} catch (NumberFormatException e) {
		} finally {
			setExisting((cmd != null) && (isExisting()));
		}
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() throws SolrServerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		return getHistory(cmd, cmdHistory);
	}

	static List<Map<String, Object>> getHistory(ScheduledSolrCommand cmd, SolrCmdHistory cmdHistory)
			throws SolrServerException {
		List<Map<String, Object>> history = cmdHistory.getHistory(cmd.getId());
		if (history == null) {
			return new ArrayList<Map<String, Object>>(0);
		}

		return history;
	}
}
